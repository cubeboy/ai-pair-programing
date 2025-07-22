package com.jinnara.accounting.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal

@Schema(description = "분개 항목 요청")
data class JournalEntryRequest(
    @field:NotNull(message = "계정과목 ID는 필수입니다")
    @field:Positive(message = "계정과목 ID는 양수여야 합니다")
    @Schema(description = "계정과목 ID", example = "1", required = true)
    val accountId: Long,

    @field:NotBlank(message = "차/대변 구분은 필수입니다")
    @field:Pattern(regexp = "^(DEBIT|CREDIT)$", message = "차/대변 구분은 DEBIT 또는 CREDIT이어야 합니다")
    @Schema(description = "차/대변 구분", example = "DEBIT", required = true, allowableValues = ["DEBIT", "CREDIT"])
    val entryType: String,

    @field:NotNull(message = "금액은 필수입니다")
    @field:DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다")
    @field:Digits(integer = 15, fraction = 2, message = "금액은 최대 15자리, 소수점 2자리까지 가능합니다")
    @Schema(description = "거래 금액", example = "100000.00", required = true)
    val amount: BigDecimal,

    @field:Size(max = 200, message = "적요는 200자를 초과할 수 없습니다")
    @Schema(description = "적요 (선택사항)", example = "현금 입금", maxLength = 200)
    val description: String? = null
)
