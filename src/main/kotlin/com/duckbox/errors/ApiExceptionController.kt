package com.duckbox.errors

import com.duckbox.errors.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionController {

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(conflictException: ConflictException): ResponseEntity<APIException> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                APIException(
                    HttpStatus.CONFLICT,
                    conflictException.message!!
                )
            )
    }

    @ExceptionHandler(EthereumException::class)
    fun handleEthereumException(ethereumException: EthereumException) : ResponseEntity<APIException> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                APIException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ethereumException.message!!
                )
            )
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(forbiddenException: ForbiddenException): ResponseEntity<APIException> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                APIException(
                    HttpStatus.FORBIDDEN,
                    forbiddenException.message!!
                )
            )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(notFoundException: NotFoundException) : ResponseEntity<APIException> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                APIException(
                    HttpStatus.NOT_FOUND,
                    notFoundException.message!!
                )
            )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(unauthorizedException: UnauthorizedException) : ResponseEntity<APIException> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                APIException(
                    HttpStatus.UNAUTHORIZED,
                    unauthorizedException.message!!
                )
            )
    }

    @ExceptionHandler(UnknownException::class)
    fun handleUnknownException(unknownErrorException: UnknownException) : ResponseEntity<APIException> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                APIException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    unknownErrorException.message!!
                )
            )
    }
}
