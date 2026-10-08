package ru.itmo.soa.agency.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ru.itmo.soa.agency.model.Flat;
import ru.itmo.soa.agency.service.AgencyService;

/**
 * Операции агентства. Параметры пути принимаются строками и разбираются вручную,
 * чтобы на некорректные значения отвечать 400, а на семантические ошибки — 422 (как в спецификации).
 */
@Path("/")
@RequestScoped
@Produces(MediaType.APPLICATION_XML)
public class AgencyResource {

    @Inject
    AgencyService agency;

    @GET
    @Path("find-with-balcony/{cheapest}/{with-balcony}")
    public Flat findWithBalcony(@PathParam("cheapest") String cheapest,
                                @PathParam("with-balcony") String withBalcony) {
        return agency.findWithBalcony(cheapest, withBalcony);
    }

    @GET
    @Path("get-most-expensive/{id1}/{id2}/{id3}")
    public Flat getMostExpensive(@PathParam("id1") String id1,
                                 @PathParam("id2") String id2,
                                 @PathParam("id3") String id3) {
        return agency.getMostExpensive(id1, id2, id3);
    }
}
