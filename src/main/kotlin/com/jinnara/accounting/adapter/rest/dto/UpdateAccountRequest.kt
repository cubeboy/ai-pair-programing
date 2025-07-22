package com.jinnara.accounting.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "계정 수정 요청")
data class UpdateAccountRequest(
    @field:NotBlank(message = "계정명은 필수입니다")
    @Schema(description = "계정명", example = "수정된 현금", required = true)
    val name: String,

    @Schema(description = "상위 계정 ID (선택사항, null로 설정하면 최상위 계정이 됨)", example = "1")
    val parentId: Long?
)
