package com.g2s.trading.event

import com.g2s.trading.account.Account
import org.springframework.context.ApplicationEvent

sealed class UserEvent(
    source: Any
) : ApplicationEvent(source) {
    data class AccountRefreshEvent(
        val source: Account
    ) : UserEvent(source)
}
