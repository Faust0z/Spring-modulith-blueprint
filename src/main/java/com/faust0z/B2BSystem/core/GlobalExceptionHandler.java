package com.faust0z.B2BSystem.core;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private void logError(Exception e, HttpStatus status, String message, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        String logPrefix = traceId != null ? "[TraceID: " + traceId + "]" : "[No TraceID]";

        if (status.is5xxServerError()) {
            log.error("{} Server Error [{}] = {} - Path={}", logPrefix, status.value(), message, request.getRequestURI(), e);
        } else if (status.is4xxClientError()) {
            log.warn("{} Client Error [{}] = {} - Path={}", logPrefix, status.value(), message, request.getRequestURI());
        } else {
            log.info("{} Response Status [{}] = {} - Path={}", logPrefix, status.value(), message, request.getRequestURI());
        }
    }

    private ProblemDetail buildProblemDetail(Exception e, HttpStatus status, String title, String detail, HttpServletRequest request) {
        logError(e, status, detail, request);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : "An error occurred");
        problem.setTitle(title);
        problem.setInstance(URI.create((request.getRequestURI())));

        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }

        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e, HttpServletRequest request) {
        String safeMessage = "Ha ocurrido un error inesperado en el sistema. Por favor, contacte con la administración e informe el código de seguimiento (traceId).";
        return buildProblemDetail(e, HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno", safeMessage, request);
    }

    @ExceptionHandler(DomainRuleException.class)
    public ProblemDetail handleDomainRuleException(DomainRuleException e, HttpServletRequest request) {
        return buildProblemDetail(e, e.getStatus(), e.getTitle(), e.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(org.springframework.security.authentication.BadCredentialsException e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.UNAUTHORIZED, "Fallo de Autenticación", "Correo o contraseña incorrectos", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.FORBIDDEN, "Acceso Denegado", "No tienes el rol requerido para realizar esta acción.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        var fieldError = e.getBindingResult().getFieldError();
        String cleanMessage = "Error de validación";

        if (fieldError != null) {
            if (fieldError.isBindingFailure()) {
                cleanMessage = String.format("Valor inválido '%s' para el parámetro '%s'.",
                        fieldError.getRejectedValue(), fieldError.getField());
            } else {
                cleanMessage = fieldError.getDefaultMessage();
            }
        }

        return buildProblemDetail(e, HttpStatus.BAD_REQUEST, "Error de Validación", cleanMessage, request);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(Exception e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.BAD_REQUEST, "JSON Malformado", "El cuerpo de la solicitud es inválido o falta el formato requerido.", request);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(Exception e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.METHOD_NOT_ALLOWED, "Método No Permitido", e.getMessage(), request);
    }

    @ExceptionHandler({org.springframework.web.context.request.async.AsyncRequestNotUsableException.class})
    public void handleClientDisconnect(Exception e, HttpServletRequest request) {
        log.debug("Client closed connection prematurely for path: {} - Message: {}", request.getRequestURI(), e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("El parámetro '%s' tiene un valor inválido.", e.getName());
        return buildProblemDetail(e, HttpStatus.BAD_REQUEST, "Tipo de parámetro incompatible", message, request);
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ProblemDetail handleBindException(org.springframework.validation.BindException e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.BAD_REQUEST, "Parámetros Inválidos", "Los parámetros enviados en la URL no son válidos.", request);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidationException(org.springframework.web.method.annotation.HandlerMethodValidationException e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.BAD_REQUEST, "Error de Validación", "Los datos enviados no cumplen con el formato requerido.", request);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException e, HttpServletRequest request) {
        String message = String.format("La ruta '%s' no existe en este servidor.", e.getResourcePath());
        return buildProblemDetail(e, HttpStatus.NOT_FOUND, "Recurso No Encontrado", message, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleGenericAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.UNAUTHORIZED, "No Autenticado", "Debes iniciar sesión para acceder a este recurso, o tu sesión ha expirado.", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailureException(OptimisticLockingFailureException e, HttpServletRequest request) {
        return buildProblemDetail(e, HttpStatus.CONFLICT, "Imposible completar la petición", "Otro proceso modificó el recurso pedido. Por favor reinténtelo luego", request);
    }
}
