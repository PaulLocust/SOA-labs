package ru.itmo.soa.agency.web;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import ru.itmo.soa.agency.model.ApiError;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Построение XML-ответа со схемой {@code Error}. Тип ответа задаётся явно, чтобы тело ошибки
 * было записано в XML даже тогда, когда клиент запросил другой формат (ответ 406).
 */
final class ErrorResponses {

    private ErrorResponses() {
    }

    static Response.ResponseBuilder build(int status, String message, UriInfo uriInfo) {
        String path = uriInfo == null ? null : uriInfo.getRequestUri().getRawPath();
        ApiError error = new ApiError(
                DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS)),
                status, reasonPhrase(status), message, path);
        return Response.status(status).type(MediaType.APPLICATION_XML_TYPE).entity(error);
    }

    private static String reasonPhrase(int status) {
        if (status == 422) {
            return "Unprocessable Entity";
        }
        Response.Status known = Response.Status.fromStatusCode(status);
        return known == null ? "Error" : known.getReasonPhrase();
    }
}
