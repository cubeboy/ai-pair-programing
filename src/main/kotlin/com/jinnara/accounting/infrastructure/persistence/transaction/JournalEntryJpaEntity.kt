package com.jinnara.accounting.infrastructure.persistence.transaction

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.domain.transaction.EntryType
import com.jinnara.accounting.domain.transaction.JournalEntry
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "journal_entries")
class JournalEntryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    var transaction: TransactionJpaEntity,

    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var entryType: EntryType,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Column(length = 200)
    var description: String? = null,

    // Account 정보를 저장 (비정규화)
    @Column(name = "account_code", nullable = false)
    var accountCode: String,

    @Column(name = "account_name", nullable = false)
    var accountName: String
) {

    fun toDomain(): JournalEntry {
        // Account 객체를 재구성 (비정규화된 데이터로부터)
        val account = Account(
            id = AccountId(accountId),
            code = accountCode,
            name = accountName,
            type = AccountType.ASSET, // 기본값, 실제로는 별도 테이블에서 조회 필요
            parentId = null,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return JournalEntry(
            accountId = AccountId(accountId),
            account = account,
            entryType = entryType,
            amount = amount,
            description = description
        )
    }

    companion object {
        fun fromDomain(domain: JournalEntry, transaction: TransactionJpaEntity): JournalEntryJpaEntity {
            return JournalEntryJpaEntity(
                transaction = transaction,
                accountId = domain.accountId.value,
                entryType = domain.entryType,
                amount = domain.amount,
                description = domain.description,
                accountCode = domain.account.code,
                accountName = domain.account.name
            )
        }
    }
}
