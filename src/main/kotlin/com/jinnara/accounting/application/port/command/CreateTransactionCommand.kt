package com.jinnara.accounting.application.port.command

import java.time.LocalDate

data class CreateTransactionCommand(
    val date: LocalDate,
    val description: String,
    val reference: String?,
    val entries: List<CreateTransactionEntryCommand>
)
