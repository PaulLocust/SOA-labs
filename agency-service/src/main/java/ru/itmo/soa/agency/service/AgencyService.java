package ru.itmo.soa.agency.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.itmo.soa.agency.model.Flat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Операции агентства поверх API первого сервиса (openapi/agency-service.yaml).
 */
@ApplicationScoped
public class AgencyService {

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

    private FlatsGateway flats;

    /** Для CDI-прокси. */
    protected AgencyService() {
    }

    @Inject
    public AgencyService(FlatsGateway flats) {
        this.flats = flats;
    }

    /**
     * Самая дешёвая ({@code cheapest=true}) или самая дорогая квартира с балконом или без него.
     */
    public Flat findWithBalcony(String cheapestParam, String withBalconyParam) {
        boolean cheapest = parseBoolean("cheapest", cheapestParam);
        boolean withBalcony = parseBoolean("with-balcony", withBalconyParam);
        return flats.findFirstByBalconyOrderedByPrice(withBalcony, cheapest)
                .orElseThrow(() -> AgencyException.notFound(
                        "No flats " + (withBalcony ? "with" : "without") + " balcony found"));
    }

    /**
     * Самая дорогая из трёх квартир с заданными (разными) id.
     * При равной цене возвращается квартира, id которой указан раньше.
     */
    public Flat getMostExpensive(String id1Param, String id2Param, String id3Param) {
        List<Long> ids = List.of(parseId("id1", id1Param), parseId("id2", id2Param), parseId("id3", id3Param));
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) < 1) {
                throw AgencyException.unprocessable("Parameter 'id" + (i + 1) + "' must be greater than 0");
            }
        }
        Set<Long> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw AgencyException.unprocessable("id1, id2 and id3 must be different");
        }

        Flat best = null;
        for (long id : ids) {
            Flat flat = flats.findById(id)
                    .orElseThrow(() -> AgencyException.notFound("Flat with id " + id + " not found"));
            if (best == null || flat.getPrice() > best.getPrice()) {
                best = flat;
            }
        }
        return best;
    }

    private static boolean parseBoolean(String name, String raw) {
        String value = raw == null ? "" : raw.trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw AgencyException.badRequest("Parameter '" + name + "' must be a boolean");
    }

    private static long parseId(String name, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (INTEGER.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // выход за границы long
            }
        }
        throw AgencyException.badRequest("Parameter '" + name + "' must be an integer");
    }
}
