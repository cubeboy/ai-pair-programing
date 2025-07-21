package com.jinnara.accounting.domain.transaction

/**
 * 거래 상태를 나타내는 열거형
 */
enum class TransactionStatus {
    /** 대기 중 */
    PENDING,

    /** 승인됨 */
    APPROVED,

    /** 취소됨 */
    CANCELLED,

    /** 마감됨 */
    CLOSED
}

/**
 * 분개 유형 (차변/대변)
 */
enum class EntryType {
    /** 차변 */
    DEBIT,

    /** 대변 */
    CREDIT
}
