package com.jinnara.accounting.infrastructure.persistence.account

import com.jinnara.accounting.domain.account.Account
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@Import(AccountRepositoryImpl::class)
@DisplayName("AccountRepositoryImpl ORM 테스트")
class AccountRepositoryImplTest {

    @Autowired
    private lateinit var accountRepository: AccountRepositoryImpl

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testAccount: Account
    private lateinit var parentAccount: Account
    private lateinit var childAccount: Account

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 준비
        val now = LocalDateTime.now()

        testAccount = Account(
            id = AccountId(0L),
            code = "1000",
            name = "현금",
            type = AccountType.ASSET,
            parentId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        parentAccount = Account(
            id = AccountId(0L),
            code = "1100",
            name = "유동자산",
            type = AccountType.ASSET,
            parentId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        childAccount = Account(
            id = AccountId(0L),
            code = "1110",
            name = "당좌자산",
            type = AccountType.ASSET,
            parentId = AccountId(1L), // 나중에 실제 부모 ID로 설정
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }

    @Nested
    @DisplayName("계정 저장 테스트")
    inner class SaveAccountTest {

        @Test
        @DisplayName("새로운 계정을 저장할 수 있다")
        fun `새로운 계정을 저장할 수 있다`() {
            // when
            val savedAccount = accountRepository.save(testAccount)

            // then
            assertThat(savedAccount.id.value).isGreaterThan(0L)
            assertThat(savedAccount.code).isEqualTo(testAccount.code)
            assertThat(savedAccount.name).isEqualTo(testAccount.name)
            assertThat(savedAccount.type).isEqualTo(testAccount.type)
            assertThat(savedAccount.isActive).isTrue

            // 실제 DB에서 확인
            val foundEntity = entityManager.find(AccountJpaEntity::class.java, savedAccount.id.value)
            assertThat(foundEntity).isNotNull
            assertThat(foundEntity.code).isEqualTo(testAccount.code)
        }

        @Test
        @DisplayName("기존 계정을 업데이트할 수 있다")
        fun `기존 계정을 업데이트할 수 있다`() {
            // given
            val savedAccount = accountRepository.save(testAccount)
            val updatedAccount = savedAccount.copy(
                name = "수정된 현금",
                updatedAt = LocalDateTime.now()
            )

            // when
            val result = accountRepository.save(updatedAccount)

            // then
            assertThat(result.id).isEqualTo(savedAccount.id)
            assertThat(result.name).isEqualTo("수정된 현금")
            assertThat(result.code).isEqualTo(savedAccount.code)

            // 실제 DB에서 확인
            val foundEntity = entityManager.find(AccountJpaEntity::class.java, result.id.value)
            assertThat(foundEntity.name).isEqualTo("수정된 현금")
        }

        @Test
        @DisplayName("부모 계정이 있는 계정을 저장할 수 있다")
        fun `부모 계정이 있는 계정을 저장할 수 있다`() {
            // given
            val savedParent = accountRepository.save(parentAccount)
            val childWithParent = childAccount.copy(parentId = savedParent.id)

            // when
            val savedChild = accountRepository.save(childWithParent)

            // then
            assertThat(savedChild.parentId).isEqualTo(savedParent.id)

            // 실제 DB에서 확인
            val foundEntity = entityManager.find(AccountJpaEntity::class.java, savedChild.id.value)
            assertThat(foundEntity.parentId).isEqualTo(savedParent.id.value)
        }
    }

    @Nested
    @DisplayName("계정 조회 테스트")
    inner class FindAccountTest {

        @Test
        @DisplayName("ID로 계정을 조회할 수 있다")
        fun `ID로 계정을 조회할 수 있다`() {
            // given
            val savedAccount = accountRepository.save(testAccount)

            // when
            val foundAccount = accountRepository.findById(savedAccount.id)

            // then
            assertThat(foundAccount).isNotNull
            assertThat(foundAccount?.id).isEqualTo(savedAccount.id)
            assertThat(foundAccount?.code).isEqualTo(savedAccount.code)
            assertThat(foundAccount?.name).isEqualTo(savedAccount.name)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `존재하지 않는 ID로 조회하면 null을 반환한다`() {
            // when
            val foundAccount = accountRepository.findById(AccountId(999L))

            // then
            assertThat(foundAccount).isNull()
        }

        @Test
        @DisplayName("계정 코드로 계정을 조회할 수 있다")
        fun `계정 코드로 계정을 조회할 수 있다`() {
            // given
            val savedAccount = accountRepository.save(testAccount)

            // when
            val foundAccount = accountRepository.findByCode(savedAccount.code)

            // then
            assertThat(foundAccount).isNotNull
            assertThat(foundAccount?.code).isEqualTo(savedAccount.code)
            assertThat(foundAccount?.id).isEqualTo(savedAccount.id)
        }

        @Test
        @DisplayName("존재하지 않는 계정 코드로 조회하면 null을 반환한다")
        fun `존재하지 않는 계정 코드로 조회하면 null을 반환한다`() {
            // when
            val foundAccount = accountRepository.findByCode("9999")

            // then
            assertThat(foundAccount).isNull()
        }

        @Test
        @DisplayName("계정 유형별로 계정을 조회할 수 있다")
        fun `계정 유형별로 계정을 조회할 수 있다`() {
            // given
            val assetAccount = testAccount.copy(type = AccountType.ASSET)
            val liabilityAccount = testAccount.copy(code = "2000", name = "부채", type = AccountType.LIABILITY)

            accountRepository.save(assetAccount)
            accountRepository.save(liabilityAccount)

            // when
            val assetAccounts = accountRepository.findByType(AccountType.ASSET)
            val liabilityAccounts = accountRepository.findByType(AccountType.LIABILITY)

            // then
            assertThat(assetAccounts).hasSize(1)
            assertThat(assetAccounts[0].type).isEqualTo(AccountType.ASSET)

            assertThat(liabilityAccounts).hasSize(1)
            assertThat(liabilityAccounts[0].type).isEqualTo(AccountType.LIABILITY)
        }

        @Test
        @DisplayName("활성 계정만 조회할 수 있다")
        fun `활성 계정만 조회할 수 있다`() {
            // given
            val activeAccount = testAccount.copy(isActive = true)
            val inactiveAccount = testAccount.copy(code = "2000", name = "비활성 계정", isActive = false)

            accountRepository.save(activeAccount)
            accountRepository.save(inactiveAccount)

            // when
            val activeAccounts = accountRepository.findAllActive()

            // then
            assertThat(activeAccounts).hasSize(1)
            assertThat(activeAccounts[0].isActive).isTrue
            assertThat(activeAccounts[0].code).isEqualTo(activeAccount.code)
        }

        @Test
        @DisplayName("부모 ID로 하위 계정들을 조회할 수 있다")
        fun `부모 ID로 하위 계정들을 조회할 수 있다`() {
            // given
            val savedParent = accountRepository.save(parentAccount)
            val child1 = childAccount.copy(code = "1110", parentId = savedParent.id)
            val child2 = childAccount.copy(code = "1120", name = "다른 하위계정", parentId = savedParent.id)
            val otherAccount = testAccount.copy(code = "3000", parentId = null)

            accountRepository.save(child1)
            accountRepository.save(child2)
            accountRepository.save(otherAccount)

            // when
            val childAccounts = accountRepository.findByParentId(savedParent.id)

            // then
            assertThat(childAccounts).hasSize(2)
            assertThat(childAccounts).allMatch { it.parentId == savedParent.id }
        }
    }

    @Nested
    @DisplayName("계정 존재 여부 확인 테스트")
    inner class ExistsAccountTest {

        @Test
        @DisplayName("계정 코드가 존재하는지 확인할 수 있다")
        fun `계정 코드가 존재하는지 확인할 수 있다`() {
            // given
            val savedAccount = accountRepository.save(testAccount)

            // when & then
            assertThat(accountRepository.existsByCode(savedAccount.code)).isTrue
            assertThat(accountRepository.existsByCode("9999")).isFalse
        }
    }

    @Nested
    @DisplayName("계정 삭제 테스트")
    inner class DeleteAccountTest {

        @Test
        @DisplayName("계정을 삭제할 수 있다")
        fun `계정을 삭제할 수 있다`() {
            // given
            val savedAccount = accountRepository.save(testAccount)

            // when
            accountRepository.delete(savedAccount.id)

            // then
            val foundAccount = accountRepository.findById(savedAccount.id)
            assertThat(foundAccount).isNull()

            // 실제 DB에서도 확인
            val foundEntity = entityManager.find(AccountJpaEntity::class.java, savedAccount.id.value)
            assertThat(foundEntity).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 계정 삭제 시 예외가 발생하지 않는다")
        fun `존재하지 않는 계정 삭제 시 예외가 발생하지 않는다`() {
            // when & then
            assertThatCode {
                accountRepository.delete(AccountId(999L))
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("매핑 관련 테스트")
    inner class MappingTest {

        @Test
        @DisplayName("도메인 엔티티에서 JPA 엔티티로 매핑이 정확히 동작한다")
        fun `도메인 엔티티에서 JPA 엔티티로 매핑이 정확히 동작한다`() {
            // given
            val domain = testAccount

            // when
            val jpaEntity = AccountJpaEntity.fromDomain(domain)

            // then
            assertThat(jpaEntity.code).isEqualTo(domain.code)
            assertThat(jpaEntity.name).isEqualTo(domain.name)
            assertThat(jpaEntity.type).isEqualTo(domain.type)
            assertThat(jpaEntity.isActive).isEqualTo(domain.isActive)
            assertThat(jpaEntity.parentId).isEqualTo(domain.parentId?.value)
        }

        @Test
        @DisplayName("JPA 엔티티에서 도메인 엔티티로 매핑이 정확히 동작한다")
        fun `JPA 엔티티에서 도메인 엔티티로 매핑이 정확히 동작한다`() {
            // given
            val savedAccount = accountRepository.save(testAccount)
            val jpaEntity = entityManager.find(AccountJpaEntity::class.java, savedAccount.id.value)

            // when
            val domain = jpaEntity.toDomain()

            // then
            assertThat(domain.id.value).isEqualTo(jpaEntity.id)
            assertThat(domain.code).isEqualTo(jpaEntity.code)
            assertThat(domain.name).isEqualTo(jpaEntity.name)
            assertThat(domain.type).isEqualTo(jpaEntity.type)
            assertThat(domain.isActive).isEqualTo(jpaEntity.isActive)
            assertThat(domain.parentId?.value).isEqualTo(jpaEntity.parentId)
        }

        @Test
        @DisplayName("부모 ID가 null인 경우 매핑이 정확히 동작한다")
        fun `부모 ID가 null인 경우 매핑이 정확히 동작한다`() {
            // given
            val accountWithoutParent = testAccount.copy(parentId = null)

            // when
            val savedAccount = accountRepository.save(accountWithoutParent)

            // then
            assertThat(savedAccount.parentId).isNull()

            val jpaEntity = entityManager.find(AccountJpaEntity::class.java, savedAccount.id.value)
            assertThat(jpaEntity.parentId).isNull()
        }
    }

    @Nested
    @DisplayName("데이터 무결성 테스트")
    inner class DataIntegrityTest {

        @Test
        @DisplayName("중복 계정 코드 저장 시 예외가 발생한다")
        fun `중복 계정 코드 저장 시 예외가 발생한다`() {
            // given
            accountRepository.save(testAccount)
            val duplicateAccount = testAccount.copy(
                id = AccountId(0L),
                name = "다른 이름"
            )

            // when & then
            assertThatThrownBy {
                accountRepository.save(duplicateAccount)
                entityManager.flush() // 강제로 DB 저장하여 제약조건 확인
            }.isInstanceOf(Exception::class.java)
        }

        @Test
        @DisplayName("계정 유형별로 정렬된 순서로 조회된다")
        fun `활성 계정이 코드 순으로 정렬되어 조회된다`() {
            // given
            val account1 = testAccount.copy(code = "3000", name = "계정3")
            val account2 = testAccount.copy(code = "1000", name = "계정1")
            val account3 = testAccount.copy(code = "2000", name = "계정2")

            accountRepository.save(account1)
            accountRepository.save(account2)
            accountRepository.save(account3)

            // when
            val accounts = accountRepository.findAllActive()

            // then
            assertThat(accounts).hasSize(3)
            assertThat(accounts[0].code).isEqualTo("1000")
            assertThat(accounts[1].code).isEqualTo("2000")
            assertThat(accounts[2].code).isEqualTo("3000")
        }
    }
}
