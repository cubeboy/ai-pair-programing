package com.jinnara.accounting.infrastructure.persistence.transaction

import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import com.jinnara.accounting.domain.transaction.TransactionStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
class TransactionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 500)
    var description: String,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(length = 100)
    var reference: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TransactionStatus,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "transaction", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var entries: MutableList<JournalEntryJpaEntity> = mutableListOf()
) {

    fun updateWith(block: TransactionJpaEntity.() -> Unit): TransactionJpaEntity {
        return this.apply(block).also {
            this.updatedAt = LocalDateTime.now()
        }
    }

    fun updateDescription(newDescription: String) {
        this.description = newDescription
        this.updatedAt = LocalDateTime.now()
    }

    fun updateStatus(newStatus: TransactionStatus) {
        this.status = newStatus
        this.updatedAt = LocalDateTime.now()
    }

    fun toDomain(): Transaction {
        return Transaction(
            id = TransactionId(id),
            description = description,
            date = date,
            reference = reference,
            status = status,
            entries = entries.map { entry -> entry.toDomain() },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: Transaction): TransactionJpaEntity {
            val entity = TransactionJpaEntity(
                id = if (domain.id.value == 0L) 0 else domain.id.value,
                description = domain.description,
                date = domain.date,
                reference = domain.reference,
                status = domain.status,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )

            // 분개 항목들을 설정
            entity.entries = domain.entries.map { journalEntry ->
                JournalEntryJpaEntity.fromDomain(journalEntry, entity)
            }.toMutableList()

            return entity
        }
    }

    fun updateFrom(domain: Transaction): TransactionJpaEntity {
        return this.updateWith {
            description = domain.description
            date = domain.date
            reference = domain.reference
            status = domain.status

            // 기존 분개 항목들을 제거하고 새로운 항목들로 교체
            entries.clear()
            entries.addAll(domain.entries.map { journalEntry ->
                JournalEntryJpaEntity.fromDomain(journalEntry, this)
            })
        }
    }
}
