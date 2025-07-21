package com.jinnara.accounting.infrastructure.persistence.transaction

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.domain.transaction.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@Import(TransactionRepositoryImpl::class)
@DisplayName("TransactionRepositoryImpl 통합 테스트")
class TransactionRepositoryImplTest {

    @Autowired
    private lateinit var transactionRepository: TransactionRepositoryImpl

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testAccount1: Account
    private lateinit var testAccount2: Account
    private lateinit var testTransaction: Transaction

    @BeforeEach
    fun setUp() {
        val now = LocalDateTime.now()

        // 테스트용 계정 생성
        testAccount1 = Account(
            id = AccountId(1L),
            code = "1100",
            name = "현금",
            type = AccountType.ASSET,
            parentId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        testAccount2 = Account(
            id = AccountId(2L),
            code = "4100",
            name = "매출",
            type = AccountType.REVENUE,
            parentId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        // 테스트용 거래 생성
        val journalEntries = listOf(
            JournalEntry(
                accountId = testAccount1.id,
                account = testAccount1,
                entryType = EntryType.DEBIT,
                amount = BigDecimal("100000.00"),
                description = "현금 수취"
            ),
            JournalEntry(
                accountId = testAccount2.id,
                account = testAccount2,
                entryType = EntryType.CREDIT,
                amount = BigDecimal("100000.00"),
                description = "매출 발생"
            )
        )

        testTransaction = Transaction(
            id = TransactionId(0L),
            description = "상품 판매",
            date = LocalDate.now(),
            reference = "REF-001",
            status = TransactionStatus.APPROVED,
            entries = journalEntries,
            createdAt = now,
            updatedAt = now
        )
    }

    @Nested
    @DisplayName("거래 저장 테스트")
    inner class SaveTransactionTest {

        @Test
        @DisplayName("새로운 거래를 저장할 수 있다")
        fun `새로운 거래를 저장할 수 있다`() {
            // when
            val savedTransaction = transactionRepository.save(testTransaction)

            // then
            assertThat(savedTransaction.id.value).isGreaterThan(0L)
            assertThat(savedTransaction.description).isEqualTo(testTransaction.description)
            assertThat(savedTransaction.date).isEqualTo(testTransaction.date)
            assertThat(savedTransaction.status).isEqualTo(testTransaction.status)
            assertThat(savedTransaction.entries).hasSize(2)

            // 분개 항목 검증
            val debitEntry = savedTransaction.entries.find { it.entryType == EntryType.DEBIT }
            val creditEntry = savedTransaction.entries.find { it.entryType == EntryType.CREDIT }

            assertThat(debitEntry).isNotNull
            assertThat(debitEntry?.amount).isEqualTo(BigDecimal("100000.00"))
            assertThat(debitEntry?.accountId).isEqualTo(testAccount1.id)

            assertThat(creditEntry).isNotNull
            assertThat(creditEntry?.amount).isEqualTo(BigDecimal("100000.00"))
            assertThat(creditEntry?.accountId).isEqualTo(testAccount2.id)
        }

        @Test
        @DisplayName("기존 거래를 업데이트할 수 있다")
        fun `기존 거래를 업데이트할 수 있다`() {
            // given
            val savedTransaction = transactionRepository.save(testTransaction)
            val updatedTransaction = savedTransaction.copy(
                description = "수정된 거래",
                status = TransactionStatus.CLOSED,
                updatedAt = LocalDateTime.now()
            )

            // when
            val result = transactionRepository.save(updatedTransaction)

            // then
            assertThat(result.id).isEqualTo(savedTransaction.id)
            assertThat(result.description).isEqualTo("수정된 거래")
            assertThat(result.status).isEqualTo(TransactionStatus.CLOSED)
            assertThat(result.entries).hasSize(2)
        }

        @Test
        @DisplayName("복잡한 분개 항목을 가진 거래를 저장할 수 있다")
        fun `복잡한 분개 항목을 가진 거래를 저장할 수 있다`() {
            // given
            val account3 = Account(
                id = AccountId(3L),
                code = "2100",
                name = "부가세예수금",
                type = AccountType.LIABILITY,
                parentId = null,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val complexEntries = listOf(
                JournalEntry(
                    accountId = testAccount1.id,
                    account = testAccount1,
                    entryType = EntryType.DEBIT,
                    amount = BigDecimal("110000.00"),
                    description = "현금 수취 (부가세 포함)"
                ),
                JournalEntry(
                    accountId = testAccount2.id,
                    account = testAccount2,
                    entryType = EntryType.CREDIT,
                    amount = BigDecimal("100000.00"),
                    description = "매출 발생"
                ),
                JournalEntry(
                    accountId = account3.id,
                    account = account3,
                    entryType = EntryType.CREDIT,
                    amount = BigDecimal("10000.00"),
                    description = "부가세 예수"
                )
            )

            val complexTransaction = testTransaction.copy(
                description = "부가세 포함 매출",
                entries = complexEntries
            )

            // when
            val savedTransaction = transactionRepository.save(complexTransaction)

            // then
            assertThat(savedTransaction.entries).hasSize(3)
            assertThat(savedTransaction.totalAmount).isEqualTo(BigDecimal("110000.00"))

            val debitTotal = savedTransaction.entries
                .filter { it.entryType == EntryType.DEBIT }
                .sumOf { it.amount }
            val creditTotal = savedTransaction.entries
                .filter { it.entryType == EntryType.CREDIT }
                .sumOf { it.amount }

            assertThat(debitTotal).isEqualTo(creditTotal)
        }
    }

    @Nested
    @DisplayName("거래 조회 테스트")
    inner class FindTransactionTest {

        @Test
        @DisplayName("ID로 거래를 조회할 수 있다")
        fun `ID로 거래를 조회할 수 있다`() {
            // given
            val savedTransaction = transactionRepository.save(testTransaction)

            // when
            val foundTransaction = transactionRepository.findById(savedTransaction.id)

            // then
            assertThat(foundTransaction).isNotNull
            assertThat(foundTransaction?.id).isEqualTo(savedTransaction.id)
            assertThat(foundTransaction?.description).isEqualTo(savedTransaction.description)
            assertThat(foundTransaction?.entries).hasSize(2)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `존재하지 않는 ID로 조회하면 null을 반환한다`() {
            // when
            val foundTransaction = transactionRepository.findById(TransactionId(999L))

            // then
            assertThat(foundTransaction).isNull()
        }

        @Test
        @DisplayName("계정 ID로 거래를 조회할 수 있다")
        fun `계정 ID로 거래를 조회할 수 있다`() {
            // given
            val savedTransaction = transactionRepository.save(testTransaction)

            // when
            val transactions = transactionRepository.findByAccountId(testAccount1.id)

            // then
            assertThat(transactions).hasSize(1)
            assertThat(transactions[0].id).isEqualTo(savedTransaction.id)
        }

        @Test
        @DisplayName("날짜 범위로 거래를 조회할 수 있다")
        fun `날짜 범위로 거래를 조회할 수 있다`() {
            // given
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val tomorrow = today.plusDays(1)

            val todayTransaction = testTransaction.copy(date = today)
            val yesterdayTransaction = testTransaction.copy(
                description = "어제 거래",
                date = yesterday
            )

            transactionRepository.save(todayTransaction)
            transactionRepository.save(yesterdayTransaction)

            // when
            val todayTransactions = transactionRepository.findByDateRange(today, today)
            val allTransactions = transactionRepository.findByDateRange(yesterday, tomorrow)

            // then
            assertThat(todayTransactions).hasSize(1)
            assertThat(todayTransactions[0].date).isEqualTo(today)

            assertThat(allTransactions).hasSize(2)
        }

        @Test
        @DisplayName("모든 거래를 조회할 수 있다")
        fun `모든 거래를 조회할 수 있다`() {
            // given
            val transaction1 = testTransaction.copy(description = "거래1")
            val transaction2 = testTransaction.copy(description = "거래2")

            transactionRepository.save(transaction1)
            transactionRepository.save(transaction2)

            // when
            val allTransactions = transactionRepository.findAll()

            // then
            assertThat(allTransactions).hasSize(2)
        }
    }

    @Nested
    @DisplayName("페이징 조회 테스트")
    inner class PagingTest {

        @Test
        @DisplayName("페이징으로 모든 거래를 조회할 수 있다")
        fun `페이징으로 모든 거래를 조회할 수 있다`() {
            // given
            repeat(5) { index ->
                val transaction = testTransaction.copy(description = "거래 $index")
                transactionRepository.save(transaction)
            }

            val pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "date", "id"))

            // when
            val page = transactionRepository.findAll(pageable)

            // then
            assertThat(page.content).hasSize(3)
            assertThat(page.totalElements).isEqualTo(5)
            assertThat(page.totalPages).isEqualTo(2)
            assertThat(page.isFirst).isTrue
            assertThat(page.hasNext()).isTrue
        }

        @Test
        @DisplayName("날짜 범위로 페이징 조회할 수 있다")
        fun `날짜 범위로 페이징 조회할 수 있다`() {
            // given
            val today = LocalDate.now()
            repeat(3) { index ->
                val transaction = testTransaction.copy(
                    description = "오늘 거래 $index",
                    date = today
                )
                transactionRepository.save(transaction)
            }

            repeat(2) { index ->
                val transaction = testTransaction.copy(
                    description = "어제 거래 $index",
                    date = today.minusDays(1)
                )
                transactionRepository.save(transaction)
            }

            val pageable = PageRequest.of(0, 2)

            // when
            val page = transactionRepository.findByDateRange(today, today, pageable)

            // then
            assertThat(page.content).hasSize(2)
            assertThat(page.totalElements).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("거래 삭제 테스트")
    inner class DeleteTransactionTest {

        @Test
        @DisplayName("거래를 삭제할 수 있다")
        fun `거래를 삭제할 수 있다`() {
            // given
            val savedTransaction = transactionRepository.save(testTransaction)

            // when
            transactionRepository.delete(savedTransaction.id)

            // then
            val foundTransaction = transactionRepository.findById(savedTransaction.id)
            assertThat(foundTransaction).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 거래 삭제 시 예외가 발생하지 않는다")
        fun `존재하지 않는 거래 삭제 시 예외가 발생하지 않는다`() {
            // when & then
            assertThatCode {
                transactionRepository.delete(TransactionId(999L))
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("매핑 관련 테스트")
    inner class MappingTest {

        @Test
        @DisplayName("도메인 엔티티에서 JPA 엔티티로 매핑이 정확히 동작한다")
        fun `도메인 엔티티에서 JPA 엔티티로 매핑이 정확히 동작한다`() {
            // when
            val jpaEntity = TransactionJpaEntity.fromDomain(testTransaction)

            // then
            assertThat(jpaEntity.description).isEqualTo(testTransaction.description)
            assertThat(jpaEntity.date).isEqualTo(testTransaction.date)
            assertThat(jpaEntity.status).isEqualTo(testTransaction.status)
            assertThat(jpaEntity.entries).hasSize(2)

            val debitEntry = jpaEntity.entries.find { it.entryType == EntryType.DEBIT }
            assertThat(debitEntry?.amount).isEqualTo(BigDecimal("100000.00"))
            assertThat(debitEntry?.accountCode).isEqualTo("1100")
        }

        @Test
        @DisplayName("JPA 엔티티에서 도메인 엔티티로 매핑이 정확히 동작한다")
        fun `JPA 엔티티에서 도메인 엔티티로 매핑이 정확히 동작한다`() {
            // given
            val savedTransaction = transactionRepository.save(testTransaction)

            // when
            val domain = savedTransaction

            // then
            assertThat(domain.description).isEqualTo(testTransaction.description)
            assertThat(domain.entries).hasSize(2)
            assertThat(domain.totalAmount).isEqualTo(BigDecimal("100000.00"))

            // 복식부기 검증
            val debitTotal = domain.entries
                .filter { it.entryType == EntryType.DEBIT }
                .sumOf { it.amount }
            val creditTotal = domain.entries
                .filter { it.entryType == EntryType.CREDIT }
                .sumOf { it.amount }

            assertThat(debitTotal).isEqualTo(creditTotal)
        }
    }

    @Nested
    @DisplayName("비즈니스 규칙 테스트")
    inner class BusinessRuleTest {

        @Test
        @DisplayName("저장된 거래는 복식부기 원칙을 준수한다")
        fun `저장된 거래는 복식부기 원칙을 준수한다`() {
            // given
            val savedTransaction = transactionRepository.save(testTransaction)

            // when
            val foundTransaction = transactionRepository.findById(savedTransaction.id)

            // then
            assertThat(foundTransaction).isNotNull
            val debitTotal = foundTransaction!!.entries
                .filter { it.entryType == EntryType.DEBIT }
                .sumOf { it.amount }
            val creditTotal = foundTransaction.entries
                .filter { it.entryType == EntryType.CREDIT }
                .sumOf { it.amount }

            assertThat(debitTotal).isEqualTo(creditTotal)
        }

        @Test
        @DisplayName("거래 상태별로 필터링할 수 있다")
        fun `거래 상태별로 필터링할 수 있다`() {
            // given
            val approvedTransaction = testTransaction.copy(
                description = "승인된 거래",
                status = TransactionStatus.APPROVED
            )
            val pendingTransaction = testTransaction.copy(
                description = "대기 중인 거래",
                status = TransactionStatus.PENDING
            )

            transactionRepository.save(approvedTransaction)
            transactionRepository.save(pendingTransaction)

            // when
            val allTransactions = transactionRepository.findAll()

            // then
            assertThat(allTransactions).hasSize(2)

            val approvedTransactions = allTransactions.filter { it.status == TransactionStatus.APPROVED }
            val pendingTransactions = allTransactions.filter { it.status == TransactionStatus.PENDING }

            assertThat(approvedTransactions).hasSize(1)
            assertThat(pendingTransactions).hasSize(1)
        }
    }
}
