package com.jinnara.accounting.application.port.command

import org.springframework.data.domain.Pageable
import java.time.LocalDate

/**
 * 거래 목록 조회 쿼리
 */
data class GetTransactionsQuery(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val pageable: Pageable
)
