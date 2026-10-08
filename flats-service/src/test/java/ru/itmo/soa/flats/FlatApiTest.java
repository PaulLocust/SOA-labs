package ru.itmo.soa.flats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.soa.flats.config.WebConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringJUnitWebConfig(WebConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FlatApiTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static MockHttpServletRequestBuilder withFlat(MockHttpServletRequestBuilder builder, String name,
                                                          long area, long price, boolean balcony, long rooms,
                                                          String transport) {
        return builder.queryParam("name", name)
                .queryParam("coordinatesX", "10.5")
                .queryParam("coordinatesY", "-20")
                .queryParam("area", String.valueOf(area))
                .queryParam("price", String.valueOf(price))
                .queryParam("balcony", String.valueOf(balcony))
                .queryParam("numberOfRooms", String.valueOf(rooms))
                .queryParam("transport", transport);
    }

    private ResultActions create(String name, long area, long price, boolean balcony, long rooms, String transport)
            throws Exception {
        return mvc.perform(withFlat(post("/flats"), name, area, price, balcony, rooms, transport));
    }

    @Test
    void createAndGet() throws Exception {
        mvc.perform(withFlat(post("/flats"), "A", 50, 1000, true, 2, "FEW")
                        .queryParam("furnish", "DESIGNER")
                        .queryParam("houseName", "H")
                        .queryParam("houseYear", "1990")
                        .queryParam("houseNumberOfFlatsOnFloor", "4")
                        .queryParam("houseNumberOfLifts", "1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/flats/1"))
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(xpath("/flat/id").string("1"))
                .andExpect(xpath("/flat/coordinates/x").string("10.5"))
                .andExpect(xpath("/flat/furnish").string("DESIGNER"))
                .andExpect(xpath("/flat/view").doesNotExist())
                .andExpect(xpath("/flat/house/name").string("H"))
                .andExpect(xpath("/flat/house/numberOfFloors").doesNotExist());

        mvc.perform(get("/flats/1"))
                .andExpect(status().isOk())
                .andExpect(xpath("/flat/name").string("A"))
                .andExpect(xpath("/flat/creationDate").exists());
    }

    @Test
    void createValidation() throws Exception {
        mvc.perform(post("/flats").queryParam("name", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(xpath("/error/status").string("400"))
                .andExpect(xpath("/error/message").string("Required parameter 'coordinatesX' is missing"));
        mvc.perform(withFlat(post("/flats"), "A", 50, 1, true, 1, "FEW").queryParam("houseName", "H"))
                .andExpect(status().isBadRequest());
        mvc.perform(withFlat(post("/flats"), "A", 50, 1, true, 1, "WRONG"))
                .andExpect(status().isBadRequest());
        mvc.perform(withFlat(post("/flats"), "A", 50, 1, true, 1, "FEW").queryParam("area", "x"))
                .andExpect(status().isBadRequest());
        create("A", 983, 1, true, 1, "FEW")
                .andExpect(status().isUnprocessableContent())
                .andExpect(xpath("/error/error").string("Unprocessable Entity"))
                .andExpect(xpath("/error/message").string("Field 'area' must be between 1 and 982"));
        create(" ", 10, 0, true, 1, "FEW").andExpect(status().isUnprocessableContent());
        mvc.perform(withFlat(post("/flats"), "A", 50, 1, true, 1, "FEW")
                        .queryParam("coordinatesY", "-560"))
                .andExpect(status().isBadRequest()); // параметр передан дважды
    }

    @Test
    void notAcceptable() throws Exception {
        mvc.perform(get("/flats").accept("application/json"))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(xpath("/error/status").string("406"));
    }

    @Test
    void getByIdErrors() throws Exception {
        mvc.perform(get("/flats/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/flats/0")).andExpect(status().isUnprocessableContent());
        mvc.perform(get("/flats/42"))
                .andExpect(status().isNotFound())
                .andExpect(xpath("/error/message").string("Flat with id 42 not found"))
                .andExpect(xpath("/error/path").string("/flats/42"));
    }

    @Test
    void listFilterSortPage() throws Exception {
        create("A", 10, 300, true, 1, "FEW");
        create("B", 20, 100, false, 2, "NONE");
        create("C", 30, 200, true, 3, "ENOUGH");
        create("D", 40, 400, true, 4, "FEW");

        mvc.perform(get("/flats"))
                .andExpect(status().isOk())
                .andExpect(xpath("/flatPage/totalElements").string("4"))
                .andExpect(xpath("/flatPage/totalPages").string("1"))
                .andExpect(xpath("count(/flatPage/flat)").number(4.0));

        mvc.perform(get("/flats").queryParam("balcony", "true").queryParam("sort", "price,asc")
                        .queryParam("pageSize", "2").queryParam("pageNumber", "1"))
                .andExpect(xpath("/flatPage/totalElements").string("3"))
                .andExpect(xpath("/flatPage/totalPages").string("2"))
                .andExpect(xpath("/flatPage/flat[1]/name").string("D"));

        mvc.perform(get("/flats").queryParam("sort", "transport,desc").queryParam("sort", "price,desc"))
                .andExpect(xpath("/flatPage/flat[1]/name").string("B"))
                .andExpect(xpath("/flatPage/flat[2]/name").string("D"));

        mvc.perform(get("/flats").queryParam("areaMin", "15").queryParam("areaMax", "35"))
                .andExpect(xpath("/flatPage/totalElements").string("2"));

        mvc.perform(get("/flats").queryParam("areaMin", "50").queryParam("areaMax", "10"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(xpath("/error/message").string("areaMin must not be greater than areaMax"));
        mvc.perform(get("/flats").queryParam("sort", "unknown,asc")).andExpect(status().isBadRequest());
        mvc.perform(get("/flats").queryParam("pageSize", "0")).andExpect(status().isUnprocessableContent());
        mvc.perform(get("/flats").queryParam("creationDateFrom", "yesterday")).andExpect(status().isBadRequest());
        mvc.perform(get("/flats").queryParam("creationDateFrom", "2000-01-01T00:00:00Z"))
                .andExpect(xpath("/flatPage/totalElements").string("4"));
    }

    @Test
    void updateAndPatch() throws Exception {
        create("A", 10, 300, true, 1, "FEW");
        mvc.perform(withFlat(put("/flats/1"), "B", 20, 500, false, 2, "NONE"))
                .andExpect(status().isCreated())
                .andExpect(xpath("/flat/name").string("B"))
                .andExpect(xpath("/flat/id").string("1"));
        mvc.perform(withFlat(put("/flats/7"), "B", 20, 500, false, 2, "NONE")).andExpect(status().isNotFound());

        mvc.perform(patch("/flats/1")).andExpect(status().isBadRequest());
        mvc.perform(patch("/flats/1").queryParam("price", "777"))
                .andExpect(status().isOk())
                .andExpect(xpath("/flat/price").string("777"))
                .andExpect(xpath("/flat/name").string("B"));
        mvc.perform(patch("/flats/1").queryParam("houseName", "H")).andExpect(status().isBadRequest());
        mvc.perform(patch("/flats/1").queryParam("houseName", "H").queryParam("houseYear", "2000")
                        .queryParam("houseNumberOfFlatsOnFloor", "3").queryParam("houseNumberOfLifts", "2"))
                .andExpect(status().isOk())
                .andExpect(xpath("/flat/house/year").string("2000"));
        mvc.perform(patch("/flats/1").queryParam("houseYear", "2010"))
                .andExpect(status().isOk())
                .andExpect(xpath("/flat/house/year").string("2010"))
                .andExpect(xpath("/flat/house/name").string("H"));
        mvc.perform(patch("/flats/1").queryParam("area", "0")).andExpect(status().isUnprocessableContent());
    }

    @Test
    void deleteAndSpecialOperations() throws Exception {
        mvc.perform(get("/flats/average-number-of-rooms"))
                .andExpect(xpath("/averageNumberOfRoomsResult/average").number(0.0));
        create("A", 10, 300, true, 1, "FEW");
        create("B", 20, 100, false, 2, "NONE");
        create("C", 30, 200, true, 4, "FEW");

        mvc.perform(get("/flats/average-number-of-rooms"))
                .andExpect(status().isOk())
                .andExpect(xpath("/averageNumberOfRoomsResult/average").number(7.0 / 3));

        mvc.perform(patch("/flats/1").queryParam("houseName", "H").queryParam("houseYear", "2000")
                .queryParam("houseNumberOfFlatsOnFloor", "3").queryParam("houseNumberOfLifts", "2"));
        mvc.perform(patch("/flats/2").queryParam("houseName", "H").queryParam("houseYear", "1990")
                .queryParam("houseNumberOfFlatsOnFloor", "3").queryParam("houseNumberOfLifts", "2"));
        mvc.perform(get("/flats/count-by-house-greater-than").queryParam("houseName", "X")
                        .queryParam("houseYear", "1995").queryParam("houseNumberOfFlatsOnFloor", "1")
                        .queryParam("houseNumberOfLifts", "1"))
                .andExpect(status().isOk())
                .andExpect(xpath("/countResult/count").string("1"));
        mvc.perform(get("/flats/count-by-house-greater-than").queryParam("houseName", "X"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/flats/count-by-house-greater-than").queryParam("houseName", "X")
                        .queryParam("houseYear", "0").queryParam("houseNumberOfFlatsOnFloor", "1")
                        .queryParam("houseNumberOfLifts", "1"))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(delete("/flats/by-transport/FEW"))
                .andExpect(status().isOk())
                .andExpect(xpath("/deletionResult/deletedCount").string("2"));
        mvc.perform(delete("/flats/by-transport/bad")).andExpect(status().isBadRequest());

        mvc.perform(delete("/flats/2")).andExpect(status().isNoContent());
        mvc.perform(delete("/flats/2")).andExpect(status().isNotFound());
        mvc.perform(delete("/flats/0")).andExpect(status().isUnprocessableContent());
    }

    @Test
    void unknownUrl() throws Exception {
        mvc.perform(get("/nothing"))
                .andExpect(status().isNotFound())
                .andExpect(xpath("/error/status").string("404"));
    }
}
