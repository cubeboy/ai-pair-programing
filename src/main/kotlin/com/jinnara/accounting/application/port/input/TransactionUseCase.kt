package com.jinnara.accounting.application.port.input

import com.jinnara.accounting.application.port.command.*
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import org.springframework.data.domain.Page

/**
 * 거래 관련 유즈케이스 인터페이스
 */
interface TransactionUseCase {

    /**
     * 새로운 거래를 생성합니다
     */
    fun createTransaction(command: CreateTransactionCommand): Transaction

    /**
     * 기존 거래를 수정합니다
     */
    fun updateTransaction(command: UpdateTransactionCommand): Transaction

    /**
     * 거래를 취소합니다
     */
    fun cancelTransaction(command: CancelTransactionCommand): CancelTransactionResult

    /**
     * 거래를 조회합니다
     */
    fun getTransaction(transactionId: TransactionId): Transaction

    /**
     * 거래 목록을 조회합니다
     */
    fun getTransactions(query: GetTransactionsQuery): Page<Transaction>
}
