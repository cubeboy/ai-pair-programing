# 회계 관리 솔루션 (Accounting Management Solution)

## 프로젝트 개요
REST API를 제공하는 회계 관리 솔루션의 백엔드 시스템입니다.

## 아키텍처
본 프로젝트는 **헥사고날 아키텍처(Hexagonal Architecture)** 패턴을 적용하여 구성되었습니다.

### 패키지 구조

```
com.jinnara.accounting/
├── domain/                           # 도메인 계층 (핵심 비즈니스 로직)
│   ├── account/                      # 계정과목 도메인
│   │   └── Account.kt               # 계정과목 엔티티
│   ├── transaction/                  # 거래 도메인
│   │   └── Transaction.kt           # 거래 엔티티
│   └── journal/                      # 분개장 도메인
│
├── application/                      # 애플리케이션 계층 (유스케이스)
│   ├── port/                        # 포트 인터페이스
│   │   ├── in/                      # 인바운드 포트 (Use Case)
│   │   │   ├── AccountUseCase.kt
│   │   │   └── TransactionUseCase.kt
│   │   └── out/                     # 아웃바운드 포트 (Repository)
│   │       ├── AccountRepository.kt
│   │       └── TransactionRepository.kt
│   └── service/                     # 애플리케이션 서비스
│       └── AccountService.kt
│
├── infrastructure/                   # 인프라스트럭처 계층 (외부 시스템)
│   ├── persistence/                 # 데이터 영속성
│   │   ├── account/
│   │   │   ├── AccountJpaEntity.kt
│   │   │   ├── AccountJpaRepository.kt
│   │   │   └── AccountRepositoryImpl.kt
│   │   ├── transaction/
│   │   └── journal/
│   └── config/                      # 설정
│
└── interface/                       # 인터페이스 계층 (외부 인터페이스)
    └── rest/                        # REST API
        ├── AccountController.kt
        └── dto/
            └── AccountDto.kt
```

## 헥사고날 아키텍처의 장점

1. **비즈니스 로직의 독립성**: 도메인 계층이 외부 기술에 의존하지 않음
2. **테스트 용이성**: 각 계층별로 독립적인 테스트 가능
3. **유연성**: 외부 어댑터 변경 시 핵심 로직에 영향 없음
4. **관심사의 분리**: 각 계층이 명확한 역할과 책임을 가짐

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.5.3
- **데이터베이스**: H2 (개발용)
- **ORM**: Spring Data JPA
- **빌드 도구**: Gradle
- **JVM**: Java 21

## 주요 도메인 모델

### 계정과목 (Account)
- 회계 시스템의 기본 단위
- 자산, 부채, 자본, 수익, 비용으로 분류
- 계층 구조 지원 (상위/하위 계정)

### 거래 (Transaction)
- 회계 거래를 나타내는 핵심 엔티티
- 차변과 대변의 균형 원칙 적용
- 임시저장, 전기완료, 취소 상태 관리

## API 엔드포인트

### 계정과목 관리
- `POST /api/v1/accounts` - 계정과목 생성
- `GET /api/v1/accounts` - 계정과목 목록 조회
- `GET /api/v1/accounts/{id}` - 특정 계정과목 조회
- `PUT /api/v1/accounts/{id}` - 계정과목 수정
- `DELETE /api/v1/accounts/{id}` - 계정과목 비활성화

## 실행 방법

```bash
# 애플리케이션 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

## 개발 가이드라인

1. **도메인 우선 설계**: 비즈니스 로직을 먼저 구현하고 인프라를 나중에 연결
2. **포트와 어댑터**: 인터페이스를 통한 의존성 역전 원칙 적용
3. **테스트 주도 개발**: 각 계층별 단위 테스트 및 통합 테스트 작성
4. **불변성**: 도메인 객체의 불변성 유지
5. **명시적 모델링**: 값 객체와 엔티티의 명확한 구분
