package com.example.interviewagent.exception;

import com.example.interviewagent.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        // 统一响应体保留业务码，同时使用对应的 HTTP 错误状态。
        HttpStatus httpStatus = HttpStatus.resolve(e.getCode());
        if (httpStatus == null || !httpStatus.isError()) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(httpStatus).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().isEmpty()
                ? "参数错误"
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }

    @ExceptionHandler({
            BindException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            TypeMismatchException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequestException(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求参数格式错误"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(409, "数据冲突，请勿重复提交"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(Result.fail(500, "系统异常：" + e.getMessage()));
    }
}
