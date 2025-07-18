package com.jinnara.accounting.`interface`.rest

import com.jinnara.accounting.application.port.input.AccountUseCase
import com.jinnara.accounting.application.port.input.CreateAccountCommand
import com.jinnara.accounting.application.port.input.UpdateAccountCommand
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.`interface`.rest.dto.AccountResponse
import com.jinnara.accounting.`interface`.rest.dto.CreateAccountRequest
import com.jinnara.accounting.`interface`.rest.dto.UpdateAccountRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountUseCase: AccountUseCase
) {

    @PostMapping
    fun createAccount(@Valid @RequestBody request: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val command = CreateAccountCommand(
            code = request.code,
            name = request.name,
            type = request.type,
            parentId = request.parentId?.let { AccountId(it) }
        )

        val account = accountUseCase.createAccount(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AccountResponse.fromDomain(account))
    }

    @PutMapping("/{id}")
    fun updateAccount(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAccountRequest
    ): ResponseEntity<AccountResponse> {
        val command = UpdateAccountCommand(
            accountId = AccountId(id),
            name = request.name,
            parentId = request.parentId?.let { AccountId(it) }
        )

        val account = accountUseCase.updateAccount(command)
        return ResponseEntity.ok(AccountResponse.fromDomain(account))
    }

    @DeleteMapping("/{id}")
    fun deactivateAccount(@PathVariable id: Long): ResponseEntity<AccountResponse> {
        val account = accountUseCase.deactivateAccount(AccountId(id))
        return ResponseEntity.ok(AccountResponse.fromDomain(account))
    }

    @GetMapping("/{id}")
    fun getAccount(@PathVariable id: Long): ResponseEntity<AccountResponse> {
        val account = accountUseCase.getAccount(AccountId(id))
        return ResponseEntity.ok(AccountResponse.fromDomain(account))
    }

    @GetMapping
    fun getAccounts(@RequestParam(required = false) type: AccountType?): ResponseEntity<List<AccountResponse>> {
        val accounts = if (type != null) {
            accountUseCase.getAccountsByType(type)
        } else {
            accountUseCase.getAllActiveAccounts()
        }

        val response = accounts.map { AccountResponse.fromDomain(it) }
        return ResponseEntity.ok(response)
    }
}
