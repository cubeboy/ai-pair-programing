package com.jinnara.accounting.adapter.rest.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.LocalDate

data class UpdateTransactionRequest(
    @field:NotBlank(message = "거래 설명은 필수입니다")
    @field:Size(max = 500, message = "거래 설명은 500자를 초과할 수 없습니다")
    val description: String,

    @field:NotNull(message = "거래일자는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val transactionDate: LocalDate,

    @field:NotEmpty(message = "분개 항목은 최소 2개 이상이어야 합니다")
    @field:Size(min = 2, message = "분개 항목은 최소 2개 이상이어야 합니다")
    @field:Valid
    val journalEntries: List<JournalEntryRequest>
)
