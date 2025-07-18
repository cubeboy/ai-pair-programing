package com.jinnara.accounting.application.port.input

import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import com.jinnara.accounting.application.port.command.CreateTransactionCommand
import java.time.LocalDate

interface TransactionUseCase {
    fun createTransaction(command: CreateTransactionCommand): Transaction
    fun postTransaction(transactionId: TransactionId): Transaction
    fun reverseTransaction(transactionId: TransactionId): Transaction
    fun getTransaction(transactionId: TransactionId): Transaction
    fun getTransactionsByAccount(accountId: AccountId): List<Transaction>
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction>
}
