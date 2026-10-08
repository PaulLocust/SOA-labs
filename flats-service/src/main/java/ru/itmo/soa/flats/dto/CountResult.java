package ru.itmo.soa.flats.dto;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "countResult")
public class CountResult {
    private long count;

    public CountResult() {
    }

    public CountResult(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
