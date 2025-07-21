package com.jinnara.accounting.adapter.rest

import com.jinnara.accounting.application.port.input.TransactionUseCase
import com.jinnara.accounting.domain.transaction.TransactionId
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.EntryType
import com.jinnara.accounting.adapter.rest.dto.*
import com.jinnara.accounting.application.port.command.*
import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(
    private val transactionUseCase: TransactionUseCase
) {

    @PostMapping
    fun createTransaction(@Valid @RequestBody request: CreateTransactionRequest): ResponseEntity<TransactionResponse> {
        val command = CreateTransactionCommand(
            description = request.description,
            date = request.transactionDate,
            reference = null, // reference 매개변수 추가
            entries = request.journalEntries.map { entry ->
                CreateTransactionEntryCommand(
                    accountId = AccountId(entry.accountId),
                    type = EntryType.valueOf(entry.entryType),
                    amount = entry.amount,
                    description = entry.description
                )
            }
        )

        val transaction = transactionUseCase.createTransaction(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TransactionResponse.fromDomain(transaction))
    }

    @PutMapping("/{id}")
    fun updateTransaction(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val command = UpdateTransactionCommand(
            transactionId = TransactionId(id),
            description = request.description,
            date = request.transactionDate,
            reference = null, // reference 매개변수 추가
            entries = request.journalEntries.map { entry ->
                CreateTransactionEntryCommand(
                    accountId = AccountId(entry.accountId),
                    type = EntryType.valueOf(entry.entryType),
                    amount = entry.amount,
                    description = entry.description
                )
            }
        )

        val transaction = transactionUseCase.updateTransaction(command)
        return ResponseEntity.ok(TransactionResponse.fromDomain(transaction))
    }

    @PostMapping("/{id}/cancel")
    fun cancelTransaction(
        @PathVariable id: Long,
        @Valid @RequestBody request: CancelTransactionRequest
    ): ResponseEntity<CancelTransactionResponse> {
        val command = CancelTransactionCommand(
            transactionId = TransactionId(id),
            cancelReason = request.cancelReason
        )

        val result = transactionUseCase.cancelTransaction(command)
        return ResponseEntity.ok(
            CancelTransactionResponse(
                originalTransaction = TransactionResponse.fromDomain(result.originalTransaction),
                reversalTransaction = TransactionResponse.fromDomain(result.reversalTransaction),
                cancelReason = result.cancelReason
            )
        )
    }

    @GetMapping("/{id}")
    fun getTransaction(@PathVariable id: Long): ResponseEntity<TransactionResponse> {
        val transaction = transactionUseCase.getTransaction(TransactionId(id))
        return ResponseEntity.ok(TransactionResponse.fromDomain(transaction))
    }

    @GetMapping
    fun getTransactions(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<TransactionPageResponse> {
        val query = GetTransactionsQuery(
            startDate = startDate,
            endDate = endDate,
            pageable = PageRequest.of(page, size)
        )

        val transactionPage = transactionUseCase.getTransactions(query)

        val response = TransactionPageResponse(
            content = transactionPage.content.map { TransactionResponse.fromDomain(it) },
            totalElements = transactionPage.totalElements,
            totalPages = transactionPage.totalPages,
            size = transactionPage.size,
            number = transactionPage.number,
            first = transactionPage.isFirst,
            last = transactionPage.isLast
        )

        return ResponseEntity.ok(response)
    }
}
