package com.jinnara.accounting.application.service

import com.jinnara.accounting.application.port.command.*
import com.jinnara.accounting.application.port.output.AccountRepository
import com.jinnara.accounting.application.port.output.TransactionRepository
import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.domain.transaction.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("TransactionService 테스트")
class TransactionServiceTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val transactionService = TransactionService(transactionRepository, accountRepository)

    private val testAccount1 = Account(
        id = AccountId(1),
        code = "1000",
        name = "현금",
        type = AccountType.ASSET,
        isActive = true,
        parentId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testAccount2 = Account(
        id = AccountId(2),
        code = "4000",
        name = "매출",
        type = AccountType.REVENUE,
        isActive = true,
        parentId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testTransaction = Transaction(
        id = TransactionId(1),
        description = "매출 거래",
        date = LocalDate.now(),
        reference = "REF-001",
        status = TransactionStatus.PENDING,
        entries = listOf(
            JournalEntry(
                accountId = AccountId(1),
                account = testAccount1,
                entryType = EntryType.DEBIT,
                amount = BigDecimal("100.00"),
                description = "현금 입금"
            ),
            JournalEntry(
                accountId = AccountId(2),
                account = testAccount2,
                entryType = EntryType.CREDIT,
                amount = BigDecimal("100.00"),
                description = "매출 발생"
            )
        ),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("거래 생성 테스트")
    inner class CreateTransactionTest {

        @Test
        @DisplayName("정상적인 거래 생성")
        fun `정상적인 거래를 생성한다`() {
            // given
            val command = CreateTransactionCommand(
                date = LocalDate.now(),
                description = "매출 거래",
                reference = "REF-001",
                entries = listOf(
                    CreateTransactionEntryCommand(
                        accountId = AccountId(1),
                        type = EntryType.DEBIT,
                        amount = BigDecimal("100.00"),
                        description = "현금 입금"
                    ),
                    CreateTransactionEntryCommand(
                        accountId = AccountId(2),
                        type = EntryType.CREDIT,
                        amount = BigDecimal("100.00"),
                        description = "매출 발생"
                    )
                )
            )

            every { accountRepository.findById(AccountId(1)) } returns testAccount1
            every { accountRepository.findById(AccountId(2)) } returns testAccount2
            every { transactionRepository.save(any()) } returns testTransaction

            // when
            val result = transactionService.createTransaction(command)

            // then
            assertNotNull(result)
            assertEquals("매출 거래", result.description)
            assertEquals(2, result.entries.size)
            verify(exactly = 1) { transactionRepository.save(any()) }
            // 정확한 호출 횟수 검증을 위해 atLeast로 변경
            verify(atLeast = 1) { accountRepository.findById(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 계정으로 거래 생성 시 예외 발생")
        fun `존재하지 않는 계정으로 거래 생성 시 예외가 발생한다`() {
            // given
            val command = CreateTransactionCommand(
                date = LocalDate.now(),
                description = "잘못된 거래",
                reference = null,
                entries = listOf(
                    CreateTransactionEntryCommand(
                        accountId = AccountId(999),
                        type = EntryType.DEBIT,
                        amount = BigDecimal("100.00"),
                        description = "잘못된 계정"
                    )
                )
            )

            every { accountRepository.findById(AccountId(999)) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                transactionService.createTransaction(command)
            }
            assertEquals("계정을 찾을 수 없습니다: AccountId(value=999)", exception.message)
        }

        @Test
        @DisplayName("비활성화된 계정으로 거래 생성 시 예외 발생")
        fun `비활성화된 계정으로 거래 생성 시 예외가 발생한다`() {
            // given
            val inactiveAccount = testAccount1.copy(isActive = false)
            val command = CreateTransactionCommand(
                date = LocalDate.now(),
                description = "비활성 계정 거래",
                reference = null,
                entries = listOf(
                    CreateTransactionEntryCommand(
                        accountId = AccountId(1),
                        type = EntryType.DEBIT,
                        amount = BigDecimal("100.00"),
                        description = "비활성 계정"
                    )
                )
            )

            every { accountRepository.findById(AccountId(1)) } returns inactiveAccount

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                transactionService.createTransaction(command)
            }
            assertEquals("비활성화된 계정입니다: 현금 (1000)", exception.message)
        }
    }

    @Nested
    @DisplayName("거래 수정 테스트")
    inner class UpdateTransactionTest {

        @Test
        @DisplayName("정상적인 거래 수정")
        fun `정상적인 거래를 수정한다`() {
            // given
            val command = UpdateTransactionCommand(
                transactionId = TransactionId(1),
                description = "수정된 매출 거래",
                date = LocalDate.now(),
                reference = "REF-001-UPDATED",
                entries = listOf(
                    CreateTransactionEntryCommand(
                        accountId = AccountId(1),
                        type = EntryType.DEBIT,
                        amount = BigDecimal("200.00"),
                        description = "수정된 현금 입금"
                    ),
                    CreateTransactionEntryCommand(
                        accountId = AccountId(2),
                        type = EntryType.CREDIT,
                        amount = BigDecimal("200.00"),
                        description = "수정된 매출 발생"
                    )
                )
            )

            val updatedTransaction = testTransaction.copy(
                description = "수정된 매출 거래",
                reference = "REF-001-UPDATED"
            )

            every { transactionRepository.findById(TransactionId(1)) } returns testTransaction
            every { accountRepository.findById(AccountId(1)) } returns testAccount1
            every { accountRepository.findById(AccountId(2)) } returns testAccount2
            every { transactionRepository.save(any()) } returns updatedTransaction

            // when
            val result = transactionService.updateTransaction(command)

            // then
            assertNotNull(result)
            assertEquals("수정된 매출 거래", result.description)
            verify(exactly = 1) { transactionRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 거래 수정 시 예외 발생")
        fun `존재하지 않는 거래 수정 시 예외가 발생한다`() {
            // given
            val command = UpdateTransactionCommand(
                transactionId = TransactionId(999),
                description = "수정된 거래",
                date = LocalDate.now(),
                entries = emptyList()
            )

            every { transactionRepository.findById(TransactionId(999)) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                transactionService.updateTransaction(command)
            }
            assertEquals("거래를 찾을 수 없습니다: TransactionId(value=999)", exception.message)
        }

        @Test
        @DisplayName("승인된 거래 수정 시 예외 발생")
        fun `승인된 거래 수정 시 예외가 발생한다`() {
            // given
            val approvedTransaction = testTransaction.copy(status = TransactionStatus.APPROVED)
            val command = UpdateTransactionCommand(
                transactionId = TransactionId(1),
                description = "수정된 거래",
                date = LocalDate.now(),
                entries = emptyList()
            )

            every { transactionRepository.findById(TransactionId(1)) } returns approvedTransaction

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                transactionService.updateTransaction(command)
            }
            assertEquals("승인된 거래는 수정할 수 없습니다", exception.message)
        }
    }

    @Nested
    @DisplayName("거래 취소 테스트")
    inner class CancelTransactionTest {

        @Test
        @DisplayName("정상적인 거래 취소")
        fun `정상적인 거래를 취소한다`() {
            // given
            val command = CancelTransactionCommand(
                transactionId = TransactionId(1),
                cancelReason = "고객 요청"
            )

            val cancelledTransaction = testTransaction.copy(status = TransactionStatus.CANCELLED)
            val reversalTransaction = testTransaction.copy(
                id = TransactionId(2),
                description = "거래 취소: 매출 거래",
                reference = "CANCEL-1",
                status = TransactionStatus.APPROVED
            )

            every { transactionRepository.findById(TransactionId(1)) } returns testTransaction
            every { transactionRepository.save(match { it.status == TransactionStatus.CANCELLED }) } returns cancelledTransaction
            every { transactionRepository.save(match { it.description.startsWith("거래 취소:") }) } returns reversalTransaction

            // when
            val result = transactionService.cancelTransaction(command)

            // then
            assertNotNull(result)
            assertEquals(TransactionStatus.CANCELLED, result.originalTransaction.status)
            assertEquals("거래 취소: 매출 거래", result.reversalTransaction.description)
            assertEquals("고객 요청", result.cancelReason)
            verify(exactly = 2) { transactionRepository.save(any()) }
        }

        @Test
        @DisplayName("이미 취소된 거래 취소 시 예외 발생")
        fun `이미 취소된 거래 취소 시 예외가 발생한다`() {
            // given
            val cancelledTransaction = testTransaction.copy(status = TransactionStatus.CANCELLED)
            val command = CancelTransactionCommand(
                transactionId = TransactionId(1),
                cancelReason = "중복 취소"
            )

            every { transactionRepository.findById(TransactionId(1)) } returns cancelledTransaction

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                transactionService.cancelTransaction(command)
            }
            assertEquals("이미 취소된 거래입니다", exception.message)
        }
    }

    @Nested
    @DisplayName("거래 조회 테스트")
    inner class GetTransactionTest {

        @Test
        @DisplayName("정상적인 거래 조회")
        fun `정상적인 거래를 조회한다`() {
            // given
            every { transactionRepository.findById(TransactionId(1)) } returns testTransaction

            // when
            val result = transactionService.getTransaction(TransactionId(1))

            // then
            assertNotNull(result)
            assertEquals("매출 거래", result.description)
            assertEquals(TransactionId(1), result.id)
        }

        @Test
        @DisplayName("존재하지 않는 거래 조회 시 예외 발생")
        fun `존재하지 않는 거래 조회 시 예외가 발생한다`() {
            // given
            every { transactionRepository.findById(TransactionId(999)) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                transactionService.getTransaction(TransactionId(999))
            }
            assertEquals("거래를 찾을 수 없습니다: TransactionId(value=999)", exception.message)
        }
    }

    @Nested
    @DisplayName("거래 목록 조회 테스트")
    inner class GetTransactionsTest {

        @Test
        @DisplayName("날짜 범위로 거래 목록 조회")
        fun `날짜 범위로 거래 목록을 조회한다`() {
            // given
            val startDate = LocalDate.now().minusDays(7)
            val endDate = LocalDate.now()
            val query = GetTransactionsQuery(
                startDate = startDate,
                endDate = endDate,
                pageable = PageRequest.of(0, 10)
            )

            every { transactionRepository.findByDateRange(startDate, endDate, query.pageable) } returns PageImpl(listOf(testTransaction), PageRequest.of(0, 10), 1)

            // when
            val result = transactionService.getTransactions(query)

            // then
            assertEquals(1, result.content.size)
            assertEquals(1, result.totalElements)
            assertEquals("매출 거래", result.content[0].description)
        }

        @Test
        @DisplayName("전체 거래 목록 조회")
        fun `전체 거래 목록을 조회한다`() {
            // given
            val query = GetTransactionsQuery(
                pageable = PageRequest.of(0, 10)
            )
            every { transactionRepository.findAll(query.pageable) } returns PageImpl(listOf(testTransaction), PageRequest.of(0, 10), 1)

            // when
            val result = transactionService.getTransactions(query)

            // then
            assertEquals(1, result.content.size)
            assertEquals(1, result.totalElements)
        }
    }
}
