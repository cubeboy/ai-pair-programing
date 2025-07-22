package com.jinnara.accounting.adapter.rest

import com.jinnara.accounting.application.port.input.AccountUseCase
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.account.AccountType
import com.jinnara.accounting.adapter.rest.dto.AccountResponse
import com.jinnara.accounting.adapter.rest.dto.CreateAccountRequest
import com.jinnara.accounting.adapter.rest.dto.UpdateAccountRequest
import com.jinnara.accounting.application.port.command.CreateAccountCommand
import com.jinnara.accounting.application.port.command.UpdateAccountCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@Tag(name = "계정 관리", description = "회계 시스템의 계정 생성, 수정, 조회, 비활성화 API")
@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountUseCase: AccountUseCase
) {

    @Operation(
        summary = "계정 생성",
        description = """
            새로운 계정을 생성합니다.
            
            **기능:**
            - 계정 코드, 이름, 타입을 필수로 입력받습니다
            - 상위 계정(parentId)은 선택사항입니다
            - 계정 타입: ASSET(자산), LIABILITY(부채), EQUITY(자본), REVENUE(수익), EXPENSE(비용)
            
            **성공 시나리오:**
            - 일반 계정 생성: parentId 없이 생성 가능
            - 하위 계정 생성: parentId와 함께 생성 가능
            
            **실패 시나리오:**
            - 잘못된 요청 데이터: 400 Bad Request
            - 서버 처리 오류: 500 Internal Server Error
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "계정 생성 성공",
            content = [Content(schema = Schema(implementation = AccountResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터 (유효성 검사 실패)"
        ),
        ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류 (계정 생성 실패)"
        )
    )
    @PostMapping
    fun createAccount(@Valid @RequestBody request: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val command = CreateAccountCommand(
            code = request.code,
            name = request.name,
            type = request.type,
            parentId = request.parentId?.let { AccountId(it) }
        )

        val account = accountUseCase.createAccount(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AccountResponse.fromDomain(account))
    }

    @Operation(
        summary = "계정 수정",
        description = """
            기존 계정의 정보를 수정합니다.
            
            **기능:**
            - 계정 이름과 상위 계정(parentId)을 수정할 수 있습니다
            - 계정 코드와 타입은 수정할 수 없습니다
            
            **성공 시나리오:**
            - 계정 이름만 수정
            - 상위 계정 변경
            - 상위 계정을 null로 설정 (최상위 계정으로 변경)
            
            **실패 시나리오:**
            - 존재하지 않는 계정 ID: 404 Not Found
            - 잘못된 요청 데이터: 400 Bad Request
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "계정 수정 성공",
            content = [Content(schema = Schema(implementation = AccountResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터 (유효성 검사 실패)"
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 계정 ID"
        )
    )
    @PutMapping("/{id}")
    fun updateAccount(
        @Parameter(description = "수정할 계정의 ID", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAccountRequest
    ): ResponseEntity<AccountResponse> {
        val command = UpdateAccountCommand(
            accountId = AccountId(id),
            name = request.name,
            parentId = request.parentId?.let { AccountId(it) }
        )

        val account = accountUseCase.updateAccount(command)
        return ResponseEntity.ok(AccountResponse.fromDomain(account))
    }

    @Operation(
        summary = "계정 비활성화",
        description = """
            계정을 비활성 상태로 변경합니다.
            
            **기능:**
            - 계정을 삭제하지 않고 비활성 상태로 변경합니다
            - 비활성화된 계정은 일반 목록 조회에서 제외됩니다
            - 기존 거래 내역은 유지됩니다
            
            **성공 시나리오:**
            - 활성 계정을 비활성화
            - 응답에서 isActive = false 확인 가능
            
            **실패 시나리오:**
            - 존재하지 않는 계정 ID: 404 Not Found
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "계정 비활성화 성공",
            content = [Content(schema = Schema(implementation = AccountResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 계정 ID"
        )
    )
    @DeleteMapping("/{id}")
    fun deactivateAccount(
        @Parameter(description = "비활성화할 계정의 ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<AccountResponse> {
        val account = accountUseCase.deactivateAccount(AccountId(id))
        return ResponseEntity.ok(AccountResponse.fromDomain(account))
    }

    @Operation(
        summary = "계정 단건 조회",
        description = """
            ID로 특정 계정의 상세 정보를 조회합니다.
            
            **기능:**
            - 계정 ID로 특정 계정의 모든 정보를 조회
            - 활성/비활성 상태와 관계없이 조회 가능
            
            **응답 정보:**
            - 계정 ID, 코드, 이름, 타입
            - 상위 계정 ID (있는 경우)
            - 활성 상태, 생성/수정 일시
            
            **실패 시나리오:**
            - 존재하지 않는 계정 ID: 404 Not Found
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "계정 조회 성공",
            content = [Content(schema = Schema(implementation = AccountResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 계정 ID"
        )
    )
    @GetMapping("/{id}")
    fun getAccount(
        @Parameter(description = "조회할 계정의 ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<AccountResponse> {
        val account = accountUseCase.getAccount(AccountId(id))
        return ResponseEntity.ok(AccountResponse.fromDomain(account))
    }

    @Operation(
        summary = "계정 목록 조회",
        description = """
            활성 계정 목록을 조회합니다. 타입별 필터링이 가능합니다.
            
            **기능:**
            - 모든 활성 계정 목록 조회 (type 파라미터 없음)
            - 특정 타입의 계정만 조회 (type 파라미터 사용)
            - 비활성 계정은 목록에서 제외됩니다
            
            **타입별 조회:**
            - ASSET: 자산 계정들
            - LIABILITY: 부채 계정들  
            - EQUITY: 자본 계정들
            - REVENUE: 수익 계정들
            - EXPENSE: 비용 계정들
            
            **응답:**
            - 계정이 없는 경우: 빈 배열 반환
            - 배열 형태로 계정 목록 반환
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "계정 목록 조회 성공 (빈 목록 포함)",
            content = [Content(schema = Schema(implementation = Array<AccountResponse>::class))]
        )
    )
    @GetMapping
    fun getAccounts(
        @Parameter(
            description = "계정 타입으로 필터링 (선택사항)",
            example = "ASSET",
            schema = Schema(allowableValues = ["ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"])
        )
        @RequestParam(required = false) type: AccountType?
    ): ResponseEntity<List<AccountResponse>> {
        val accounts = if (type != null) {
            accountUseCase.getAccountsByType(type)
        } else {
            accountUseCase.getAllActiveAccounts()
        }

        val response = accounts.map { AccountResponse.fromDomain(it) }
        return ResponseEntity.ok(response)
    }
}
