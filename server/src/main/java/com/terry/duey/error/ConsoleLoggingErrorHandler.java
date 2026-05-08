package com.terry.duey.error;

import org.springframework.stereotype.Component;

@Component
public class ConsoleLoggingErrorHandler implements ErrorHandler {
    @Override
    public void handle(Throwable error) {
        error.printStackTrace(System.err);
    }
}
