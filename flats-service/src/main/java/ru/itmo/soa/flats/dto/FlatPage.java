package ru.itmo.soa.flats.dto;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import ru.itmo.soa.flats.model.Flat;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "flatPage")
@XmlType(propOrder = {"pageNumber", "pageSize", "totalElements", "totalPages", "items"})
public class FlatPage {
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    /** Элементы списка не оборачиваются: {@code <flatPage>...<flat/><flat/></flatPage>}. */
    @XmlElement(name = "flat")
    private List<Flat> items = new ArrayList<>();

    public FlatPage() {
    }

    public FlatPage(int pageNumber, int pageSize, long totalElements, int totalPages, List<Flat> items) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.items = items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<Flat> getItems() {
        return items;
    }
}
