package com.unified.scheduler.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.unified.scheduler.config.MdcLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("urn:problem-type:resource-not-found"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(ResourceUnavailableException.class)
    public ProblemDetail handleResourceUnavailable(ResourceUnavailableException ex) {
        log.warn("Resource conflict/unavailable: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("urn:problem-type:resource-conflict"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(InvalidBookingException.class)
    public ProblemDetail handleInvalidBooking(InvalidBookingException ex) {
        log.warn("Invalid booking request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Booking Request");
        problem.setType(URI.create("urn:problem-type:invalid-booking"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problem.setTitle("Validation Failure");
        problem.setType(URI.create("urn:problem-type:validation-error"));

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        problem.setProperty("invalid_params", fieldErrors);
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed HTTP request payload: {}", ex.getMessage());
        String detail = "Malformed JSON request body or incompatible field data type.";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : String.valueOf(ref.getIndex()))
                .reduce((a, b) -> a + "." + b)
                .orElse("field");
            String targetType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "expected type";
            Object value = ife.getValue();

            if ("customerId".equalsIgnoreCase(fieldName)) {
                detail = String.format(
                    "Invalid value '%s' for field 'customerId': expected a numeric %s ID (e.g., 1, 2, 3). Customer IDs are numeric master IDs. You can view available customer profiles via GET /api/v1/customers.",
                    value, targetType
                );
            } else {
                detail = String.format(
                    "Invalid value '%s' for field '%s': expected value of type %s.",
                    value, fieldName, targetType
                );
            }
        } else if (cause instanceof JsonParseException jpe) {
            detail = "Invalid JSON syntax: " + jpe.getOriginalMessage();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            detail = "Required request body is missing.";
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Malformed Request Payload");
        problem.setType(URI.create("urn:problem-type:malformed-request"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameter(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Required request parameter '" + ex.getParameterName() + "' of type " + ex.getParameterType() + " is missing"
        );
        problem.setTitle("Missing Parameter");
        problem.setType(URI.create("urn:problem-type:missing-parameter"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch: name={}, value={}", ex.getName(), ex.getValue());
        String msg = String.format("Parameter '%s' with value '%s' could not be converted to expected type '%s'",
            ex.getName(), ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        problem.setTitle("Type Mismatch");
        problem.setType(URI.create("urn:problem-type:type-mismatch"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Constraint Violation");
        problem.setType(URI.create("urn:problem-type:constraint-violation"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        String detail = ex.getMessage();
        if (detail != null && detail.contains("Request method 'POST' is not supported")) {
            detail = detail + ". If you intended to call a sub-resource (e.g. /reschedule, /cancel, /reassign), please ensure the path parameter {id} is provided in the URL.";
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, detail);
        problem.setTitle("Method Not Allowed");
        problem.setType(URI.create("urn:problem-type:method-not-allowed"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Resource/Endpoint not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Endpoint Not Found");
        problem.setType(URI.create("urn:problem-type:endpoint-not-found"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public ProblemDetail handleErrorResponseException(org.springframework.web.ErrorResponseException ex) {
        log.warn("Spring web error: status={}, detail={}", ex.getStatusCode(), ex.getBody().getDetail());
        ProblemDetail problem = ex.getBody();
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unhandled server exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact system support.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:problem-type:internal-server-error"));
        enrichProblemDetail(problem);
        return problem;
    }

    private void enrichProblemDetail(ProblemDetail problem) {
        String traceId = MDC.get(MdcLoggingFilter.MDC_TRACE_ID_KEY);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
        problem.setProperty("timestamp", OffsetDateTime.now());
    }
}
