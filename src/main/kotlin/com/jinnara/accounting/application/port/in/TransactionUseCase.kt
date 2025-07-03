package com.jinnara.accounting.application.port.`in`

import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.EntryType
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import java.math.BigDecimal
import java.time.LocalDate

interface TransactionUseCase {
    fun createTransaction(command: CreateTransactionCommand): Transaction
    fun postTransaction(transactionId: TransactionId): Transaction
    fun reverseTransaction(transactionId: TransactionId): Transaction
    fun getTransaction(transactionId: TransactionId): Transaction
    fun getTransactionsByAccount(accountId: AccountId): List<Transaction>
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction>
}

data class CreateTransactionCommand(
    val date: LocalDate,
    val description: String,
    val reference: String?,
    val entries: List<CreateTransactionEntryCommand>
)

data class CreateTransactionEntryCommand(
    val accountId: AccountId,
    val type: EntryType,
    val amount: BigDecimal,
    val description: String?
)
