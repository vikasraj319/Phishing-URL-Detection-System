package com.phishguard.ai.exception;

import com.phishguard.ai.detection.InvalidInputException;
import com.phishguard.ai.dto.ApiDtos.ApiErrorDto;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Safe error bodies only: no stack traces, no echoing of submitted input. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ApiErrorDto> body(HttpStatus s, String code, String msg, Map<String, String> fields) {
        return ResponseEntity.status(s).body(new ApiErrorDto(s.value(), code, msg, fields));
    }

    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<ApiErrorDto> invalid(InvalidInputException e) { return body(HttpStatus.BAD_REQUEST, "INVALID_INPUT", e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorDto> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String first = fields.values().stream().findFirst().orElse("Request is not valid.");
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", first, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorDto> unreadable(Exception e) { return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request could not be read.", null); }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiErrorDto> notFound(NotFoundException e) { return body(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), null); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorDto> unexpected(Exception e) throws Exception {
        // Framework HTTP errors (unknown path 404, wrong method 405, unsupported media type 415...) keep their own status.
        if (e instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            String msg = status == HttpStatus.NOT_FOUND ? "Resource not found." : status == HttpStatus.METHOD_NOT_ALLOWED ? "Method not allowed."
                : status == HttpStatus.UNSUPPORTED_MEDIA_TYPE ? "Unsupported media type." : "Request could not be processed.";
            return body(status != null ? status : HttpStatus.BAD_REQUEST, "HTTP_" + er.getStatusCode().value(), msg, null);
        }
        log.error("Unhandled error ({})", e.getClass().getSimpleName(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong on the server.", null);
    }
}
