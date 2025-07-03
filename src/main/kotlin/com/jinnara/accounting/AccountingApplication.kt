package com.jinnara.accounting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 회계 관리 솔루션 메인 애플리케이션
 * 헥사고날 아키텍처 패턴으로 구성됨
 */
@SpringBootApplication
class AccountingApplication

fun main(args: Array<String>) {
	runApplication<AccountingApplication>(*args)
}
