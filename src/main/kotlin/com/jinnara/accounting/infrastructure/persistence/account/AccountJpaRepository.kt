package com.jinnara.accounting.infrastructure.persistence.account

import com.jinnara.accounting.domain.account.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AccountJpaRepository : JpaRepository<AccountJpaEntity, Long> {
    fun findByCode(code: String): AccountJpaEntity?
    fun findByType(type: AccountType): List<AccountJpaEntity>
    fun findByIsActiveTrue(): List<AccountJpaEntity>
    fun findByParentId(parentId: Long): List<AccountJpaEntity>
    fun existsByCode(code: String): Boolean
    
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.isActive = true ORDER BY a.code")
    fun findAllActiveOrderByCode(): List<AccountJpaEntity>
}
