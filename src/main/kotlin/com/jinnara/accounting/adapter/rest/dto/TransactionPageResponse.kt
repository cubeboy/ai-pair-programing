package com.jinnara.accounting.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "거래 목록 페이징 응답")
data class TransactionPageResponse(
    @Schema(description = "현재 페이지의 거래 목록")
    val content: List<TransactionResponse>,

    @Schema(description = "전체 거래 건수", example = "100")
    val totalElements: Long,

    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,

    @Schema(description = "페이지 크기", example = "20")
    val size: Int,

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val number: Int,

    @Schema(description = "첫 번째 페이지 여부", example = "true")
    val first: Boolean,

    @Schema(description = "마지막 페이지 여부", example = "false")
    val last: Boolean
)
