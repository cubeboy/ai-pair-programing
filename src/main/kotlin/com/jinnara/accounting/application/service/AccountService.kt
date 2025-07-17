package com.jinnara.accounting.application.service

import com.jinnara.accounting.application.port.input.AccountUseCase
import com.jinnara.accounting.application.port.input.CreateAccountCommand
import com.jinnara.accounting.application.port.input.UpdateAccountCommand
import com.jinnara.accounting.application.port.output.AccountRepository
import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository
) : AccountUseCase {

    override fun createAccount(command: CreateAccountCommand): Account {
        require(!accountRepository.existsByCode(command.code)) {
            "계정코드 '${command.code}'가 이미 존재합니다."
        }
        
        command.parentId?.let { parentId ->
            val parent = accountRepository.findById(parentId)
                ?: throw IllegalArgumentException("상위 계정을 찾을 수 없습니다: $parentId")
            require(parent.isActive) { "비활성화된 상위 계정입니다." }
        }

        val account = Account(
            id = AccountId(0), // ID는 저장시 자동 생성
            code = command.code,
            name = command.name,
            type = command.type,
            parentId = command.parentId
        )
        
        return accountRepository.save(account)
    }

    override fun updateAccount(command: UpdateAccountCommand): Account {
        val account = accountRepository.findById(command.accountId)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: ${command.accountId}")
        
        command.parentId?.let { parentId ->
            val parent = accountRepository.findById(parentId)
                ?: throw IllegalArgumentException("상위 계정을 찾을 수 없습니다: $parentId")
            require(parent.isActive) { "비활성화된 상위 계정입니다." }
            require(parentId != command.accountId) { "자기 자신을 상위 계정으로 설정할 수 없습니다." }
        }

        val updatedAccount = account.copy(
            name = command.name,
            parentId = command.parentId
        )
        
        return accountRepository.save(updatedAccount)
    }

    override fun deactivateAccount(accountId: AccountId): Account {
        val account = accountRepository.findById(accountId)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: $accountId")
        
        val children = accountRepository.findByParentId(accountId)
        require(children.isEmpty()) { "하위 계정이 존재하는 계정은 비활성화할 수 없습니다." }
        
        val deactivatedAccount = account.deactivate()
        return accountRepository.save(deactivatedAccount)
    }

    @Transactional(readOnly = true)
    override fun getAccount(accountId: AccountId): Account {
        return accountRepository.findById(accountId)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: $accountId")
    }

    @Transactional(readOnly = true)
    override fun getAccountsByType(type: AccountType): List<Account> {
        return accountRepository.findByType(type)
    }

    @Transactional(readOnly = true)
    override fun getAllActiveAccounts(): List<Account> {
        return accountRepository.findAllActive()
    }
}
