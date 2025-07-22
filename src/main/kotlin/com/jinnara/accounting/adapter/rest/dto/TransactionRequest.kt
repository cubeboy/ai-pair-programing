package com.jinnara.accounting.adapter.rest.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.LocalDate

@Schema(description = "거래 생성 요청")
data class CreateTransactionRequest(
    @field:NotBlank(message = "거래 설명은 필수입니다")
    @field:Size(max = 500, message = "거래 설명은 500자를 초과할 수 없습니다")
    @Schema(description = "거래 설명", example = "현금 입금", required = true, maxLength = 500)
    val description: String,

    @field:NotNull(message = "거래일자는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "거래 날짜", example = "2024-01-15", required = true, format = "date")
    val transactionDate: LocalDate,

    @field:NotEmpty(message = "분개 항목은 최소 2개 이상이어야 합니다")
    @field:Size(min = 2, message = "분개 항목은 최소 2개 이상이어야 합니다")
    @field:Valid
    @Schema(description = "분개 항목 목록 (차변과 대변 합계가 일치해야 함)", required = true, minLength = 2)
    val journalEntries: List<JournalEntryRequest>
)
