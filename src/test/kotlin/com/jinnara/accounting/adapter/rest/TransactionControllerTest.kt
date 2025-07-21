package com.jinnara.accounting.adapter.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.jinnara.accounting.application.port.input.TransactionUseCase
import com.jinnara.accounting.domain.transaction.Transaction
import com.jinnara.accounting.domain.transaction.TransactionId
import com.jinnara.accounting.domain.transaction.TransactionStatus
import com.jinnara.accounting.domain.transaction.JournalEntry
import com.jinnara.accounting.domain.transaction.EntryType
import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.adapter.rest.dto.*
import com.jinnara.accounting.application.port.command.CancelTransactionResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(TransactionController::class)
@DisplayName("TransactionController 테스트")
class TransactionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var transactionUseCase: TransactionUseCase

    private fun createTestAccount(
        id: Long = 1L,
        code: String = "1000",
        name: String = "현금",
        type: AccountType = AccountType.ASSET
    ): Account {
        return Account(
            id = AccountId(id),
            code = code,
            name = name,
            type = type,
            parentId = null,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createTestTransaction(
        id: Long = 1L,
        description: String = "현금 입금",
        transactionDate: LocalDate = LocalDate.now(),
        status: TransactionStatus = TransactionStatus.PENDING
    ): Transaction {
        val cashAccount = createTestAccount(1L, "1000", "현금", AccountType.ASSET)
        val revenueAccount = createTestAccount(2L, "4000", "매출", AccountType.REVENUE)

        val journalEntries = listOf(
            JournalEntry(
                accountId = AccountId(1L),
                account = cashAccount,
                entryType = EntryType.DEBIT,
                amount = BigDecimal("100000"),
                description = "현금 입금"
            ),
            JournalEntry(
                accountId = AccountId(2L),
                account = revenueAccount,
                entryType = EntryType.CREDIT,
                amount = BigDecimal("100000"),
                description = "매출 발생"
            )
        )

        return Transaction(
            id = TransactionId(id),
            description = description,
            date = transactionDate,
            status = status,
            entries = journalEntries,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("거래 생성 API")
    inner class CreateTransactionTest {

        @Test
        @DisplayName("거래 생성이 성공하면 201 Created를 반환한다")
        fun `거래 생성이 성공하면 201 Created를 반환한다`() {
            // given
            val request = CreateTransactionRequest(
                description = "현금 입금",
                transactionDate = LocalDate.of(2024, 1, 15),
                journalEntries = listOf(
                    JournalEntryRequest(
                        accountId = 1L,
                        entryType = "DEBIT",
                        amount = BigDecimal("100000"),
                        description = "현금 입금"
                    ),
                    JournalEntryRequest(
                        accountId = 2L,
                        entryType = "CREDIT",
                        amount = BigDecimal("100000"),
                        description = "매출 발생"
                    )
                )
            )
            val createdTransaction = createTestTransaction()

            every { transactionUseCase.createTransaction(any()) } returns createdTransaction

            // when & then
            mockMvc.perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("현금 입금"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(100000))
                .andExpect(jsonPath("$.journalEntries").isArray)
                .andExpect(jsonPath("$.journalEntries.length()").value(2))

            verify { transactionUseCase.createTransaction(any()) }
        }

        @Test
        @DisplayName("필수 필드가 누락되면 400 Bad Request를 반환한다")
        fun `필수 필드가 누락되면 400 Bad Request를 반환한다`() {
            // given
            val request = CreateTransactionRequest(
                description = "",
                transactionDate = LocalDate.of(2024, 1, 15),
                journalEntries = emptyList()
            )

            // when & then
            mockMvc.perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("차변과 대변이 균형을 이루지 않으면 400 Bad Request를 반환한다")
        fun `차변과 대변이 균형을 이루지 않으면 400 Bad Request를 반환한다`() {
            // given
            val request = CreateTransactionRequest(
                description = "불균형 거래",
                transactionDate = LocalDate.of(2024, 1, 15),
                journalEntries = listOf(
                    JournalEntryRequest(
                        accountId = 1L,
                        entryType = "DEBIT",
                        amount = BigDecimal("100000"),
                        description = "차변"
                    ),
                    JournalEntryRequest(
                        accountId = 2L,
                        entryType = "CREDIT",
                        amount = BigDecimal("50000"),
                        description = "대변"
                    )
                )
            )

            every { transactionUseCase.createTransaction(any()) } throws IllegalArgumentException("차변과 대변의 합이 일치하지 않습니다")

            // when & then
            mockMvc.perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("거래 수정 API")
    inner class UpdateTransactionTest {

        @Test
        @DisplayName("거래 수정이 성공하면 200 OK를 반환한다")
        fun `거래 수정이 성공하면 200 OK를 반환한다`() {
            // given
            val request = UpdateTransactionRequest(
                description = "수정된 거래",
                transactionDate = LocalDate.of(2024, 1, 16),
                journalEntries = listOf(
                    JournalEntryRequest(
                        accountId = 1L,
                        entryType = "DEBIT",
                        amount = BigDecimal("200000"),
                        description = "수정된 차변"
                    ),
                    JournalEntryRequest(
                        accountId = 2L,
                        entryType = "CREDIT",
                        amount = BigDecimal("200000"),
                        description = "수정된 대변"
                    )
                )
            )
            val updatedTransaction = createTestTransaction(
                description = "수정된 거래",
                transactionDate = LocalDate.of(2024, 1, 16)
            )

            every { transactionUseCase.updateTransaction(any()) } returns updatedTransaction

            // when & then
            mockMvc.perform(
                put("/api/v1/transactions/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("수정된 거래"))

            verify { transactionUseCase.updateTransaction(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 거래를 수정하려고 하면 404 Not Found를 반환한다")
        fun `존재하지 않는 거래를 수정하려고 하면 404 Not Found를 반환한다`() {
            // given
            val request = UpdateTransactionRequest(
                description = "수정된 거래",
                transactionDate = LocalDate.of(2024, 1, 16),
                journalEntries = listOf(
                    JournalEntryRequest(
                        accountId = 1L,
                        entryType = "DEBIT",
                        amount = BigDecimal("200000"),
                        description = "수정된 차변"
                    ),
                    JournalEntryRequest(
                        accountId = 2L,
                        entryType = "CREDIT",
                        amount = BigDecimal("200000"),
                        description = "수정된 대변"
                    )
                )
            )

            every { transactionUseCase.updateTransaction(any()) } throws NoSuchElementException("거래를 찾을 수 없습니다")

            // when & then
            mockMvc.perform(
                put("/api/v1/transactions/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("거래 취소 API")
    inner class CancelTransactionTest {

        @Test
        @DisplayName("거래 취소가 성공하면 200 OK를 반환한다")
        fun `거래 취소가 성공하면 200 OK를 반환한다`() {
            // given
            val request = CancelTransactionRequest(
                cancelReason = "고객 요청으로 인한 취소"
            )
            val originalTransaction = createTestTransaction(status = TransactionStatus.CANCELLED)
            val reversalTransaction = createTestTransaction(id = 2L, description = "취소 분개")
            val cancelResult = CancelTransactionResult(
                originalTransaction = originalTransaction,
                reversalTransaction = reversalTransaction,
                cancelReason = "고객 요청으로 인한 취소"
            )

            every { transactionUseCase.cancelTransaction(any()) } returns cancelResult

            // when & then
            mockMvc.perform(
                post("/api/v1/transactions/1/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.originalTransaction.id").value(1))
                .andExpect(jsonPath("$.originalTransaction.status").value("CANCELLED"))
                .andExpect(jsonPath("$.reversalTransaction.id").value(2))
                .andExpect(jsonPath("$.cancelReason").value("고객 요청으로 인한 취소"))

            verify { transactionUseCase.cancelTransaction(any()) }
        }

        @Test
        @DisplayName("이미 취소된 거래를 취소하려고 하면 400 Bad Request를 반환한다")
        fun `이미 취소된 거래를 취소하려고 하면 400 Bad Request를 반환한다`() {
            // given
            val request = CancelTransactionRequest(
                cancelReason = "중복 취소 시도"
            )

            every { transactionUseCase.cancelTransaction(any()) } throws IllegalStateException("이미 취소된 거래입니다")

            // when & then
            mockMvc.perform(
                post("/api/v1/transactions/1/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("거래 조회 API")
    inner class GetTransactionTest {

        @Test
        @DisplayName("거래 조회가 성공하면 200 OK를 반환한다")
        fun `거래 조회가 성공하면 200 OK를 반환한다`() {
            // given
            val transaction = createTestTransaction()

            every { transactionUseCase.getTransaction(TransactionId(1L)) } returns transaction

            // when & then
            mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("현금 입금"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.journalEntries").isArray)
                .andExpect(jsonPath("$.journalEntries.length()").value(2))

            verify { transactionUseCase.getTransaction(TransactionId(1L)) }
        }

        @Test
        @DisplayName("존재하지 않는 거래를 조회하면 404 Not Found를 반환한다")
        fun `존재하지 않는 거래를 조회하면 404 Not Found를 반환한다`() {
            // given
            every { transactionUseCase.getTransaction(TransactionId(999L)) } throws NoSuchElementException("거래를 찾을 수 없습니다")

            // when & then
            mockMvc.perform(get("/api/v1/transactions/999"))
                .andExpect(status().isNotFound)

            verify { transactionUseCase.getTransaction(TransactionId(999L)) }
        }
    }

    @Nested
    @DisplayName("거래 목록 조회 API")
    inner class GetTransactionsTest {

        @Test
        @DisplayName("거래 목록 조회가 성공하면 200 OK를 반환한다")
        fun `거래 목록 조회가 성공하면 200 OK를 반환한다`() {
            // given
            val transactions = listOf(
                createTestTransaction(1L, "거래 1"),
                createTestTransaction(2L, "거래 2")
            )
            val page = PageImpl(transactions, PageRequest.of(0, 20), 2L)

            every { transactionUseCase.getTransactions(any()) } returns page

            // when & then
            mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))

            verify { transactionUseCase.getTransactions(any()) }
        }

        @Test
        @DisplayName("날짜 범위로 거래 목록 조회가 성공하면 200 OK를 반환한다")
        fun `날짜 범위로 거래 목록 조회가 성공하면 200 OK를 반환한다`() {
            // given
            val transactions = listOf(createTestTransaction())
            val page = PageImpl(transactions, PageRequest.of(0, 20), 1L)

            every { transactionUseCase.getTransactions(any()) } returns page

            // when & then
            mockMvc.perform(
                get("/api/v1/transactions")
                    .param("startDate", "2024-01-01")
                    .param("endDate", "2024-01-31")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.size").value(20))

            verify { transactionUseCase.getTransactions(any()) }
        }
    }
}
