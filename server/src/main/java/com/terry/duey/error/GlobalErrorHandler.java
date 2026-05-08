package com.terry.duey.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalErrorHandler {
    private final ErrorHandler errorHandler;

    public GlobalErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception exception) {
        errorHandler.handle(exception);

        if (exception instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(errorResponse.getBody());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setDetail("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
