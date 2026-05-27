package com.skku.zip.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skku.zip.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SecurityErrorResponseWriter securityErrorResponseWriter = new SecurityErrorResponseWriter(
            objectMapper
    );

    @Test
    void securityErrorWritesHttpStatusAndSharedErrorBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "Authentication is required.");

        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(body.message()).isEqualTo("Authentication is required.");
        assertThat(body.time()).isNotNull();
    }
}
