package ru.itmo.soa.agency.service;

/**
 * Ошибка с HTTP-кодом, которая возвращается клиенту агентства.
 */
public class AgencyException extends RuntimeException {
    private final int status;

    public AgencyException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AgencyException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /** 400 — некорректный синтаксис параметров пути. */
    public static AgencyException badRequest(String message) {
        return new AgencyException(400, message);
    }

    public static AgencyException notFound(String message) {
        return new AgencyException(404, message);
    }

    /** 422 — семантическая ошибка параметров (id меньше 1, совпадающие id). */
    public static AgencyException unprocessable(String message) {
        return new AgencyException(422, message);
    }

    public int getStatus() {
        return status;
    }
}
