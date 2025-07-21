# Midas 회계 관리 시스템 - Copilot 규칙

## 프로젝트 개요
- **이름**: Midas (회계 관리 솔루션)
- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.5.3
- **아키텍처**: 헥사고날 아키텍처 (Hexagonal Architecture)
- **패키지 구조**: `com.jinnara.accounting`

## 아키텍처 패턴 및 규칙

### 1. 헥사고날 아키텍처 준수
- **도메인 계층**: 비즈니스 로직과 엔티티 (`domain/`)
- **애플리케이션 계층**: 유즈케이스와 포트 (`application/`)
- **인프라 계층**: 외부 시스템 연동 (`infrastructure/`)
- **인터페이스 계층**: REST API 및 DTO (`interface/`)

### 2. 패키지 네이밍 규칙
```
com.jinnara.accounting/
├── domain/           # 도메인 엔티티와 비즈니스 로직
├── application/      # 애플리케이션 서비스
│   ├── port/
│   │   ├── input/    # 인바운드 포트 (UseCase 인터페이스)
│   │   └── output/   # 아웃바운드 포트 (Repository 인터페이스)
│   └── service/      # 애플리케이션 서비스 구현
├── infrastructure/   # 외부 시스템 어댑터
│   └── persistence/  # 데이터베이스 관련
└── interface/        # 외부 인터페이스
    └── rest/         # REST API 컨트롤러
```

### 3. 코딩 규칙

#### 도메인 엔티티
- data class로 불변성 유지
- 도메인 로직은 엔티티 내부에 구현
- ID는 타입 안전성을 위해 별도 클래스로 정의 (예: `AccountId`)
- 생성/수정 시간은 `LocalDateTime` 사용

#### 애플리케이션 서비스
- UseCase 인터페이스로 비즈니스 기능 정의
- Command 패턴으로 입력 데이터 구조화
- 트랜잭션 경계는 서비스 계층에서 관리

#### REST API
- `/api/v1/` 접두사 사용
- RESTful 설계 원칙 준수
- Request/Response DTO는 별도 패키지 관리
- HTTP 상태 코드 적절히 활용

#### 데이터베이스
- JPA Entity는 infrastructure 계층에 위치
- **도메인 엔티티와 JPA 엔티티 분리**: 각각 다른 목적과 특성을 가짐
- **JPA Entity 가변성 규칙**:
  - JPA Entity는 `var` 속성을 사용하여 가변성 허용
  - 업데이트 메소드를 통해 비즈니스 로직과 검증 수행
  - 직접 필드 변경보다는 의미있는 메소드명 사용

#### JPA Entity 업데이트 패턴
```kotlin
// 좋은 예: 업데이트 메소드 제공
@Entity
class AccountJpaEntity(
    // ...기존 필드들...
    var name: String,
    var isActive: Boolean,
    var updatedAt: LocalDateTime
) {
    fun updateName(newName: String) {
        this.name = newName
        this.updatedAt = LocalDateTime.now()
    }
    
    fun deactivate() {
        this.isActive = false
        this.updatedAt = LocalDateTime.now()
    }
    
    // Kotlin의 apply/let을 활용한 비구조화 할당 스타일
    fun updateWith(block: AccountJpaEntity.() -> Unit): AccountJpaEntity {
        return this.apply(block).also { 
            this.updatedAt = LocalDateTime.now() 
        }
    }
}

// 사용 예시:
entity.updateWith {
    name = "새로운 계정명"
    isActive = true
}
```

#### 도메인-JPA 매핑 규칙
- **도메인 → JPA**: `toDomain()` 확장 함수 사용
- **JPA → 도메인**: `toJpaEntity()` 확장 함수 사용
- **부분 업데이트**: `updateFrom(domain)` 메소드로 선택적 필드 업데이트

```kotlin
// 매핑 확장 함수 예시
fun AccountJpaEntity.toDomain(): Account = Account(
    id = AccountId(this.id),
    code = this.code,
    name = this.name,
    // ...기타 필드들...
)

fun Account.toJpaEntity(): AccountJpaEntity = AccountJpaEntity(
    id = this.id.value,
    code = this.code,
    name = this.name,
    // ...기타 필드들...
)

// 부분 업데이트를 위한 메소드
fun AccountJpaEntity.updateFrom(domain: Account): AccountJpaEntity {
    return this.updateWith {
        name = domain.name
        isActive = domain.isActive
        // 필요한 필드만 선택적 업데이트
    }
}
```

### 4. 네이밍 컨벤션
- **클래스**: PascalCase (예: `AccountService`)
- **메소드/변수**: camelCase (예: `createAccount`)
- **상수**: UPPER_SNAKE_CASE
- **패키지**: lowercase with dots
- **UseCase 인터페이스**: `{Domain}UseCase` 형식
- **Command**: `{Action}{Domain}Command` 형식
- **JPA Entity**: `{Domain}JpaEntity` 형식

### 5. 코드 작성 지침

#### 코틀린 스타일
- 널 안전성 적극 활용
- data class 적극 사용 (도메인 엔티티용)
- extension function 적절히 활용
- when 표현식 선호
- **apply/let/run/with 스코프 함수** 활용하여 가독성 향상

#### 주석 및 문서화
- 도메인 엔티티와 주요 비즈니스 로직에 KDoc 주석
- 복잡한 비즈니스 규칙은 상세 주석 작성
- API 엔드포인트는 기능 설명 포함

#### 테스트
- 단위 테스트의 Mockup Framework 는 Mockk 사용
- 통합 테스트는 Spring Boot Test
- 테스트 메소드명은 한글로 명확하게 작성
- JPA Repository 구현체 테스트는 @DataJpaTest 어노테이션 사용
- 테스트 케이스는 정상 케이스 보다 예외 및 오류 케이스를 풍부하게 작성

### 6. 의존성 관리
- Spring Boot Starter 적극 활용
- Jackson Kotlin Module 사용
- H2 데이터베이스 (테스트용)
- MockK (모킹 프레임워크)

### 7. 비즈니스 도메인 규칙

#### 계정과목 (Account)
- 차변/대변 구분 로직 포함
- 계정 유형: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
- 계층 구조 지원 (parentId)
- 활성/비활성 상태 관리

#### 거래 (Transaction)
- 복식부기 원칙 준수
- 차변/대변 합계 일치 검증

### 8. 에러 처리
- 도메인 예외는 별도 패키지에서 관리
- 글로벌 예외 핸들러 구현
- 적절한 HTTP 상태 코드 반환

### 9. 성능 고려사항
- 지연 로딩 적절히 활용
- 쿼리 최적화 고려
- 페이징 처리 구현

### 10. JPA Entity vs Domain Entity 사용 지침
- **Domain Entity**: 비즈니스 로직 중심, 불변성 유지, 순수한 도메인 모델
- **JPA Entity**: 영속성 관리 중심, 가변성 허용, 업데이트 편의성 제공
- **Repository 구현체**에서만 JPA Entity 사용, 서비스 계층에서는 Domain Entity 사용
- **매핑 로직**은 Repository 구현체에서 담당

이 규칙들을 따라 일관되고 유지보수 가능한 코드를 작성해주세요.
