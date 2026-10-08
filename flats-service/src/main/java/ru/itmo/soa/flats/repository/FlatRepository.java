package ru.itmo.soa.flats.repository;

import org.springframework.stereotype.Repository;
import ru.itmo.soa.flats.model.Flat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Хранилище коллекции квартир в памяти. Наружу отдаются только копии объектов,
 * поэтому изменение возвращённой квартиры не меняет коллекцию.
 */
@Repository
public class FlatRepository {

    private final Map<Long, Flat> flats = new TreeMap<>();
    private long nextId = 1;

    public synchronized long nextId() {
        return nextId++;
    }

    public synchronized void save(Flat flat) {
        flats.put(flat.getId(), flat.copy());
    }

    public synchronized Optional<Flat> findById(long id) {
        return Optional.ofNullable(flats.get(id)).map(Flat::copy);
    }

    public synchronized List<Flat> findAll() {
        return flats.values().stream().map(Flat::copy).toList();
    }

    public synchronized boolean deleteById(long id) {
        return flats.remove(id) != null;
    }

    public synchronized long deleteIf(Predicate<Flat> predicate) {
        int before = flats.size();
        flats.values().removeIf(predicate);
        return before - flats.size();
    }
}
