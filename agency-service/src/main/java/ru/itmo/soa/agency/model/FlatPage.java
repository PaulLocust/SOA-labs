package ru.itmo.soa.agency.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/**
 * Схема {@code FlatPage} — ответ GET /flats первого сервиса.
 */
@XmlRootElement(name = "flatPage")
@XmlType(propOrder = {"pageNumber", "pageSize", "totalElements", "totalPages", "items"})
public class FlatPage {
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    @XmlElement(name = "flat")
    private List<Flat> items = new ArrayList<>();

    public long getTotalElements() {
        return totalElements;
    }

    public List<Flat> getItems() {
        return items;
    }
}
