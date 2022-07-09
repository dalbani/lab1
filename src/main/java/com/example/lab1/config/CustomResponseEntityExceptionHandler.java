package com.example.lab1.config;

import lombok.AllArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.Map;

@RestControllerAdvice
@AllArgsConstructor
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorAttributes errorAttributes;

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraintViolationException(ConstraintViolationException exc, WebRequest request) {
        Map<String, Object> body = errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("problems", exc.getConstraintViolations().stream().map(constraintViolation ->
                String.format("Property \"%s\": %s.", constraintViolation.getPropertyPath(),
                        constraintViolation.getMessage())
        ).toList());
        return ResponseEntity.status(status).body(body);
    }
}
