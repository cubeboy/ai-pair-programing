package com.jinnara.accounting.application.service

import com.jinnara.accounting.application.port.command.*
import com.jinnara.accounting.application.port.input.TransactionUseCase
import com.jinnara.accounting.application.port.output.AccountRepository
import com.jinnara.accounting.application.port.output.TransactionRepository
import com.jinnara.accounting.domain.transaction.*
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : TransactionUseCase {

    override fun createTransaction(command: CreateTransactionCommand): Transaction {
        // 거래 항목의 계정들이 모두 존재하는지 확인
        validateAccountsExist(command.entries.map { it.accountId })

        // 분개 항목 생성
        val journalEntries = command.entries.map { entryCommand ->
            val account = accountRepository.findById(entryCommand.accountId)
                ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: ${entryCommand.accountId}")

            JournalEntry(
                accountId = entryCommand.accountId,
                account = account,
                entryType = entryCommand.type,
                amount = entryCommand.amount,
                description = entryCommand.description
            )
        }

        // 거래 생성
        val transaction = Transaction(
            id = TransactionId(1), // 임시 ID, 저장시 실제 ID로 변경됨
            description = command.description,
            date = command.date,
            reference = command.reference,
            status = TransactionStatus.PENDING,
            entries = journalEntries,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return transactionRepository.save(transaction)
    }

    override fun updateTransaction(command: UpdateTransactionCommand): Transaction {
        val existingTransaction = transactionRepository.findById(command.transactionId)
            ?: throw IllegalArgumentException("거래를 찾을 수 없습니다: ${command.transactionId}")

        require(existingTransaction.status == TransactionStatus.PENDING) {
            "승인된 거래는 수정할 수 없습니다"
        }

        // 거래 항목의 계정들이 모두 존재하는지 확인
        validateAccountsExist(command.entries.map { it.accountId })

        // 새로운 분개 항목 생성
        val updatedEntries = command.entries.map { entryCommand ->
            val account = accountRepository.findById(entryCommand.accountId)
                ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: ${entryCommand.accountId}")

            JournalEntry(
                accountId = entryCommand.accountId,
                account = account,
                entryType = entryCommand.type,
                amount = entryCommand.amount,
                description = entryCommand.description
            )
        }

        // 거래 업데이트
        val updatedTransaction = existingTransaction.copy(
            description = command.description,
            date = command.date,
            reference = command.reference,
            entries = updatedEntries,
            updatedAt = LocalDateTime.now()
        )

        return transactionRepository.save(updatedTransaction)
    }

    override fun cancelTransaction(command: CancelTransactionCommand): CancelTransactionResult {
        val originalTransaction = transactionRepository.findById(command.transactionId)
            ?: throw IllegalArgumentException("거래를 찾을 수 없습니다: ${command.transactionId}")

        require(originalTransaction.status != TransactionStatus.CANCELLED) {
            "이미 취소된 거래입니다"
        }

        // 원본 거래를 취소 상태로 변경
        val cancelledTransaction = originalTransaction.copy(
            status = TransactionStatus.CANCELLED,
            updatedAt = LocalDateTime.now()
        )
        transactionRepository.save(cancelledTransaction)

        // 역분개 항목 생성 (차변과 대변을 반대로)
        val reversalEntries = originalTransaction.entries.map { entry ->
            entry.copy(
                entryType = if (entry.entryType == EntryType.DEBIT) EntryType.CREDIT else EntryType.DEBIT,
                description = "거래 취소: ${entry.description ?: ""}"
            )
        }

        // 역분개 거래 생성
        val reversalTransaction = Transaction(
            id = TransactionId(1),
            description = "거래 취소: ${originalTransaction.description}",
            date = originalTransaction.date,
            reference = "CANCEL-${originalTransaction.id.value}",
            status = TransactionStatus.APPROVED,
            entries = reversalEntries,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedReversalTransaction = transactionRepository.save(reversalTransaction)

        return CancelTransactionResult(
            originalTransaction = cancelledTransaction,
            reversalTransaction = savedReversalTransaction,
            cancelReason = command.cancelReason
        )
    }

    @Transactional(readOnly = true)
    override fun getTransaction(transactionId: TransactionId): Transaction {
        return transactionRepository.findById(transactionId)
            ?: throw IllegalArgumentException("거래를 찾을 수 없습니다: $transactionId")
    }

    @Transactional(readOnly = true)
    override fun getTransactions(query: GetTransactionsQuery): Page<Transaction> {
        return when {
            query.startDate != null && query.endDate != null -> {
                transactionRepository.findByDateRange(query.startDate, query.endDate, query.pageable)
            }
            else -> transactionRepository.findAll(query.pageable)
        }
    }

    private fun validateAccountsExist(accountIds: List<com.jinnara.accounting.domain.account.AccountId>) {
        accountIds.forEach { accountId ->
            val account = accountRepository.findById(accountId)
                ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: $accountId")

            require(account.isActive) {
                "비활성화된 계정입니다: ${account.name} (${account.code})"
            }
        }
    }
}
