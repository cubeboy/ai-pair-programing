package com.jinnara.accounting.domain.transaction

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 거래를 표현하는 도메인 엔티티
 */
data class Transaction(
    val id: TransactionId,
    val description: String,
    val date: LocalDate,
    val reference: String? = null,
    val status: TransactionStatus,
    val entries: List<JournalEntry>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    init {
        require(description.isNotBlank()) { "거래 설명은 필수입니다" }
        require(description.length <= 500) { "거래 설명은 500자를 초과할 수 없습니다" }
        require(entries.size >= 2) { "분개 항목은 최소 2개 이상이어야 합니다" }
        validateBalanced()
    }

    /**
     * 차변과 대변의 균형을 검증합니다
     */
    private fun validateBalanced() {
        val debitTotal = entries.filter { it.entryType == EntryType.DEBIT }
            .sumOf { it.amount }
        val creditTotal = entries.filter { it.entryType == EntryType.CREDIT }
            .sumOf { it.amount }

        require(debitTotal == creditTotal) {
            "차변과 대변의 합이 일치하지 않습니다. 차변: $debitTotal, 대변: $creditTotal"
        }
    }

    /**
     * 거래 총액을 반환합니다 (차변 또는 대변 총액)
     */
    val totalAmount: BigDecimal
        get() = entries.filter { it.entryType == EntryType.DEBIT }.sumOf { it.amount }

    /**
     * 분개 항목 목록 (하위 호환성을 위한 별칭)
     */
    val journalEntries: List<JournalEntry>
        get() = entries

    /**
     * 거래 일자 (하위 호환성을 위한 별칭)
     */
    val transactionDate: LocalDate
        get() = date

    /**
     * 거래를 취소할 수 있는지 확인합니다
     */
    fun canBeCancelled(): Boolean {
        return status != TransactionStatus.CANCELLED && status != TransactionStatus.CLOSED
    }

    /**
     * 거래를 수정할 수 있는지 확인합니다
     */
    fun canBeUpdated(): Boolean {
        return status == TransactionStatus.PENDING
    }

    /**
     * 거래를 취소 상태로 변경합니다
     */
    fun cancel(): Transaction {
        require(canBeCancelled()) { "현재 상태에서는 거래를 취소할 수 없습니다: $status" }
        return copy(
            status = TransactionStatus.CANCELLED,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 거래 정보를 업데이트합니다
     */
    fun update(
        description: String,
        date: LocalDate,
        entries: List<JournalEntry>,
        reference: String? = null
    ): Transaction {
        require(canBeUpdated()) { "현재 상태에서는 거래를 수정할 수 없습니다: $status" }

        return copy(
            description = description,
            date = date,
            reference = reference,
            entries = entries,
            updatedAt = LocalDateTime.now()
        )
    }

    companion object {
        /**
         * 새로운 거래를 생성합니다
         */
        fun create(
            description: String,
            date: LocalDate,
            entries: List<JournalEntry>,
            reference: String? = null
        ): Transaction {
            val now = LocalDateTime.now()
            return Transaction(
                id = TransactionId(0), // Repository에서 실제 ID 할당
                description = description,
                date = date,
                reference = reference,
                status = TransactionStatus.PENDING,
                entries = entries,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
