package com.rooming.common.exception;

import com.rooming.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void businessExceptionKeepsItsHttpStatusAndErrorBodyContract() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusiness(
                new NotFoundException("Recommendation not found.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Recommendation not found.");
        assertThat(response.getBody().time()).isNotNull();
    }

    @Test
    void unexpectedExceptionUsesInternalServerErrorWithoutLeakingCause() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnexpected(
                new RuntimeException("database password leaked")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error.");
        assertThat(response.getBody().time()).isNotNull();
    }
}