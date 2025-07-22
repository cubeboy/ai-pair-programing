package com.jinnara.accounting.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "거래 취소 요청")
data class CancelTransactionRequest(
    @field:NotBlank(message = "취소 사유는 필수입니다")
    @field:Size(max = 500, message = "취소 사유는 500자를 초과할 수 없습니다")
    @Schema(description = "취소 사유", example = "고객 요청으로 인한 취소", required = true, maxLength = 500)
    val cancelReason: String
)
