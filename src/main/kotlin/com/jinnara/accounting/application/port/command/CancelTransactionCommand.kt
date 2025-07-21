package com.jinnara.accounting.application.port.command

import com.jinnara.accounting.domain.transaction.TransactionId

/**
 * 거래 취소 명령
 */
data class CancelTransactionCommand(
    val transactionId: TransactionId,
    val cancelReason: String
)
