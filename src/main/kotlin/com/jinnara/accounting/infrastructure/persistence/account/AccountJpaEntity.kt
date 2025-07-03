package com.jinnara.accounting.infrastructure.persistence.account

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "accounts")
class AccountJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val code: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: AccountType,
    
    @Column(name = "parent_id")
    val parentId: Long?,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): Account {
        return Account(
            id = AccountId(id),
            code = code,
            name = name,
            type = type,
            parentId = parentId?.let { AccountId(it) },
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomain(account: Account): AccountJpaEntity {
            return AccountJpaEntity(
                id = account.id.value,
                code = account.code,
                name = account.name,
                type = account.type,
                parentId = account.parentId?.value,
                isActive = account.isActive,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt
            )
        }
    }
}
