package com.example.interviewagent.exception;

import com.example.interviewagent.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionUsesItsHttpErrorStatus() {
        assertBusinessStatus(400, HttpStatus.BAD_REQUEST);
        assertBusinessStatus(401, HttpStatus.UNAUTHORIZED);
        assertBusinessStatus(403, HttpStatus.FORBIDDEN);
        assertBusinessStatus(404, HttpStatus.NOT_FOUND);
        assertBusinessStatus(409, HttpStatus.CONFLICT);
        assertBusinessStatus(500, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void unknownBusinessCodeFallsBackToBadRequest() {
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(
                new BusinessException(1001, "业务错误"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1001, response.getBody().getCode());
    }

    @Test
    void validationExceptionReturnsBadRequest() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.reject("invalid", "参数错误");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Result<Void>> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertEquals("参数错误", response.getBody().getMessage());
    }

    @Test
    void unexpectedExceptionReturnsInternalServerError() {
        ResponseEntity<Result<Void>> response = handler.handleException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
    }

    @Test
    void requestFormatExceptionReturnsBadRequest() {
        ResponseEntity<Result<Void>> response = handler.handleBadRequestException(
                new IllegalArgumentException("bad request"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void dataIntegrityViolationReturnsConflict() {
        ResponseEntity<Result<Void>> response = handler.handleDataConflict(
                new DataIntegrityViolationException("duplicate"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getCode());
    }

    private void assertBusinessStatus(int code, HttpStatus expectedStatus) {
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(
                new BusinessException(code, "test"));

        assertEquals(expectedStatus, response.getStatusCode());
        assertEquals(code, response.getBody().getCode());
    }
}
