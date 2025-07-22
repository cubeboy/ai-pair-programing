package com.jinnara.accounting.adapter.rest

import com.jinnara.accounting.application.port.input.TransactionUseCase
import com.jinnara.accounting.domain.transaction.TransactionId
import com.jinnara.accounting.domain.account.AccountId
import com.jinnara.accounting.domain.transaction.EntryType
import com.jinnara.accounting.adapter.rest.dto.*
import com.jinnara.accounting.application.port.command.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.LocalDate

@Tag(name = "거래 관리", description = "회계 거래 생성, 수정, 취소, 조회 API")
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(
    private val transactionUseCase: TransactionUseCase
) {

    @Operation(
        summary = "거래 생성",
        description = """
            새로운 회계 거래를 생성합니다.
            
            **기능:**
            - 복식부기 원칙에 따라 차변과 대변이 균형을 이루어야 합니다
            - 여러 개의 분개 항목(Journal Entry)을 포함할 수 있습니다
            - 거래 설명, 날짜, 분개 항목들을 필수로 입력받습니다
            
            **성공 시나리오:**
            - 차변과 대변 합계가 일치하는 정상 거래 생성
            - 여러 계정이 포함된 복합 거래 생성
            - 거래 상태는 PENDING으로 초기화됩니다
            
            **실패 시나리오:**
            - 필수 필드 누락: 400 Bad Request
            - 차변과 대변 불균형: 400 Bad Request
            - 존재하지 않는 계정 ID: 400 Bad Request
            - 서버 처리 오류: 500 Internal Server Error
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "거래 생성 성공",
            content = [Content(schema = Schema(implementation = TransactionResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터 (필수 필드 누락, 차변/대변 불균형 등)"
        ),
        ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류"
        )
    )
    @PostMapping
    fun createTransaction(@Valid @RequestBody request: CreateTransactionRequest): ResponseEntity<TransactionResponse> {
        val command = CreateTransactionCommand(
            description = request.description,
            date = request.transactionDate,
            reference = null, // reference 매개변수 추가
            entries = request.journalEntries.map { entry ->
                CreateTransactionEntryCommand(
                    accountId = AccountId(entry.accountId),
                    type = EntryType.valueOf(entry.entryType),
                    amount = entry.amount,
                    description = entry.description
                )
            }
        )

        val transaction = transactionUseCase.createTransaction(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TransactionResponse.fromDomain(transaction))
    }

    @Operation(
        summary = "거래 수정",
        description = """
            기존 거래의 정보를 수정합니다.
            
            **기능:**
            - 거래 설명, ��짜, 분개 항목들을 수정할 수 있습니다
            - 수정 시에도 차변과 대변의 균형을 유지해야 합니다
            - 기존 분개 항목들은 새로운 항목들로 완전히 대체됩니다
            
            **성공 시나리오:**
            - 거래 설명만 수정
            - 거래 날짜 변경
            - 분개 항목 수정 (금액, 계정 등)
            - 복합적인 수정 (설명 + 날짜 + 분개 항목)
            
            **실패 시나리오:**
            - 존재하지 않는 거래 ID: 404 Not Found
            - 잘못된 요청 데이터: 400 Bad Request
            - 차변과 대변 불균형: 400 Bad Request
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "거래 수정 성공",
            content = [Content(schema = Schema(implementation = TransactionResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터 (차변/대변 불균형 등)"
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 거래 ID"
        )
    )
    @PutMapping("/{id}")
    fun updateTransaction(
        @Parameter(description = "수정할 거래의 ID", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val command = UpdateTransactionCommand(
            transactionId = TransactionId(id),
            description = request.description,
            date = request.transactionDate,
            reference = null, // reference 매개변수 추가
            entries = request.journalEntries.map { entry ->
                CreateTransactionEntryCommand(
                    accountId = AccountId(entry.accountId),
                    type = EntryType.valueOf(entry.entryType),
                    amount = entry.amount,
                    description = entry.description
                )
            }
        )

        val transaction = transactionUseCase.updateTransaction(command)
        return ResponseEntity.ok(TransactionResponse.fromDomain(transaction))
    }

    @Operation(
        summary = "거래 취소",
        description = """
            기존 거래를 취소하고 역분개를 생성합니다.
            
            **기능:**
            - 원본 거래의 상태를 CANCELLED로 변경합니다
            - 원본 거래와 반대 방향의 역분개 거래를 자동 생성합니다
            - 취소 사유를 필수로 입력받습니다
            - 취소 후에는 회계 잔액이 원상복구됩니다
            
            **성공 시나리오:**
            - PENDING 상태의 거래 취소
            - 취소 사유와 함께 역분개 생성
            - 원본 거래와 역분개 거래 정보 반환
            
            **실패 시나리오:**
            - 이미 취소된 거래 재취소 시도: 400 Bad Request
            - 존재하지 않는 거래 ID: 404 Not Found
            - 취소 불가능한 상태의 거래: 400 Bad Request
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "거래 취소 성공",
            content = [Content(schema = Schema(implementation = CancelTransactionResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (이미 취소된 거래, 취소 불가능한 상태 등)"
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 거래 ID"
        )
    )
    @PostMapping("/{id}/cancel")
    fun cancelTransaction(
        @Parameter(description = "취소할 거래의 ID", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody request: CancelTransactionRequest
    ): ResponseEntity<CancelTransactionResponse> {
        val command = CancelTransactionCommand(
            transactionId = TransactionId(id),
            cancelReason = request.cancelReason
        )

        val result = transactionUseCase.cancelTransaction(command)
        return ResponseEntity.ok(
            CancelTransactionResponse(
                originalTransaction = TransactionResponse.fromDomain(result.originalTransaction),
                reversalTransaction = TransactionResponse.fromDomain(result.reversalTransaction),
                cancelReason = result.cancelReason
            )
        )
    }

    @Operation(
        summary = "거래 단건 조회",
        description = """
            ID로 특정 거래의 상세 정보를 조회합니다.
            
            **기능:**
            - 거래 ID로 특정 거래의 모든 정보를 조회
            - 분개 항목들의 상세 정보도 함께 반환
            - 취소된 거래도 조회 가능
            
            **응답 정보:**
            - 거래 기본 정보 (ID, 설명, 날짜, 상태)
            - 총 거래 금액
            - 분개 항목 목록 (계정 정보, 차변/대변, 금액)
            - 생성/수정 일시
            
            **실패 시나리오:**
            - 존재하지 않는 거래 ID: 404 Not Found
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "거래 조회 성공",
            content = [Content(schema = Schema(implementation = TransactionResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 거래 ID"
        )
    )
    @GetMapping("/{id}")
    fun getTransaction(
        @Parameter(description = "조회할 거래의 ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<TransactionResponse> {
        val transaction = transactionUseCase.getTransaction(TransactionId(id))
        return ResponseEntity.ok(TransactionResponse.fromDomain(transaction))
    }

    @Operation(
        summary = "거래 목록 조회",
        description = """
            거래 목록을 페이징하여 조회합니다. 날짜 범위 필터링이 가능합니다.
            
            **기능:**
            - 전체 거래 목록 조회 (날짜 필터 없음)
            - 시작일/종료일로 날짜 범위 필터링
            - 페이징 지원 (page, size 파라미터)
            - 취소된 거래도 목록에 포함됩니다
            
            **쿼리 파라미터:**
            - startDate: 조회 시작일 (yyyy-MM-dd 형식, 선택사항)
            - endDate: 조회 종료일 (yyyy-MM-dd 형식, 선택사항)
            - page: 페이지 번호 (기본값: 0)
            - size: 페이지 크기 (기본값: 20)
            
            **응답:**
            - 거래가 없는 경우: 빈 배열 반환
            - 페이징 정보와 함께 거래 목록 반환
            - 총 건수, 페이지 수 등의 메타데이터 포함
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "거래 목록 조회 성공 (빈 목록 포함)",
            content = [Content(schema = Schema(implementation = TransactionPageResponse::class))]
        )
    )
    @GetMapping
    fun getTransactions(
        @Parameter(
            description = "조회 시작일 (yyyy-MM-dd 형식, 선택사항)",
            example = "2024-01-01"
        )
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @Parameter(
            description = "조회 종료일 (yyyy-MM-dd 형식, 선택사항)",
            example = "2024-01-31"
        )
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<TransactionPageResponse> {
        val query = GetTransactionsQuery(
            startDate = startDate,
            endDate = endDate,
            pageable = PageRequest.of(page, size)
        )

        val transactionPage = transactionUseCase.getTransactions(query)

        val response = TransactionPageResponse(
            content = transactionPage.content.map { TransactionResponse.fromDomain(it) },
            totalElements = transactionPage.totalElements,
            totalPages = transactionPage.totalPages,
            size = transactionPage.size,
            number = transactionPage.number,
            first = transactionPage.isFirst,
            last = transactionPage.isLast
        )

        return ResponseEntity.ok(response)
    }
}
