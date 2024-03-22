package com.g2s.trading.symbol

data class Symbol(val value: String) {
    companion object {
        fun valueOf(value : String) : Symbol {
            return Symbol(value)
        }
    }

}
