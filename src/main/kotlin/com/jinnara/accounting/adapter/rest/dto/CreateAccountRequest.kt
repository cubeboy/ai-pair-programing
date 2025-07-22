package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.account.AccountType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "계정 생성 요청")
data class CreateAccountRequest(
    @field:NotBlank(message = "계정 코드는 필수입니다")
    @Schema(description = "계정 코드", example = "1000", required = true)
    val code: String,

    @field:NotBlank(message = "계정명은 필수입니다")
    @Schema(description = "계정명", example = "현금", required = true)
    val name: String,

    @field:NotNull(message = "계정 유형은 필수입니다")
    @Schema(description = "계정 유형", example = "ASSET", required = true, allowableValues = ["ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"])
    val type: AccountType,

    @Schema(description = "상위 계정 ID (선택사항)", example = "1")
    val parentId: Long?
)
