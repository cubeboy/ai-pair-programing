package com.jinnara.accounting.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "거래 취소 응답")
data class CancelTransactionResponse(
    @Schema(description = "원본 거래 정보 (취소된 거래)")
    val originalTransaction: TransactionResponse,

    @Schema(description = "역분개 거래 정보 (취소를 위해 생성된 거래)")
    val reversalTransaction: TransactionResponse,

    @Schema(description = "취소 사유", example = "고객 요청으로 인한 취소")
    val cancelReason: String
)
