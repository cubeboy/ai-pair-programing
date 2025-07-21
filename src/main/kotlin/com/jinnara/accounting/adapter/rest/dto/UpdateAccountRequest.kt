package com.jinnara.accounting.adapter.rest.dto

import jakarta.validation.constraints.NotBlank

data class UpdateAccountRequest(
    @field:NotBlank(message = "계정명은 필수입니다")
    val name: String,

    val parentId: Long?
)
