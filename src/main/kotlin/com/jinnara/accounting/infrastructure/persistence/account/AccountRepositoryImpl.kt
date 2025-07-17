package com.jinnara.accounting.infrastructure.persistence.account

import com.jinnara.accounting.application.port.output.AccountRepository
import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import org.springframework.stereotype.Repository

@Repository
class AccountRepositoryImpl(
    private val jpaRepository: AccountJpaRepository
) : AccountRepository {

    override fun save(account: Account): Account {
        val entity = if (account.id.value == 0L) {
            // 새로운 엔티티는 ID를 0으로 설정하여 자동 생성되도록 함
            AccountJpaEntity.fromDomain(account).copy(id = 0)
        } else {
            AccountJpaEntity.fromDomain(account)
        }
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: AccountId): Account? {
        return jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByCode(code: String): Account? {
        return jpaRepository.findByCode(code)?.toDomain()
    }

    override fun findByType(type: AccountType): List<Account> {
        return jpaRepository.findByType(type).map { it.toDomain() }
    }

    override fun findAllActive(): List<Account> {
        return jpaRepository.findAllActiveOrderByCode().map { it.toDomain() }
    }

    override fun findByParentId(parentId: AccountId): List<Account> {
        return jpaRepository.findByParentId(parentId.value).map { it.toDomain() }
    }

    override fun existsByCode(code: String): Boolean {
        return jpaRepository.existsByCode(code)
    }

    override fun delete(id: AccountId) {
        jpaRepository.deleteById(id.value)
    }
}
