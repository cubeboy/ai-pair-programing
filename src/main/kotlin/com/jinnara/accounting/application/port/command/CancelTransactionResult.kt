package com.jinnara.accounting.application.port.command

import com.jinnara.accounting.domain.transaction.Transaction

/**
 * 거래 취소 결과
 */
data class CancelTransactionResult(
    val originalTransaction: Transaction,
    val reversalTransaction: Transaction,
    val cancelReason: String
)
