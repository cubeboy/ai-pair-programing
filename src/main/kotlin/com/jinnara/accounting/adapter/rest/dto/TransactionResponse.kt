package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "거래 응답")
data class TransactionResponse(
    @Schema(description = "거래 ID", example = "1")
    val id: Long,

    @Schema(description = "거래 설명", example = "현금 입금")
    val description: String,

    @Schema(description = "거래 날짜", example = "2024-01-15")
    val transactionDate: LocalDate,

    @Schema(description = "거래 상태", example = "PENDING", allowableValues = ["PENDING", "CONFIRMED", "CANCELLED"])
    val status: TransactionStatus,

    @Schema(description = "총 거래 금액 (차변 또는 대변의 합계)", example = "100000.00")
    val totalAmount: BigDecimal,

    @Schema(description = "분개 항목 목록")
    val journalEntries: List<JournalEntryResponse>,

    @Schema(description = "생성 일시", example = "2024-01-15T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 일시", example = "2024-01-15T10:00:00")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromDomain(transaction: Transaction): TransactionResponse {
            return TransactionResponse(
                id = transaction.id.value,
                description = transaction.description,
                transactionDate = transaction.transactionDate,
                status = transaction.status,
                totalAmount = transaction.totalAmount,
                journalEntries = transaction.journalEntries.map { JournalEntryResponse.fromDomain(it) },
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt
            )
        }
    }
}
