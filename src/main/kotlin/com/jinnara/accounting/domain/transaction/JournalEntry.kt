package com.jinnara.accounting.domain.transaction

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import java.math.BigDecimal

/**
 * 분개 항목을 표현하는 도메인 엔티티
 */
data class JournalEntry(
    val accountId: AccountId,
    val account: Account,
    val entryType: EntryType,
    val amount: BigDecimal,
    val description: String? = null
) {
    init {
        require(amount > BigDecimal.ZERO) { "금액은 0보다 커야 합니다" }
        require(amount.scale() <= 2) { "금액은 소수점 2자리까지만 허용됩니다" }
        description?.let {
            require(it.length <= 200) { "적요는 200자를 초과할 수 없습니다" }
        }
    }
}
