package com.jinnara.accounting.domain.transaction

/**
 * 거래 ID를 표현하는 값 객체
 */
data class TransactionId(val value: Long) {
    init {
        require(value > 0) { "거래 ID는 0보다 큰 값이어야 합니다" }
    }
}
