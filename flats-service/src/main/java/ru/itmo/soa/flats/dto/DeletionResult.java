package ru.itmo.soa.flats.dto;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "deletionResult")
public class DeletionResult {
    private long deletedCount;

    public DeletionResult() {
    }

    public DeletionResult(long deletedCount) {
        this.deletedCount = deletedCount;
    }

    public long getDeletedCount() {
        return deletedCount;
    }
}
