package com.jinnara.accounting.`interface`.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.jinnara.accounting.application.port.input.AccountUseCase
import com.jinnara.accounting.application.port.input.CreateAccountCommand
import com.jinnara.accounting.application.port.input.UpdateAccountCommand
import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.`interface`.rest.dto.CreateAccountRequest
import com.jinnara.accounting.`interface`.rest.dto.UpdateAccountRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(AccountController::class)
@ContextConfiguration(classes = [AccountController::class, GlobalExceptionHandler::class, AccountControllerTest.TestConfig::class])
@DisplayName("AccountController 테스트")
class AccountControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var accountUseCase: AccountUseCase

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockAccountUseCase(): AccountUseCase = mockk()
    }

    private fun createTestAccount(
        id: Long = 1L,
        code: String = "1000",
        name: String = "현금",
        type: AccountType = AccountType.ASSET,
        parentId: Long? = null,
        isActive: Boolean = true
    ): Account {
        return Account(
            id = AccountId(id),
            code = code,
            name = name,
            type = type,
            parentId = parentId?.let { AccountId(it) },
            isActive = isActive,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("계정 생성 API")
    inner class CreateAccountTest {

        @Test
        @DisplayName("계정 생성이 성공하면 201 Created를 반환한다")
        fun `계정 생성이 성공하면 201 Created를 반환한다`() {
            // given
            val request = CreateAccountRequest(
                code = "1000",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )
            val createdAccount = createTestAccount()

            every { accountUseCase.createAccount(any()) } returns createdAccount

            // when & then
            mockMvc.perform(
                post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.name").value("현금"))
                .andExpect(jsonPath("$.type").value("ASSET"))
                .andExpect(jsonPath("$.parentId").isEmpty)
                .andExpect(jsonPath("$.isActive").value(true))

            verify {
                accountUseCase.createAccount(
                    CreateAccountCommand(
                        code = "1000",
                        name = "현금",
                        type = AccountType.ASSET,
                        parentId = null
                    )
                )
            }
        }

        @Test
        @DisplayName("상위 계정이 있는 계정 생성이 성공하면 201 Created를 반환한다")
        fun `상위 계정이 있는 계정 생성이 성공하면 201 Created를 반환한다`() {
            // given
            val request = CreateAccountRequest(
                code = "1100",
                name = "은행예금",
                type = AccountType.ASSET,
                parentId = 1L
            )
            val createdAccount = createTestAccount(
                id = 2L,
                code = "1100",
                name = "은행예금",
                parentId = 1L
            )

            every { accountUseCase.createAccount(any()) } returns createdAccount

            // when & then
            mockMvc.perform(
                post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("1100"))
                .andExpect(jsonPath("$.name").value("은행예금"))
                .andExpect(jsonPath("$.type").value("ASSET"))
                .andExpect(jsonPath("$.parentId").value(1))
                .andExpect(jsonPath("$.isActive").value(true))

            verify {
                accountUseCase.createAccount(
                    CreateAccountCommand(
                        code = "1100",
                        name = "은행예금",
                        type = AccountType.ASSET,
                        parentId = AccountId(1L)
                    )
                )
            }
        }

        @Test
        @DisplayName("잘못된 JSON 형식으로 요청하면 400 Bad Request를 반환한다")
        fun `잘못된 JSON 형식으로 요청하면 400 Bad Request를 반환한다`() {
            // given
            val invalidJson = "{ invalid json }"

            // when & then
            mockMvc.perform(
                post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("필수 필드가 누락되면 400 Bad Request를 반환한다")
        fun `필수 필드가 누락되면 400 Bad Request를 반환한다`() {
            // given
            val incompleteRequest = mapOf(
                "code" to "1000",
                "name" to "현금"
                // type 누락
            )

            // when & then
            mockMvc.perform(
                post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(incompleteRequest))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("UseCase에서 예외가 발생하면 적절한 에러 응답을 반환한다")
        fun `UseCase에서 예외가 발생하면 적절한 에러 응답을 반환한다`() {
            // given
            val request = CreateAccountRequest(
                code = "1000",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )

            every { accountUseCase.createAccount(any()) } throws RuntimeException("계정 생성 실패")

            // when & then
            mockMvc.perform(
                post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("계정 수정 API")
    inner class UpdateAccountTest {

        @Test
        @DisplayName("계정 수정이 성공하면 200 OK를 반환한다")
        fun `계정 수정이 성공하면 200 OK를 반환한다`() {
            // given
            val accountId = 1L
            val request = UpdateAccountRequest(
                name = "수정된 현금",
                parentId = null
            )
            val updatedAccount = createTestAccount(name = "수정된 현금")

            every { accountUseCase.updateAccount(any()) } returns updatedAccount

            // when & then
            mockMvc.perform(
                put("/api/v1/accounts/{id}", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("수정된 현금"))

            verify {
                accountUseCase.updateAccount(
                    UpdateAccountCommand(
                        accountId = AccountId(accountId),
                        name = "수정된 현금",
                        parentId = null
                    )
                )
            }
        }

        @Test
        @DisplayName("존재하지 않는 계정 ID로 수정 요청하면 에러를 반환한다")
        fun `존재하지 않는 계정 ID로 수정 요청하면 에러를 반환한다`() {
            // given
            val nonExistentAccountId = 999L
            val request = UpdateAccountRequest(
                name = "수정된 계정명",
                parentId = null
            )

            every { accountUseCase.updateAccount(any()) } throws NoSuchElementException("계정을 찾을 수 없습니다")

            // when & then
            mockMvc.perform(
                put("/api/v1/accounts/{id}", nonExistentAccountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("잘못된 경로 변수로 요청하면 400 Bad Request를 반환한다")
        fun `잘못된 경로 변수로 요청하면 400 Bad Request를 반환한다`() {
            // given
            val invalidId = "invalid"
            val request = UpdateAccountRequest(
                name = "수정된 계정명",
                parentId = null
            )

            // when & then
            mockMvc.perform(
                put("/api/v1/accounts/{id}", invalidId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("계정 비활성화 API")
    inner class DeactivateAccountTest {

        @Test
        @DisplayName("계정 비활성화가 성공하면 200 OK를 반환한다")
        fun `계정 비활성화가 성공하면 200 OK를 반환한다`() {
            // given
            val accountId = 1L
            val deactivatedAccount = createTestAccount(isActive = false)

            every { accountUseCase.deactivateAccount(AccountId(accountId)) } returns deactivatedAccount

            // when & then
            mockMvc.perform(delete("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isActive").value(false))

            verify { accountUseCase.deactivateAccount(AccountId(accountId)) }
        }

        @Test
        @DisplayName("존재하지 않는 계정 ID로 비활성화 요청하면 404 Not Found를 반환한다")
        fun `존재하지 않는 계정 ID로 비활성화 요청하면 404 Not Found를 반환한다`() {
            // given
            val nonExistentAccountId = 999L

            every { accountUseCase.deactivateAccount(AccountId(nonExistentAccountId)) } throws NoSuchElementException("계정을 찾을 수 없습니다")

            // when & then
            mockMvc.perform(delete("/api/v1/accounts/{id}", nonExistentAccountId))
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("이미 비활성화된 계정을 비활성화하려고 하면 에러를 반환한다")
        fun `이미 비활성화된 계정을 비활성화하려고 하면 에러를 반환한다`() {
            // given
            val accountId = 1L

            every { accountUseCase.deactivateAccount(AccountId(accountId)) } throws IllegalStateException("이미 비활성화된 계정입니다")

            // when & then
            mockMvc.perform(delete("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("계정 조회 API")
    inner class GetAccountTest {

        @Test
        @DisplayName("계정 조회가 성공하면 200 OK를 반환한다")
        fun `계정 조회가 성공하면 200 OK를 반환한다`() {
            // given
            val accountId = 1L
            val account = createTestAccount()

            every { accountUseCase.getAccount(AccountId(accountId)) } returns account

            // when & then
            mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.name").value("현금"))

            verify { accountUseCase.getAccount(AccountId(accountId)) }
        }

        @Test
        @DisplayName("존재하지 않는 계정 ID로 조회하면 404 Not Found를 반환한다")
        fun `존재하지 않는 계정 ID로 조회하면 404 Not Found를 반환한다`() {
            // given
            val nonExistentAccountId = 999L

            every { accountUseCase.getAccount(AccountId(nonExistentAccountId)) } throws NoSuchElementException("계정을 찾을 수 없습니다")

            // when & then
            mockMvc.perform(get("/api/v1/accounts/{id}", nonExistentAccountId))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("계정 목록 조회 API")
    inner class GetAccountsTest {

        @Test
        @DisplayName("모든 활성 계정 조회가 성공하면 200 OK를 반환한다")
        fun `모든 활성 계정 조회가 성공하면 200 OK를 반환한다`() {
            // given
            val accounts = listOf(
                createTestAccount(id = 1L, name = "현금"),
                createTestAccount(id = 2L, name = "은행예금")
            )

            every { accountUseCase.getAllActiveAccounts() } returns accounts

            // when & then
            mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("현금"))
                .andExpect(jsonPath("$[1].name").value("은행예금"))

            verify { accountUseCase.getAllActiveAccounts() }
        }

        @Test
        @DisplayName("특정 타입의 계정 조회가 성공하면 200 OK를 반환한다")
        fun `특정 타입의 계정 조회가 성공하면 200 OK를 반환한다`() {
            // given
            val assetAccounts = listOf(
                createTestAccount(id = 1L, name = "현금", type = AccountType.ASSET),
                createTestAccount(id = 2L, name = "은행예금", type = AccountType.ASSET)
            )

            every { accountUseCase.getAccountsByType(AccountType.ASSET) } returns assetAccounts

            // when & then
            mockMvc.perform(get("/api/v1/accounts").param("type", "ASSET"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("ASSET"))
                .andExpect(jsonPath("$[1].type").value("ASSET"))

            verify { accountUseCase.getAccountsByType(AccountType.ASSET) }
        }

        @Test
        @DisplayName("잘못된 계정 타입으로 조회하면 400 Bad Request를 반환한다")
        fun `잘못된 계정 타입으로 조회하면 400 Bad Request를 반환한다`() {
            // when & then
            mockMvc.perform(get("/api/v1/accounts").param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("계정이 없을 때 빈 배열을 반환한다")
        fun `계정이 없을 때 빈 배열을 반환한다`() {
            // given
            every { accountUseCase.getAllActiveAccounts() } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(0))

            verify { accountUseCase.getAllActiveAccounts() }
        }

        @Test
        @DisplayName("UseCase에서 예외가 발생하면 500 Internal Server Error를 반환한다")
        fun `UseCase에서 예외가 발생하면 500 Internal Server Error를 반환한다`() {
            // given
            every { accountUseCase.getAllActiveAccounts() } throws RuntimeException("데이터베이스 연결 오류")

            // when & then
            mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("공통 예외 처리")
    inner class CommonExceptionHandlingTest {

        @Test
        @DisplayName("Content-Type이 없으면 415 Unsupported Media Type을 반환한다")
        fun `Content-Type이 없으면 415 Unsupported Media Type을 반환한다`() {
            // given
            val request = CreateAccountRequest(
                code = "1000",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )

            // when & then
            mockMvc.perform(
                post("/api/v1/accounts")
                    .content(objectMapper.writeValueAsString(request))
                    // Content-Type 헤더 누락
            )
                .andExpect(status().isUnsupportedMediaType)
        }

        @Test
        @DisplayName("지원하지 않는 HTTP 메소드로 요청하면 405 Method Not Allowed를 반환한다")
        fun `지원하지 않는 HTTP 메소드로 요청하면 405 Method Not Allowed를 반환한다`() {
            // when & then
            mockMvc.perform(patch("/api/v1/accounts/1"))
                .andExpect(status().isMethodNotAllowed)
        }
    }
}
