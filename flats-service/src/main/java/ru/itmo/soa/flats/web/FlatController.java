package ru.itmo.soa.flats.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.soa.flats.dto.AverageNumberOfRoomsResult;
import ru.itmo.soa.flats.dto.CountResult;
import ru.itmo.soa.flats.dto.DeletionResult;
import ru.itmo.soa.flats.dto.FlatPage;
import ru.itmo.soa.flats.model.Flat;
import ru.itmo.soa.flats.model.Transport;
import ru.itmo.soa.flats.service.FlatService;
import ru.itmo.soa.flats.service.FlatSort;

import java.net.URI;
import java.util.List;

/**
 * REST API коллекции квартир (openapi/flats-service.yaml). Все параметры передаются в URL, ответы — XML.
 */
@RestController
@RequestMapping("/flats")
public class FlatController {

    private static final String XML = MediaType.APPLICATION_XML_VALUE;
    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final FlatService service;

    public FlatController(FlatService service) {
        this.service = service;
    }

    @GetMapping(produces = XML)
    public FlatPage getFlats(HttpServletRequest request) {
        QueryParams params = params(request);
        Integer pageNumber = params.intValue("pageNumber");
        Integer pageSize = params.intValue("pageSize");
        List<String> sort = params.all("sort");
        return service.findPage(
                FlatParams.filter(params),
                FlatSort.parse(sort.isEmpty() ? List.of("id,asc") : sort),
                pageNumber == null ? DEFAULT_PAGE_NUMBER : pageNumber,
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
    }

    @PostMapping(produces = XML)
    public ResponseEntity<Flat> createFlat(HttpServletRequest request) {
        Flat flat = service.create(FlatParams.fullInput(params(request)));
        URI location = URI.create(request.getContextPath() + "/flats/" + flat.getId());
        return ResponseEntity.created(location).body(flat);
    }

    @GetMapping(value = "/{id}", produces = XML)
    public Flat getFlatById(@PathVariable("id") String id) {
        return service.findById(QueryParams.parsePathId("id", id));
    }

    @PutMapping(value = "/{id}", produces = XML)
    public ResponseEntity<Flat> updateFlat(@PathVariable("id") String id, HttpServletRequest request) {
        long flatId = QueryParams.parsePathId("id", id);
        Flat flat = service.replace(flatId, FlatParams.fullInput(params(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(flat);
    }

    @PatchMapping(value = "/{id}", produces = XML)
    public Flat patchFlat(@PathVariable("id") String id, HttpServletRequest request) {
        long flatId = QueryParams.parsePathId("id", id);
        return service.patch(flatId, FlatParams.partialInput(params(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlat(@PathVariable("id") String id) {
        service.delete(QueryParams.parsePathId("id", id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/by-transport/{transport}", produces = XML)
    public DeletionResult deleteFlatsByTransport(@PathVariable("transport") String transport) {
        Transport value = QueryParams.parseEnum("transport", transport, Transport.class);
        return new DeletionResult(service.deleteByTransport(value));
    }

    @GetMapping(value = "/average-number-of-rooms", produces = XML)
    public AverageNumberOfRoomsResult getAverageNumberOfRooms() {
        return new AverageNumberOfRoomsResult(service.averageNumberOfRooms());
    }

    @GetMapping(value = "/count-by-house-greater-than", produces = XML)
    public CountResult countFlatsByHouseGreaterThan(HttpServletRequest request) {
        return new CountResult(service.countByHouseGreaterThan(FlatParams.requiredHouse(params(request))));
    }

    private static QueryParams params(HttpServletRequest request) {
        return new QueryParams(request.getParameterMap());
    }
}
