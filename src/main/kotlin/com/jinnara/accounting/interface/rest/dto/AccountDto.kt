package com.jinnara.accounting.`interface`.rest.dto

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountType
import java.time.LocalDateTime

data class AccountResponse(
    val id: Long,
    val code: String,
    val name: String,
    val type: AccountType,
    val parentId: Long?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
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

data class CreateAccountRequest(
    val code: String,
    val name: String,
    val type: AccountType,
    val parentId: Long?
)

data class UpdateAccountRequest(
    val name: String,
    val parentId: Long?
)
