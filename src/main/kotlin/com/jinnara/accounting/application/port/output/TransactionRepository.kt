package com.jinnara.accounting.application.port.output

import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: TransactionId): Transaction?
    fun findByAccountId(accountId: AccountId): List<Transaction>
    fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findAll(): List<Transaction>
    fun delete(id: TransactionId)

    // 페이징 지원 메서드 추가
    fun findAll(pageable: Pageable): Page<Transaction>
    fun findByDateRange(startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Transaction>
}
