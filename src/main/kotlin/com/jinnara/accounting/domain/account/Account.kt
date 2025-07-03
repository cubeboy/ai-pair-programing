package com.jinnara.accounting.domain.account

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 계정과목 도메인 엔티티
 */
data class Account(
    val id: AccountId,
    val code: String,
    val name: String,
    val type: AccountType,
    val parentId: AccountId?,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun isDebitAccount(): Boolean {
        return type in listOf(AccountType.ASSET, AccountType.EXPENSE)
    }
    
    fun isCreditAccount(): Boolean {
        return type in listOf(AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE)
    }
    
    fun deactivate(): Account {
        return copy(isActive = false, updatedAt = LocalDateTime.now())
    }
}

@JvmInline
value class AccountId(val value: Long)

enum class AccountType {
    ASSET,      // 자산
    LIABILITY,  // 부채
    EQUITY,     // 자본
    REVENUE,    // 수익
    EXPENSE     // 비용
}
