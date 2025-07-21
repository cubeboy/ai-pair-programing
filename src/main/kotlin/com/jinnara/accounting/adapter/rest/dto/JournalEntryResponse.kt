package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.transaction.JournalEntry
import java.math.BigDecimal

data class JournalEntryResponse(
    val accountId: Long,
    val accountCode: String,
    val accountName: String,
    val entryType: String,
    val amount: BigDecimal,
    val description: String?
) {
    companion object {
        fun fromDomain(journalEntry: JournalEntry): JournalEntryResponse {
            return JournalEntryResponse(
                accountId = journalEntry.accountId.value,
                accountCode = journalEntry.account.code,
                accountName = journalEntry.account.name,
                entryType = journalEntry.entryType.name,
                amount = journalEntry.amount,
                description = journalEntry.description
            )
        }
    }
}
