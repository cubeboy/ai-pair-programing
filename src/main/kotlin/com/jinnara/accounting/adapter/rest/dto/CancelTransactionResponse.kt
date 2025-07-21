package com.jinnara.accounting.adapter.rest.dto

data class CancelTransactionResponse(
    val originalTransaction: TransactionResponse,
    val reversalTransaction: TransactionResponse,
    val cancelReason: String
)
