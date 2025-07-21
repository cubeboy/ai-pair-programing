package com.jinnara.accounting.adapter.rest.dto

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class JournalEntryRequest(
    @field:NotNull(message = "계정과목 ID는 필수입니다")
    @field:Positive(message = "계정과목 ID는 양수여야 합니다")
    val accountId: Long,

    @field:NotBlank(message = "차/대변 구분은 필수입니다")
    @field:Pattern(regexp = "^(DEBIT|CREDIT)$", message = "차/대변 구분은 DEBIT 또는 CREDIT이어야 합니다")
    val entryType: String,

    @field:NotNull(message = "금액은 필수입니다")
    @field:DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다")
    @field:Digits(integer = 15, fraction = 2, message = "금액은 최대 15자리, 소수점 2자리까지 가능합니다")
    val amount: BigDecimal,

    @field:Size(max = 200, message = "적요는 200자를 초과할 수 없습니다")
    val description: String? = null
)
