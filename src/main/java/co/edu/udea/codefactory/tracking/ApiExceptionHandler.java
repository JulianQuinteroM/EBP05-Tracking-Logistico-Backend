package co.edu.udea.codefactory.tracking;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(String code, String message, Map<String, String> fields) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Revise los campos indicados", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> json() {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_JSON", "Datos no válidos o campo no permitido", Map.of()));
    }

    @ExceptionHandler(InvalidTrackingCodeException.class)
    ResponseEntity<ApiError> invalidCode(InvalidTrackingCodeException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_TRACKING_CODE", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    ResponseEntity<ApiError> notFound(ShipmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError("SHIPMENT_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler({ShipmentConflictException.class, ObjectOptimisticLockingFailureException.class,
                       OptimisticLockingFailureException.class})
    ResponseEntity<ApiError> conflict(Exception exception) {
        String message = exception instanceof ShipmentConflictException ? exception.getMessage()
            : "El envío cambió; recargue la ficha antes de continuar";
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError("SHIPMENT_CONFLICT", message, Map.of()));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException exception) {
        return ResponseEntity.badRequest().body(new ApiError("BUSINESS_RULE", exception.getMessage(), Map.of()));
    }
}
