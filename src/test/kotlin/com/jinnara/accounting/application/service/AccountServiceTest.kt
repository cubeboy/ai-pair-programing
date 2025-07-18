package com.jinnara.accounting.application.service

import com.jinnara.accounting.application.port.command.CreateAccountCommand
import com.jinnara.accounting.application.port.command.UpdateAccountCommand
import com.jinnara.accounting.application.port.output.AccountRepository
import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("AccountService 단위 테스트")
class AccountServiceTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var accountService: AccountService

    @BeforeEach
    fun setUp() {
        accountRepository = mockk()
        accountService = AccountService(accountRepository)
    }

    @Nested
    @DisplayName("계정 생성 테스트")
    inner class CreateAccountTest {

        @Test
        @DisplayName("정상적인 계정 생성 - 부모 계정 없음")
        fun `should create account successfully without parent`() {
            // Given
            val command = CreateAccountCommand(
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )
            val expectedAccount = Account(
                id = AccountId(1L),
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )

            every { accountRepository.existsByCode("1100") } returns false
            every { accountRepository.save(any()) } returns expectedAccount

            // When
            val result = accountService.createAccount(command)

            // Then
            assertEquals(expectedAccount, result)
            verify { accountRepository.existsByCode("1100") }
            verify { accountRepository.save(any()) }
        }

        @Test
        @DisplayName("정상적인 계정 생성 - 부모 계정 있음")
        fun `should create account successfully with parent`() {
            // Given
            val parentId = AccountId(1L)
            val parentAccount = Account(
                id = parentId,
                code = "1000",
                name = "자산",
                type = AccountType.ASSET,
                parentId = null,
                isActive = true
            )
            val command = CreateAccountCommand(
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = parentId
            )
            val expectedAccount = Account(
                id = AccountId(2L),
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = parentId
            )

            every { accountRepository.existsByCode("1100") } returns false
            every { accountRepository.findById(parentId) } returns parentAccount
            every { accountRepository.save(any()) } returns expectedAccount

            // When
            val result = accountService.createAccount(command)

            // Then
            assertEquals(expectedAccount, result)
            verify { accountRepository.existsByCode("1100") }
            verify { accountRepository.findById(parentId) }
            verify { accountRepository.save(any()) }
        }

        @Test
        @DisplayName("중복된 계정 코드로 생성 실패")
        fun `should fail when account code already exists`() {
            // Given
            val command = CreateAccountCommand(
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )

            every { accountRepository.existsByCode("1100") } returns true

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.createAccount(command)
            }
            assertEquals("계정코드 '1100'가 이미 존재합니다.", exception.message)
            verify { accountRepository.existsByCode("1100") }
        }

        @Test
        @DisplayName("존재하지 않는 부모 계정으로 생성 실패")
        fun `should fail when parent account does not exist`() {
            // Given
            val parentId = AccountId(1L)
            val command = CreateAccountCommand(
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = parentId
            )

            every { accountRepository.existsByCode("1100") } returns false
            every { accountRepository.findById(parentId) } returns null

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.createAccount(command)
            }
            assertEquals("상위 계정을 찾을 수 없습니다: $parentId", exception.message)
        }

        @Test
        @DisplayName("비활성화된 부모 계정으로 생성 실패")
        fun `should fail when parent account is inactive`() {
            // Given
            val parentId = AccountId(1L)
            val inactiveParent = Account(
                id = parentId,
                code = "1000",
                name = "자산",
                type = AccountType.ASSET,
                parentId = null,
                isActive = false
            )
            val command = CreateAccountCommand(
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = parentId
            )

            every { accountRepository.existsByCode("1100") } returns false
            every { accountRepository.findById(parentId) } returns inactiveParent

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.createAccount(command)
            }
            assertEquals("비활성화된 상위 계정입니다.", exception.message)
        }
    }

    @Nested
    @DisplayName("계정 수정 테스트")
    inner class UpdateAccountTest {

        @Test
        @DisplayName("정상적인 계정 수정")
        fun `should update account successfully`() {
            // Given
            val accountId = AccountId(1L)
            val parentId = AccountId(2L)
            val existingAccount = Account(
                id = accountId,
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )
            val parentAccount = Account(
                id = parentId,
                code = "1000",
                name = "자산",
                type = AccountType.ASSET,
                parentId = null,
                isActive = true
            )
            val command = UpdateAccountCommand(
                accountId = accountId,
                name = "현금및현금성자산",
                parentId = parentId
            )
            val updatedAccount = existingAccount.copy(
                name = "현금및현금성자산",
                parentId = parentId
            )

            every { accountRepository.findById(accountId) } returns existingAccount
            every { accountRepository.findById(parentId) } returns parentAccount
            every { accountRepository.save(any()) } returns updatedAccount

            // When
            val result = accountService.updateAccount(command)

            // Then
            assertEquals(updatedAccount, result)
            verify { accountRepository.findById(accountId) }
            verify { accountRepository.findById(parentId) }
            verify { accountRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 계정 수정 실패")
        fun `should fail when account does not exist`() {
            // Given
            val accountId = AccountId(1L)
            val command = UpdateAccountCommand(
                accountId = accountId,
                name = "현금및현금성자산",
                parentId = null
            )

            every { accountRepository.findById(accountId) } returns null

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.updateAccount(command)
            }
            assertEquals("계정을 찾을 수 없습니다: $accountId", exception.message)
        }

        @Test
        @DisplayName("자기 자신을 부모로 설정 실패")
        fun `should fail when setting self as parent`() {
            // Given
            val accountId = AccountId(1L)
            val existingAccount = Account(
                id = accountId,
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )
            val command = UpdateAccountCommand(
                accountId = accountId,
                name = "현금및현금성자산",
                parentId = accountId
            )

            every { accountRepository.findById(accountId) } returns existingAccount andThen existingAccount

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.updateAccount(command)
            }
            assertEquals("자기 자신을 상위 계정으로 설정할 수 없습니다.", exception.message)
        }
    }

    @Nested
    @DisplayName("계정 비활성화 테스트")
    inner class DeactivateAccountTest {

        @Test
        @DisplayName("정상적인 계정 비활성화")
        fun `should deactivate account successfully`() {
            // Given
            val accountId = AccountId(1L)
            val activeAccount = Account(
                id = accountId,
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null,
                isActive = true
            )
            val deactivatedAccount = activeAccount.deactivate()

            every { accountRepository.findById(accountId) } returns activeAccount
            every { accountRepository.findByParentId(accountId) } returns emptyList()
            every { accountRepository.save(any()) } returns deactivatedAccount

            // When
            val result = accountService.deactivateAccount(accountId)

            // Then
            assertFalse(result.isActive)
            verify { accountRepository.findById(accountId) }
            verify { accountRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 계정 비활성화 실패")
        fun `should fail when account does not exist`() {
            // Given
            val accountId = AccountId(1L)

            every { accountRepository.findById(accountId) } returns null

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.deactivateAccount(accountId)
            }
            assertEquals("계정을 찾을 수 없습니다: $accountId", exception.message)
        }

        @Test
        @DisplayName("하위 계정이 있는 계정 비활성화 실패")
        fun `should fail when account has child accounts`() {
            // Given
            val parentAccountId = AccountId(1L)
            val parentAccount = Account(
                id = parentAccountId,
                code = "1000",
                name = "자산",
                type = AccountType.ASSET,
                parentId = null,
                isActive = true
            )
            val childAccount = Account(
                id = AccountId(2L),
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = parentAccountId,
                isActive = true
            )
            val childAccounts = listOf(childAccount)

            every { accountRepository.findById(parentAccountId) } returns parentAccount
            every { accountRepository.findByParentId(parentAccountId) } returns childAccounts

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.deactivateAccount(parentAccountId)
            }
            assertEquals("하위 계정이 존재하는 계정은 비활성화할 수 없습니다.", exception.message)
            verify { accountRepository.findById(parentAccountId) }
            verify { accountRepository.findByParentId(parentAccountId) }
        }
    }

    @Nested
    @DisplayName("계정 조회 테스트")
    inner class GetAccountTest {

        @Test
        @DisplayName("정상적인 계정 조회")
        fun `should get account successfully`() {
            // Given
            val accountId = AccountId(1L)
            val account = Account(
                id = accountId,
                code = "1100",
                name = "현금",
                type = AccountType.ASSET,
                parentId = null
            )

            every { accountRepository.findById(accountId) } returns account

            // When
            val result = accountService.getAccount(accountId)

            // Then
            assertEquals(account, result)
            verify { accountRepository.findById(accountId) }
        }

        @Test
        @DisplayName("존재하지 않는 계정 조회 실패")
        fun `should fail when account does not exist`() {
            // Given
            val accountId = AccountId(1L)

            every { accountRepository.findById(accountId) } returns null

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                accountService.getAccount(accountId)
            }
            assertEquals("계정을 찾을 수 없습니다: $accountId", exception.message)
        }
    }

    @Nested
    @DisplayName("계정 목록 조회 테스트")
    inner class GetAccountsTest {

        @Test
        @DisplayName("유형별 계정 목록 조회")
        fun `should get accounts by type successfully`() {
            // Given
            val accountType = AccountType.ASSET
            val accounts = listOf(
                Account(
                    id = AccountId(1L),
                    code = "1100",
                    name = "현금",
                    type = AccountType.ASSET,
                    parentId = null
                ),
                Account(
                    id = AccountId(2L),
                    code = "1200",
                    name = "예금",
                    type = AccountType.ASSET,
                    parentId = null
                )
            )

            every { accountRepository.findByType(accountType) } returns accounts

            // When
            val result = accountService.getAccountsByType(accountType)

            // Then
            assertEquals(accounts, result)
            assertEquals(2, result.size)
            verify { accountRepository.findByType(accountType) }
        }

        @Test
        @DisplayName("활성 계정 목록 조회")
        fun `should get all active accounts successfully`() {
            // Given
            val activeAccounts = listOf(
                Account(
                    id = AccountId(1L),
                    code = "1100",
                    name = "현금",
                    type = AccountType.ASSET,
                    parentId = null,
                    isActive = true
                ),
                Account(
                    id = AccountId(2L),
                    code = "2100",
                    name = "매입채무",
                    type = AccountType.LIABILITY,
                    parentId = null,
                    isActive = true
                )
            )

            every { accountRepository.findAllActive() } returns activeAccounts

            // When
            val result = accountService.getAllActiveAccounts()

            // Then
            assertEquals(activeAccounts, result)
            assertEquals(2, result.size)
            assertTrue(result.all { it.isActive })
            verify { accountRepository.findAllActive() }
        }

        @Test
        @DisplayName("빈 목록 반환")
        fun `should return empty list when no accounts found`() {
            // Given
            val accountType = AccountType.REVENUE

            every { accountRepository.findByType(accountType) } returns emptyList()

            // When
            val result = accountService.getAccountsByType(accountType)

            // Then
            assertTrue(result.isEmpty())
            verify { accountRepository.findByType(accountType) }
        }
    }
}
