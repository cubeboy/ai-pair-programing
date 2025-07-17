package com.jinnara.accounting.application.port.output

import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import java.time.LocalDate

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: TransactionId): Transaction?
    fun findByAccountId(accountId: AccountId): List<Transaction>
    fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findAll(): List<Transaction>
    fun delete(id: TransactionId)
}
