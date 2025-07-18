package com.jinnara.accounting.application.port.command

import com.jinnara.accounting.domain.account.AccountId

data class UpdateAccountCommand(
    val accountId: AccountId,
    val name: String,
    val parentId: AccountId?
)
