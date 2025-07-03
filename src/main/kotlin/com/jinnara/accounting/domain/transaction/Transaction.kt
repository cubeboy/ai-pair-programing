package com.jinnara.accounting.domain.transaction

import com.jinnara.accounting.domain.account.AccountId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 회계 거래 도메인 엔티티
 */
data class Transaction(
    val id: TransactionId,
    val date: LocalDate,
    val description: String,
    val reference: String?,
    val entries: List<TransactionEntry>,
    val status: TransactionStatus = TransactionStatus.DRAFT,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(entries.size >= 2) { "거래는 최소 2개의 분개를 포함해야 합니다." }
        require(isBalanced()) { "차변과 대변의 합계가 일치하지 않습니다." }
    }
    
    private fun isBalanced(): Boolean {
        val debitTotal = entries.filter { it.type == EntryType.DEBIT }.sumOf { it.amount }
        val creditTotal = entries.filter { it.type == EntryType.CREDIT }.sumOf { it.amount }
        return debitTotal == creditTotal
    }
    
    fun post(): Transaction {
        require(status == TransactionStatus.DRAFT) { "이미 전기된 거래입니다." }
        return copy(status = TransactionStatus.POSTED, updatedAt = LocalDateTime.now())
    }
    
    fun reverse(): Transaction {
        require(status == TransactionStatus.POSTED) { "전기된 거래만 취소할 수 있습니다." }
        return copy(status = TransactionStatus.REVERSED, updatedAt = LocalDateTime.now())
    }
}

@JvmInline
value class TransactionId(val value: Long)

data class TransactionEntry(
    val accountId: AccountId,
    val type: EntryType,
    val amount: BigDecimal,
    val description: String?
) {
    init {
        require(amount > BigDecimal.ZERO) { "금액은 0보다 커야 합니다." }
    }
}

enum class EntryType {
    DEBIT,  // 차변
    CREDIT  // 대변
}

enum class TransactionStatus {
    DRAFT,    // 임시저장
    POSTED,   // 전기완료
    REVERSED  // 취소
}
