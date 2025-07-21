package com.jinnara.accounting.application.port.command

import com.jinnara.accounting.domain.transaction.TransactionId
import java.time.LocalDate

/**
 * 거래 수정 명령
 */
data class UpdateTransactionCommand(
    val transactionId: TransactionId,
    val description: String,
    val date: LocalDate,
    val reference: String? = null,
    val entries: List<CreateTransactionEntryCommand>
)
