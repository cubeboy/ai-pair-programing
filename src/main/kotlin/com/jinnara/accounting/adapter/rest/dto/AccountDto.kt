package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "계정 응답")
data class AccountResponse(
    @Schema(description = "계정 ID", example = "1")
    val id: Long,

    @Schema(description = "계정 코드", example = "1000")
    val code: String,

    @Schema(description = "계정명", example = "현금")
    val name: String,

    @Schema(description = "계정 유형", example = "ASSET", allowableValues = ["ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"])
    val type: AccountType,

    @Schema(description = "상위 계정 ID (없으면 null)", example = "1")
    val parentId: Long?,

    @Schema(description = "활성 상태", example = "true")
    val isActive: Boolean,

    @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 일시", example = "2024-01-01T10:00:00")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromDomain(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id.value,
                code = account.code,
                name = account.name,
                type = account.type,
                parentId = account.parentId?.value,
                isActive = account.isActive,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt
            )
        }
    }
}
