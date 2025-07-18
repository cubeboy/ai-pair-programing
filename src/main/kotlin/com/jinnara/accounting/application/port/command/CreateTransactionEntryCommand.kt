package com.jinnara.accounting.application.port.command

import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.EntryType
import java.math.BigDecimal

data class CreateTransactionEntryCommand(
    val accountId: AccountId,
    val type: EntryType,
    val amount: BigDecimal,
    val description: String?
)
