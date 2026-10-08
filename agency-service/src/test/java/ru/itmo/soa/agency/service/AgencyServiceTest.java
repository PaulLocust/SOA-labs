package ru.itmo.soa.agency.service;

import org.junit.jupiter.api.Test;
import ru.itmo.soa.agency.model.Flat;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgencyServiceTest {

    private static final Map<Long, Flat> FLATS = Map.of(
            1L, new Flat(1, 100),
            2L, new Flat(2, 300),
            3L, new Flat(3, 300),
            4L, new Flat(4, 50));

    private boolean lastBalcony;
    private boolean lastAscending;

    private final AgencyService service = new AgencyService(new FlatsGateway() {
        @Override
        public Optional<Flat> findFirstByBalconyOrderedByPrice(boolean balcony, boolean ascending) {
            lastBalcony = balcony;
            lastAscending = ascending;
            return balcony ? Optional.of(FLATS.get(1L)) : Optional.empty();
        }

        @Override
        public Optional<Flat> findById(long id) {
            return Optional.ofNullable(FLATS.get(id));
        }
    });

    @Test
    void findWithBalconyPassesParametersToFlatsService() {
        assertEquals(1L, service.findWithBalcony("true", "true").getId());
        assertEquals(true, lastAscending);
        assertEquals(true, lastBalcony);
        service.findWithBalcony("FALSE", "True");
        assertEquals(false, lastAscending);
    }

    @Test
    void findWithBalconyErrors() {
        assertStatus(400, () -> service.findWithBalcony("not-a-bool", "true"));
        assertStatus(400, () -> service.findWithBalcony("true", "1"));
        AgencyException e = assertThrows(AgencyException.class, () -> service.findWithBalcony("true", "false"));
        assertEquals(404, e.getStatus());
        assertEquals("No flats without balcony found", e.getMessage());
    }

    @Test
    void getMostExpensive() {
        assertEquals(2L, service.getMostExpensive("1", "2", "4").getId());
        // при равной цене выбирается квартира, указанная раньше
        assertEquals(3L, service.getMostExpensive("3", "2", "1").getId());
    }

    @Test
    void getMostExpensiveErrors() {
        assertStatus(400, () -> service.getMostExpensive("1", "x", "3"));
        assertStatus(422, () -> service.getMostExpensive("0", "2", "3"));
        assertStatus(422, () -> service.getMostExpensive("1", "1", "3"));
        AgencyException e = assertThrows(AgencyException.class, () -> service.getMostExpensive("1", "2", "9"));
        assertEquals(404, e.getStatus());
        assertEquals("Flat with id 9 not found", e.getMessage());
    }

    private static void assertStatus(int status, Runnable action) {
        AgencyException e = assertThrows(AgencyException.class, action::run);
        assertEquals(status, e.getStatus());
    }
}
