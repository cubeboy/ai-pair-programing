package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.transaction.JournalEntry
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "분개 항목 응답")
data class JournalEntryResponse(
    @Schema(description = "계정과목 ID", example = "1")
    val accountId: Long,

    @Schema(description = "계정과목 코드", example = "1000")
    val accountCode: String,

    @Schema(description = "계정과목명", example = "현금")
    val accountName: String,

    @Schema(description = "���/대변 구분", example = "DEBIT", allowableValues = ["DEBIT", "CREDIT"])
    val entryType: String,

    @Schema(description = "거래 금액", example = "100000.00")
    val amount: BigDecimal,

    @Schema(description = "적요", example = "현금 입금")
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
