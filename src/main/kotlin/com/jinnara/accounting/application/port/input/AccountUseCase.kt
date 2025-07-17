package com.jinnara.accounting.application.port.input

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType

interface AccountUseCase {
    fun createAccount(command: CreateAccountCommand): Account
    fun updateAccount(command: UpdateAccountCommand): Account
    fun deactivateAccount(accountId: AccountId): Account
    fun getAccount(accountId: AccountId): Account
    fun getAccountsByType(type: AccountType): List<Account>
    fun getAllActiveAccounts(): List<Account>
}

data class CreateAccountCommand(
    val code: String,
    val name: String,
    val type: AccountType,
    val parentId: AccountId?
)

data class UpdateAccountCommand(
    val accountId: AccountId,
    val name: String,
    val parentId: AccountId?
)
