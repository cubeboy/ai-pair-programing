package com.jinnara.accounting.infrastructure.persistence.transaction

import com.jinnara.accounting.application.port.output.TransactionRepository
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class TransactionRepositoryImpl(
    private val jpaRepository: TransactionJpaRepository
) : TransactionRepository {

    override fun save(transaction: Transaction): Transaction {
        val existingEntity = if (transaction.id.value > 0) {
            jpaRepository.findByIdWithEntries(transaction.id.value)
        } else null

        val entity = if (existingEntity != null) {
            // 기존 엔티티 업데이트
            existingEntity.updateFrom(transaction)
        } else {
            // 새로운 엔티티 생성
            TransactionJpaEntity.fromDomain(transaction)
        }

        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: TransactionId): Transaction? {
        return jpaRepository.findByIdWithEntries(id.value)?.toDomain()
    }

    override fun findByAccountId(accountId: AccountId): List<Transaction> {
        return jpaRepository.findByAccountId(accountId.value)
            .map { it.toDomain() }
    }

    override fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> {
        return jpaRepository.findByDateRange(startDate, endDate)
            .map { it.toDomain() }
    }

    override fun findAll(): List<Transaction> {
        return jpaRepository.findAll()
            .map { it.toDomain() }
    }

    override fun delete(id: TransactionId) {
        if (jpaRepository.existsById(id.value)) {
            jpaRepository.deleteById(id.value)
        }
    }

    override fun findAll(pageable: Pageable): Page<Transaction> {
        return jpaRepository.findAll(pageable)
            .map { it.toDomain() }
    }

    override fun findByDateRange(startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Transaction> {
        return jpaRepository.findByDateRange(startDate, endDate, pageable)
            .map { it.toDomain() }
    }
}
