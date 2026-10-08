package ru.itmo.soa.flats.service;

import org.springframework.stereotype.Service;
import ru.itmo.soa.flats.dto.FlatPage;
import ru.itmo.soa.flats.exception.ApiException;
import ru.itmo.soa.flats.model.Coordinates;
import ru.itmo.soa.flats.model.Flat;
import ru.itmo.soa.flats.model.House;
import ru.itmo.soa.flats.model.Transport;
import ru.itmo.soa.flats.repository.FlatRepository;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class FlatService {

    public static final int MAX_PAGE_SIZE = 200;

    private final FlatRepository repository;

    public FlatService(FlatRepository repository) {
        this.repository = repository;
    }

    public FlatPage findPage(FlatFilter filter, Comparator<Flat> order, int pageNumber, int pageSize) {
        if (pageNumber < 0) {
            throw ApiException.unprocessable("Parameter 'pageNumber' must not be negative");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.unprocessable("Parameter 'pageSize' must be between 1 and " + MAX_PAGE_SIZE);
        }
        filter.validateRanges();

        List<Flat> matching = repository.findAll().stream().filter(filter).sorted(order).toList();
        long total = matching.size();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        long from = (long) pageNumber * pageSize;
        List<Flat> items = from >= total
                ? List.of()
                : matching.subList((int) from, (int) Math.min(total, from + pageSize));
        return new FlatPage(pageNumber, pageSize, total, totalPages, items);
    }

    public Flat findById(long id) {
        FlatValidator.validateId(id);
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    /** Создание: обязательные параметры и полнота дома уже проверены при разборе запроса. */
    public synchronized Flat create(FlatInput input) {
        FlatValidator.validate(input);
        Flat flat = new Flat();
        flat.setId(repository.nextId());
        flat.setCreationDate(new Date());
        applyAll(flat, input);
        repository.save(flat);
        return flat;
    }

    /** Полная замена всех полей, кроме id и creationDate. */
    public synchronized Flat replace(long id, FlatInput input) {
        FlatValidator.validateId(id);
        FlatValidator.validate(input);
        Flat flat = repository.findById(id).orElseThrow(() -> notFound(id));
        applyAll(flat, input);
        repository.save(flat);
        return flat;
    }

    /** Частичное обновление: меняются только переданные поля. */
    public synchronized Flat patch(long id, FlatInput input) {
        FlatValidator.validateId(id);
        Flat flat = repository.findById(id).orElseThrow(() -> notFound(id));
        HouseInput houseInput = input.house();
        if (flat.getHouse() == null && !houseInput.isEmpty() && !houseInput.isComplete()) {
            throw ApiException.badRequest("Flat with id " + id + " has no house: to add it, parameters "
                    + HouseInput.REQUIRED_PARAMS + " must be specified");
        }
        FlatValidator.validate(input);

        if (input.name() != null) {
            flat.setName(input.name());
        }
        if (input.coordinatesX() != null) {
            flat.getCoordinates().setX(input.coordinatesX());
        }
        if (input.coordinatesY() != null) {
            flat.getCoordinates().setY(input.coordinatesY());
        }
        if (input.area() != null) {
            flat.setArea(input.area());
        }
        if (input.price() != null) {
            flat.setPrice(input.price());
        }
        if (input.balcony() != null) {
            flat.setBalcony(input.balcony());
        }
        if (input.numberOfRooms() != null) {
            flat.setNumberOfRooms(input.numberOfRooms());
        }
        if (input.furnish() != null) {
            flat.setFurnish(input.furnish());
        }
        if (input.view() != null) {
            flat.setView(input.view());
        }
        if (input.transport() != null) {
            flat.setTransport(input.transport());
        }
        if (!houseInput.isEmpty()) {
            if (flat.getHouse() == null) {
                flat.setHouse(houseInput.toHouse());
            } else {
                houseInput.applyTo(flat.getHouse());
            }
        }
        repository.save(flat);
        return flat;
    }

    public synchronized void delete(long id) {
        FlatValidator.validateId(id);
        if (!repository.deleteById(id)) {
            throw notFound(id);
        }
    }

    public synchronized long deleteByTransport(Transport transport) {
        return repository.deleteIf(flat -> flat.getTransport() == transport);
    }

    public double averageNumberOfRooms() {
        return repository.findAll().stream().mapToLong(Flat::getNumberOfRooms).average().orElse(0);
    }

    public long countByHouseGreaterThan(HouseInput houseInput) {
        FlatValidator.validate(houseInput);
        House house = houseInput.toHouse();
        return repository.findAll().stream()
                .filter(flat -> flat.getHouse() != null)
                .filter(flat -> HouseComparator.INSTANCE.compare(flat.getHouse(), house) > 0)
                .count();
    }

    private static void applyAll(Flat flat, FlatInput input) {
        flat.setName(input.name());
        flat.setCoordinates(new Coordinates(input.coordinatesX(), input.coordinatesY()));
        flat.setArea(input.area());
        flat.setPrice(input.price());
        flat.setBalcony(input.balcony());
        flat.setNumberOfRooms(input.numberOfRooms());
        flat.setFurnish(input.furnish());
        flat.setView(input.view());
        flat.setTransport(input.transport());
        flat.setHouse(input.house().isEmpty() ? null : input.house().toHouse());
    }

    private static ApiException notFound(long id) {
        return ApiException.notFound("Flat with id " + id + " not found");
    }
}
