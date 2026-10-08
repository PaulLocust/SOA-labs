package ru.itmo.soa.agency.web;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Ошибки JAX-RS-рантайма: 406 (клиент не принимает XML), 404 (неизвестный URL), 405 и т.п.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Context
    Request request;

    @Override
    public Response toResponse(WebApplicationException e) {
        Response original = e.getResponse();
        int status = original.getStatus();
        String message = switch (status) {
            case 404 -> "Resource " + uriInfo.getRequestUri().getRawPath() + " not found";
            case 405 -> "Method " + request.getMethod() + " is not supported for this URL";
            case 406 -> "Only application/xml is supported";
            default -> e.getMessage();
        };
        Response.ResponseBuilder builder = ErrorResponses.build(status, message, uriInfo);
        String allow = original.getHeaderString(HttpHeaders.ALLOW);
        if (allow != null) {
            builder.header(HttpHeaders.ALLOW, allow);
        }
        return builder.build();
    }
}
