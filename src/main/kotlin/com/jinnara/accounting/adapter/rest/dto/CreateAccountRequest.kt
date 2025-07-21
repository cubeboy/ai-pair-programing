package com.jinnara.accounting.adapter.rest.dto

import com.jinnara.accounting.domain.account.AccountType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateAccountRequest(
    @field:NotBlank(message = "계정 코드는 필수입니다")
    val code: String,

    @field:NotBlank(message = "계정명은 필수입니다")
    val name: String,

    @field:NotNull(message = "계정 유형은 필수입니다")
    val type: AccountType,

    val parentId: Long?
)
