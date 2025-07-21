package com.jinnara.accounting.adapter.rest.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CancelTransactionRequest(
    @field:NotBlank(message = "취소 사유는 필수입니다")
    @field:Size(max = 500, message = "취소 사유는 500자를 초과할 수 없습니다")
    val cancelReason: String
)
