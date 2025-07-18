package com.jinnara.accounting.application.port.command

import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType

data class CreateAccountCommand(
    val code: String,
    val name: String,
    val type: AccountType,
    val parentId: AccountId?
)
