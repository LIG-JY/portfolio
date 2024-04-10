package com.g2s.trading.account

import com.g2s.trading.exchange.Exchange
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean

@Service
class AccountUseCase(
    private val exchangeImpl: Exchange
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    lateinit var localAccount: Account
    private var synced: Boolean = false
    private val lock = AtomicBoolean(false)

    init {
        localAccount = loadAccount()
        synced = true
    }

    fun refreshAccount(refreshedAccount: Account) {
        this.localAccount = refreshedAccount
    }

    fun syncAccount() {
        logger.debug("synced account \n- asset : ${localAccount.assetWallets[0].asset} \n- balance : ${localAccount.assetWallets[0].walletBalance}\n")
        synced = true
    }

    fun getAllocatedBalancePerStrategy(asset: Asset, availableRatio: Double): BigDecimal {
        val assetWallet = this.localAccount.assetWallets.first { it.asset == asset }
        return BigDecimal(assetWallet.walletBalance).multiply(BigDecimal(availableRatio))
    }

    fun getAvailableBalance(asset: Asset): BigDecimal {
        val assetWallet = localAccount.assetWallets.first { it.asset == asset }
        return BigDecimal(assetWallet.walletBalance)
    }

    fun getBalance(asset: Asset, timeStamp: Long): Double {
        val assetWallet = exchangeImpl.getAccount(timeStamp).assetWallets.first{it.asset == asset}

        return assetWallet.walletBalance
    }


    fun setUnSynced() {
        synced = false
    }

    fun isSynced(): Boolean {
        return synced
    }

    fun acquire(): Boolean {
        return lock.compareAndSet(false, true)
    }

    fun release(): Boolean {
        return lock.compareAndSet(true, false)
    }

    private fun loadAccount(): Account {
        return exchangeImpl.getAccount()
    }
}
