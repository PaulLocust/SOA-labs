package ru.itmo.soa.agency.web;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.soa.agency.service.AgencyException;

@Provider
public class AgencyExceptionMapper implements ExceptionMapper<AgencyException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AgencyException e) {
        return ErrorResponses.build(e.getStatus(), e.getMessage(), uriInfo).build();
    }
}
