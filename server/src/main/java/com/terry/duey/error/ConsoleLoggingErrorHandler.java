package com.terry.duey.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleLoggingErrorHandler implements ErrorHandler {
    @Override
    public void handle(Throwable error) {
        log.error("Unhandled server error", error);
    }
}
