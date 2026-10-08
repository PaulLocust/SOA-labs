package ru.itmo.soa.flats.dto;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Схема {@code Error} из спецификации.
 */
@XmlRootElement(name = "error")
@XmlType(propOrder = {"timestamp", "status", "error", "message", "path"})
public class ApiError {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public ApiError() {
    }

    public ApiError(String timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
