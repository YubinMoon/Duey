package com.terry.duey.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalErrorHandlerTest {
    @Test
    void handle_delegatesErrorToConfiguredHandler() {
        CapturingErrorHandler capturingErrorHandler = new CapturingErrorHandler();
        GlobalErrorHandler globalErrorHandler = new GlobalErrorHandler(capturingErrorHandler);
        RuntimeException exception = new RuntimeException("failure");

        globalErrorHandler.handle(exception);

        assertSame(exception, capturingErrorHandler.error);
    }

    @Test
    void handle_preservesSpringErrorResponseStatus() {
        GlobalErrorHandler globalErrorHandler = new GlobalErrorHandler(new CapturingErrorHandler());

        ResponseEntity<?> response =
                globalErrorHandler.handle(
                        new ResponseStatusException(HttpStatus.BAD_GATEWAY, "provider failed"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    private static class CapturingErrorHandler implements ErrorHandler {
        private Throwable error;

        @Override
        public void handle(Throwable error) {
            this.error = error;
        }
    }
}
