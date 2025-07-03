package com.jinnara.accounting.application.port.out

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType

interface AccountRepository {
    fun save(account: Account): Account
    fun findById(id: AccountId): Account?
    fun findByCode(code: String): Account?
    fun findByType(type: AccountType): List<Account>
    fun findAllActive(): List<Account>
    fun findByParentId(parentId: AccountId): List<Account>
    fun existsByCode(code: String): Boolean
    fun delete(id: AccountId)
}
