package com.g2s.trading.strategy.simple

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.g2s.trading.indicator.MarkPriceUseCase
import com.g2s.trading.event.StrategyEvent
import com.g2s.trading.event.TradingEvent
import com.g2s.trading.account.AccountUseCase
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.history.ConditionUseCase
import com.g2s.trading.history.OpenCondition
import com.g2s.trading.indicator.CandleStick
import com.g2s.trading.lock.LockUsage
import com.g2s.trading.lock.LockUseCase
import com.g2s.trading.matchingReport.AnalyzeReport
import com.g2s.trading.order.OrderSide
import com.g2s.trading.order.OrderType
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionUseCase
import com.g2s.trading.strategy.Strategy
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.strategy.StrategySpecRepository
import com.g2s.trading.symbol.Symbol
import com.g2s.trading.symbol.SymbolUseCase
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.IllegalArgumentException
import kotlin.math.max
import kotlin.math.min

@Component
class SimpleOpenMan(
    private val strategySpecRepository: StrategySpecRepository,
    private val lockUseCase: LockUseCase,
    private val positionUseCase: PositionUseCase,
    private val accountUseCase: AccountUseCase,
    private val markPriceUseCase: MarkPriceUseCase,
    private val symbolUseCase: SymbolUseCase,
    private val conditionUseCase: ConditionUseCase,
) : Strategy {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private var orderMode = OrderMode.MINIMUM_QUANTITY

    companion object {
        private const val TYPE = "simple"
        private const val TAKER_FEE_RATE = 0.00045  // taker fee : 0.045%
    }

    private val specs: ConcurrentHashMap<String, StrategySpec> = ConcurrentHashMap<String, StrategySpec>().also { map ->
        map.putAll(strategySpecRepository.findAllServiceStrategySpecByType(TYPE).associateBy { it.strategyKey })
    }

    private val candleSticks: MutableMap<String, MutableMap<Symbol, CandleStick>> =
        mutableMapOf() // strategyKey to Map<Symbol, CandleStick> | spec to Symbols mapping [1 : n]

    @EventListener
    fun handleStartStrategyEvent(event: StrategyEvent.StartStrategyEvent) {
        specs.computeIfAbsent(event.source.strategyKey) {
            event.source
        }
    }

    @EventListener
    fun handleStopStrategyEvent(event: StrategyEvent.StopStrategyEvent) {
        candleSticks.remove(event.source.strategyKey)
        specs.remove(event.source.strategyKey)
    }

    @EventListener
    fun handleUpdateStrategyEvent(event: StrategyEvent.UpdateStrategyEvent) {
        specs.replace(event.source.strategyKey, event.source)
    }

    @EventListener
    fun handleCandleStickEvent(event: TradingEvent.CandleStickEvent) {
        specs.values.parallelStream().forEach { spec ->
            open(spec, event.source)
        }
    }

    override fun changeOrderMode(modeValue: String) {
        try {
            val orderMode = OrderMode.valueOf(modeValue)
            this.orderMode = orderMode
        } catch (e: IllegalArgumentException) {
            logger.warn(e.message + "\n유효하지 않은 OrderMode")
        }
    }

    override fun getTypeOfStrategy(): String {
        return TYPE
    }

    fun open(spec: StrategySpec, candleStick: CandleStick) {
        // lock 획득
        val acquired = lockUseCase.acquire(spec.strategyKey, LockUsage.OPEN)
        if (!acquired) return

        // spec에 운영된 symbol중에서 현재 포지션이 없는 symbol 확인
        val unUsedSymbols = spec.symbols - positionUseCase.getAllUsedSymbols()
        if (unUsedSymbols.isEmpty()) {
            lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
            return
        }

        // spec에서 현재 포지션이 없는 symbol 중 인자로 넘어온 symbol(candleStick에 포함)을 취급하는지 확인
        if (!unUsedSymbols.contains(candleStick.symbol)) {
            lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
            return
        }

        // account lock
        val accountAcquired = accountUseCase.acquire()
        if (!accountAcquired) {
            lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
            return
        }

        // account sync check
        val accountSynced = accountUseCase.isSynced()
        if (!accountSynced) {
            accountUseCase.release()
            lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
            return
        }

        // 계좌 잔고 확인
        val allocatedBalance =
            accountUseCase.getAllocatedBalancePerStrategy(spec.asset, spec.allocatedRatio)
        val availableBalance = accountUseCase.getAvailableBalance(spec.asset)

        if (allocatedBalance > availableBalance) {
            accountUseCase.release()
            lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
            return
        }

        // update candleStick
        val strategyCandleSticks = candleSticks[spec.strategyKey]
        lateinit var oldCandleStick: CandleStick

        val shouldAnalyze = if (strategyCandleSticks == null) {
            candleSticks[spec.strategyKey] = mutableMapOf(candleStick.symbol to candleStick)
            false
        } else {
            val old = strategyCandleSticks[candleStick.symbol]
            strategyCandleSticks[candleStick.symbol] = candleStick
            if (old == null) {
                false
            } else {
                // 이미 가지고 있는 캔들스틱인 경우
                if (old.key == candleStick.key) {
                    logger.debug("same candlestick {}", candleStick.symbol)
                    false
                }
                // 1분 차이 캔들스틱이 아닌 경우
                else if (old.key + 60000L != candleStick.key) {
                    logger.debug("not updated candlestick. {}", candleStick.symbol)
                    false
                }
                // 갱신된지 1초 이상된 캔들스틱인 경우
                else if (Instant.now().toEpochMilli() - candleStick.key > 1000) {
                    logger.debug("out 1 second, {}", candleStick.symbol)
                    false
                } else {
                    logger.debug("in 1 second, {}", candleStick.symbol)
                    oldCandleStick = old
                    true
                }
            }
        }

        if (!shouldAnalyze) {
            accountUseCase.release()
            lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
            return
        }

        // 오픈 가능한 symbol 확인
        val markPrice = markPriceUseCase.getMarkPrice(candleStick.symbol)
        val hammerRatio = spec.op["hammerRatio"].asDouble()
        val takeProfitFactor = spec.op["takeProfitFactor"].asDouble()
        when (val analyzeReport = analyze(oldCandleStick, hammerRatio, takeProfitFactor, availableBalance)) {
            is AnalyzeReport.NonMatchingReport -> {
                logger.debug("non matching report for symbol: {}", candleStick.symbol)
            }

            is AnalyzeReport.MatchingReport -> {
                // open position
                val position = Position(
                    positionKey = Position.PositionKey(analyzeReport.symbol, analyzeReport.orderSide),
                    strategyKey = spec.strategyKey,
                    symbol = analyzeReport.symbol,
                    orderSide = analyzeReport.orderSide,
                    orderType = OrderType.MARKET,
                    entryPrice = markPrice.price,
                    positionAmt = quantity(
                        allocatedBalance,
                        BigDecimal(symbolUseCase.getMinNotionalValue(analyzeReport.symbol)),
                        BigDecimal(markPrice.price),
                        symbolUseCase.getQuantityPrecision(analyzeReport.symbol)
                    ),
                    asset = spec.asset,
                    referenceData = analyzeReport.referenceData,
                )
                logger.debug("openPosition strategyKey: {}, symbol: {}", position.strategyKey, position.symbol)
                conditionUseCase.setOpenCondition(position, analyzeReport.openCondition)
                positionUseCase.openPosition(position, spec)
            }
        }

        accountUseCase.release()
        lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
    }

    private fun analyze(
        candleStick: CandleStick,
        hammerRatio: Double,
        takeProfitFactor: Double,
        availableBalance: BigDecimal
    ): AnalyzeReport {
        logger.debug("analyze... candleStick: {}", candleStick)
        val tailTop = BigDecimal(candleStick.high)
        val tailBottom = BigDecimal(candleStick.low)
        val bodyTop = BigDecimal(max(candleStick.open, candleStick.close))
        val bodyBottom = BigDecimal(min(candleStick.open, candleStick.close))
        val totalLength = tailTop - tailBottom
        val bodyLength = bodyTop - bodyBottom
        val operationalHammerRatio = BigDecimal(hammerRatio)    // 운영값 : default 2
        val operationalTakeProfitFactor = BigDecimal(takeProfitFactor)    // 운영값

        if (bodyLength.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("body length : 0")
            return AnalyzeReport.NonMatchingReport
        } else if ((bodyLength.divide(totalLength, 3, RoundingMode.HALF_UP)) <= BigDecimal(0.15)) {
            logger.debug("body length / total length <= 15%")
            return AnalyzeReport.NonMatchingReport
        } else if (tailTop > bodyTop && tailBottom == bodyBottom) {
            logger.debug("top tail")
            val tailLength = tailTop - bodyTop  // tailLength = topTailLength
            val candleHammerRatio = tailLength / bodyLength
            logger.debug(
                "calculated candleHammerRatio: {}, tailLength: {}, bodyLength: {}",
                candleHammerRatio,
                tailLength,
                bodyLength
            )
            if (candleHammerRatio > operationalHammerRatio && isPositivePnl(
                    bodyTop,
                    bodyTop.minus(tailLength.multiply(operationalTakeProfitFactor))
                )
            ) {
                logger.debug("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${operationalHammerRatio.toDouble()}")
                val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                (referenceData as ObjectNode).set<DoubleNode>(
                    "tailLength",
                    DoubleNode(tailLength.toDouble())
                )
                return AnalyzeReport.MatchingReport(
                    candleStick.symbol, OrderSide.SHORT, OpenCondition.SimpleCondition(
                        patten = SimplePattern.TOP_TAIL,
                        candleHammerRatio = candleHammerRatio.toString(),
                        operationalCandleHammerRatio = operationalHammerRatio.toString(),
                        beforeBalance = availableBalance.toDouble(),
                    ), referenceData
                )
            }
        } else if (tailBottom < bodyBottom && tailTop == bodyTop) {
            logger.debug("bottom tail")
            val tailLength = bodyBottom - tailBottom
            val candleHammerRatio = tailLength / bodyLength
            logger.debug(
                "calculated candleHammerRatio: {}, tailLength: {}, bodyLength: {}",
                candleHammerRatio,
                tailLength,
                bodyLength
            )
            if (candleHammerRatio > operationalHammerRatio && isPositivePnl(
                    bodyBottom,
                    bodyBottom.plus(tailLength.multiply(operationalTakeProfitFactor))
                )
            ) {
                logger.debug("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${operationalHammerRatio.toDouble()}")
                val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                (referenceData as ObjectNode).set<DoubleNode>(
                    "tailLength",
                    DoubleNode(tailLength.toDouble())
                )
                return AnalyzeReport.MatchingReport(
                    candleStick.symbol, OrderSide.LONG, OpenCondition.SimpleCondition(
                        patten = SimplePattern.BOTTOM_TAIL,
                        candleHammerRatio = candleHammerRatio.toString(),
                        operationalCandleHammerRatio = operationalHammerRatio.toString(),
                        beforeBalance = availableBalance.toDouble(),
                    ), referenceData
                )
            }
        } else {
            val highTailLength = tailTop - bodyTop
            val lowTailLength = bodyBottom - tailBottom

            if (highTailLength > lowTailLength) {
                logger.debug(
                    "middle high tail\nhighTail: {}, lowTail: {}, bodyTail: {}",
                    highTailLength,
                    lowTailLength,
                    bodyLength
                )
                val calculatedHammerRatio = highTailLength / bodyLength
                if (calculatedHammerRatio > operationalHammerRatio && isPositivePnl(
                        bodyTop,
                        bodyTop.minus(highTailLength.multiply(operationalTakeProfitFactor))
                    )
                ) {
                    logger.debug("계산된HammerRatio: ${calculatedHammerRatio.toDouble()}, 운영HammerRatio: ${operationalHammerRatio.toDouble()}")
                    val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                    (referenceData as ObjectNode).set<DoubleNode>(
                        "tailLength",
                        DoubleNode(highTailLength.toDouble())
                    )
                    return AnalyzeReport.MatchingReport(
                        candleStick.symbol, OrderSide.SHORT, OpenCondition.SimpleCondition(
                            patten = SimplePattern.MIDDLE_HIGH_TAIL,
                            candleHammerRatio = calculatedHammerRatio.toString(),
                            operationalCandleHammerRatio = operationalHammerRatio.toString(),
                            beforeBalance = availableBalance.toDouble(),
                        ), referenceData
                    )
                }
            } else {
                logger.debug(
                    "middle low tail\nhighTail: {}, lowTail: {}, bodyTail: {}",
                    highTailLength,
                    lowTailLength,
                    bodyLength
                )
                val candleHammerRatio = lowTailLength / bodyLength
                if (candleHammerRatio > operationalHammerRatio && isPositivePnl(
                        bodyBottom,
                        bodyBottom.plus(lowTailLength.multiply(operationalTakeProfitFactor))
                    )
                ) {
                    logger.debug("계산된HammerRatio: ${candleHammerRatio.toDouble()}, 운영HammerRatio: ${operationalHammerRatio.toDouble()}")
                    val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                    (referenceData as ObjectNode).set<DoubleNode>(
                        "tailLength",
                        DoubleNode(lowTailLength.toDouble())
                    )
                    return AnalyzeReport.MatchingReport(
                        candleStick.symbol, OrderSide.LONG, OpenCondition.SimpleCondition(
                            patten = SimplePattern.MIDDLE_LOW_TAIL,
                            candleHammerRatio = candleHammerRatio.toString(),
                            operationalCandleHammerRatio = operationalHammerRatio.toString(),
                            beforeBalance = availableBalance.toDouble(),
                        ), referenceData
                    )
                }
            }
        }

        return AnalyzeReport.NonMatchingReport
    }

    private fun quantity(
        balance: BigDecimal,
        minNotional: BigDecimal,
        markPrice: BigDecimal,
        quantityPrecision: Int
    ): Double {
        return when (orderMode) {
            OrderMode.MINIMUM_QUANTITY -> {
                // "code":-4164,"msg":"Order's notional must be no smaller than 100 (unless you choose reduce only)."
                // 수량이 부족하다는 이유로 예외가 너무 자주 떠서 올림으로 처리함
                minNotional.divide(markPrice, quantityPrecision, RoundingMode.CEILING).toDouble()
            }

            OrderMode.NORMAL -> {
                balance.divide(markPrice, quantityPrecision, RoundingMode.DOWN).toDouble()
            }
        }
    }

    /*
        MARKET ORDER => taker
        taker fee : 0.045%
     */
    private fun isPositivePnl(open: BigDecimal, close: BigDecimal): Boolean {
        val pnl = (open - close).abs()
        val fee = (open + close).multiply(BigDecimal(TAKER_FEE_RATE))
        logger.debug("fee :{}, pnl :{}", fee, pnl)
        return pnl > fee
    }

    enum class OrderMode {
        NORMAL,
        MINIMUM_QUANTITY
    }
}
