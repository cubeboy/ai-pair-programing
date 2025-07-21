package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TransactionResponse(
    val id: Long,
    val description: String,
    val transactionDate: LocalDate,
    val status: TransactionStatus,
    val totalAmount: BigDecimal,
    val journalEntries: List<JournalEntryResponse>,
    val createdAt: LocalDateTime,
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
