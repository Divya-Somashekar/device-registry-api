package org.device.deviceregistryapi.common;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.device.deviceregistryapi.device.DeviceInUseException;
import org.device.deviceregistryapi.device.DeviceNotFoundException;
import org.device.deviceregistryapi.device.InvalidDeviceException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into RFC 7807 responses, so every error shares one shape and no
 * controller has to build an error body itself.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    ProblemDetail handleNotFound(DeviceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Device not found", exception.getMessage(),
                "device-not-found");
    }

    @ExceptionHandler(DeviceInUseException.class)
    ProblemDetail handleInUse(DeviceInUseException exception) {
        return problem(HttpStatus.CONFLICT, "Device is in use", exception.getMessage(),
                "device-in-use");
    }

    /**
     * A write built on a stale read: another transaction changed the device between the point
     * this one loaded it and the point it tried to write. The request is not wrong, so it is
     * reported as a conflict the client can resolve by re-reading and retrying, rather than as
     * a server fault.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentModification() {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The device was changed by another request. Fetch it again and retry.",
                "concurrent-modification");
    }

    @ExceptionHandler(InvalidDeviceException.class)
    ProblemDetail handleInvalidDevice(InvalidDeviceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid device", exception.getMessage(),
                "invalid-device");
    }

    /**
     * An unknown property in the {@code sort} parameter. Sorting is part of the public
     * contract of the collection endpoint, so naming a property that does not exist is a
     * client error rather than a server fault.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleUnknownSortProperty(PropertyReferenceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort property",
                "'%s' is not a sortable property of a device"
                        .formatted(exception.getPropertyName()),
                "invalid-sort-property");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "The request body is not valid", "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Covers malformed JSON and an unrecognised enum value in the body, both of which fail
     * before validation runs.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable() {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request",
                "The request body could not be read. Check the JSON syntax and that every "
                        + "value is of the expected type.", "malformed-request");
    }

    /** An unparseable path variable or query parameter, such as a bad UUID or state. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "'%s' is not a valid value for '%s'"
                        .formatted(exception.getValue(), exception.getName()),
                "invalid-parameter");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail,
            String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.device-registry/problems/" + type));
        return problem;
    }

    record FieldError(String field, String message) {
    }
}
