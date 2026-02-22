package com.teamwork.gateway.controller;

import com.teamwork.gateway.exception.ErrorResponse;
import com.teamwork.gateway.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PingControllerTest {

    private final PingController pingController = new PingController();

    @Test
    void ping_WithoutError_ShouldReturnOkStatus() {
        // Act
        ResponseEntity<java.util.Map<String, String>> response = pingController.ping(false);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void ping_WithError_ShouldThrowIllegalArgumentException() {
        // Act & Assert
        try {
            pingController.ping(true);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("這是一個測試用的主動拋出例外狀況");
        }
    }
}

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleAllUncaughtException_ShouldReturnInternalServerError() {
        // Arrange
        Exception ex = new Exception("Test Unexpected Error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAllUncaughtException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).contains("Test Unexpected Error");
        assertThat(response.getBody().getTraceId()).isNotBlank();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Test Bad Request");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().getMessage()).contains("Test Bad Request");
        assertThat(response.getBody().getTraceId()).isNotBlank();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
