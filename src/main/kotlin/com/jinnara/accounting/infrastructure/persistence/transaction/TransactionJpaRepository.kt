package com.jinnara.accounting.infrastructure.persistence.transaction

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface TransactionJpaRepository : JpaRepository<TransactionJpaEntity, Long> {

    @Query("SELECT t FROM TransactionJpaEntity t JOIN FETCH t.entries WHERE t.id = :id")
    fun findByIdWithEntries(@Param("id") id: Long): TransactionJpaEntity?

    @Query("SELECT t FROM TransactionJpaEntity t JOIN t.entries e WHERE e.accountId = :accountId")
    fun findByAccountId(@Param("accountId") accountId: Long): List<TransactionJpaEntity>

    @Query("SELECT t FROM TransactionJpaEntity t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC, t.id DESC")
    fun findByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<TransactionJpaEntity>

    @Query("SELECT t FROM TransactionJpaEntity t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC, t.id DESC")
    fun findByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        pageable: Pageable
    ): Page<TransactionJpaEntity>

    @Query("SELECT t FROM TransactionJpaEntity t ORDER BY t.date DESC, t.id DESC")
    override fun findAll(): List<TransactionJpaEntity>

    @Query("SELECT t FROM TransactionJpaEntity t ORDER BY t.date DESC, t.id DESC")
    override fun findAll(pageable: Pageable): Page<TransactionJpaEntity>
}
