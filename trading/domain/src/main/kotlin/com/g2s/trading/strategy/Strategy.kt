package com.g2s.trading.strategy

interface Strategy {
    fun changeOrderMode(modeValue: String)
    fun getTypeOfStrategy(): String
}
