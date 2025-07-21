package com.jinnara.accounting.adapter.rest.dto

data class TransactionPageResponse(
    val content: List<TransactionResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val last: Boolean
)
