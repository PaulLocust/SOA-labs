package ru.itmo.soa.agency.web;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = Logger.getLogger(UnexpectedExceptionMapper.class.getName());

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception e) {
        log.log(Level.SEVERE, "Unexpected error", e);
        return ErrorResponses.build(500, "Internal server error", uriInfo).build();
    }
}
