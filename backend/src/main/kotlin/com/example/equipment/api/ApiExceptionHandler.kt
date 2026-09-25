package com.example.equipment.api

import com.example.equipment.application.BUSINESS_RULE_VIOLATION
import com.example.equipment.application.EquipmentRequestAccessDenied
import com.example.equipment.application.EquipmentRequestBusinessRuleViolation
import com.example.equipment.application.EquipmentRequestNotFound
import com.example.equipment.application.EquipmentRequestValidationFailed
import com.example.equipment.application.EquipmentRequestVersionConflict
import com.example.equipment.application.ITEMS_REQUIRED
import com.example.equipment.application.REJECTION_REASON_REQUIRED
import com.example.equipment.domain.InvalidRequestTransition
import com.example.equipment.domain.RequestNotEditable
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Clock
import java.time.Instant

data class ApiErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val fieldErrors: Map<String, String> = emptyMap(),
)

@RestControllerAdvice
class ApiExceptionHandler(
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidArguments(exception: MethodArgumentNotValidException, request: HttpServletRequest) =
        error(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            request,
            exception.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") },
        )

    @ExceptionHandler(EquipmentRequestValidationFailed::class)
    fun domainValidation(exception: EquipmentRequestValidationFailed, request: HttpServletRequest) =
        error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, exception.fieldErrors)

    @ExceptionHandler(
        MalformedIdentity::class,
        MissingRequestHeaderException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
        ConstraintViolationException::class,
    )
    fun malformed(exception: Exception, request: HttpServletRequest) =
        error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request format is invalid", request)

    @ExceptionHandler(EquipmentRequestAccessDenied::class)
    fun forbidden(exception: EquipmentRequestAccessDenied, request: HttpServletRequest) =
        error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You cannot access this equipment request", request)

    @ExceptionHandler(EquipmentRequestNotFound::class)
    fun notFound(exception: EquipmentRequestNotFound, request: HttpServletRequest) =
        error(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Equipment request was not found", request)

    @ExceptionHandler(EquipmentRequestVersionConflict::class, OptimisticLockingFailureException::class)
    fun versionConflict(exception: Exception, request: HttpServletRequest) =
        error(HttpStatus.CONFLICT, "REQUEST_VERSION_CONFLICT", "This request has been updated by another user", request)

    @ExceptionHandler(RequestNotEditable::class)
    fun stateConflict(exception: RequestNotEditable, request: HttpServletRequest) =
        error(HttpStatus.CONFLICT, "REQUEST_STATE_CONFLICT", "This request can no longer be edited", request)

    @ExceptionHandler(InvalidRequestTransition::class)
    fun transitionConflict(exception: InvalidRequestTransition, request: HttpServletRequest) =
        error(
            HttpStatus.CONFLICT,
            "REQUEST_STATE_CONFLICT",
            "This action is not allowed while the request is ${exception.currentStatus}",
            request,
        )

    @ExceptionHandler(EquipmentRequestBusinessRuleViolation::class)
    fun businessRule(exception: EquipmentRequestBusinessRuleViolation, request: HttpServletRequest) =
        error(
            HttpStatus.UNPROCESSABLE_CONTENT,
            exception.code,
            businessRuleMessages[exception.code] ?: "The request does not satisfy a business rule",
            request,
            exception.fieldErrors,
        )

    @ExceptionHandler(Exception::class)
    fun unexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected request failure", exception)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request)
    }

    private val businessRuleMessages = mapOf(
        ITEMS_REQUIRED to "At least one equipment item is required before submit",
        BUSINESS_RULE_VIOLATION to "The request cannot be submitted in its current form",
        REJECTION_REASON_REQUIRED to "A rejection reason is required",
    )

    private fun error(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        fieldErrors: Map<String, String> = emptyMap(),
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
        ApiErrorResponse(Instant.now(clock), status.value(), code, message, request.requestURI, fieldErrors),
    )
}

