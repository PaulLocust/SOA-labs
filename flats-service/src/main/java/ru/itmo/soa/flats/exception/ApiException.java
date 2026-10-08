package ru.itmo.soa.flats.exception;

import org.springframework.http.HttpStatus;

/**
 * Ошибка, которую сервис возвращает клиенту с конкретным HTTP-кодом из спецификации.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** 400 — не передан обязательный параметр, неверный тип или формат значения. */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 404 — квартира не найдена. */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** 422 — значения нарушают ограничения класса или диапазон фильтра некорректен. */
    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
