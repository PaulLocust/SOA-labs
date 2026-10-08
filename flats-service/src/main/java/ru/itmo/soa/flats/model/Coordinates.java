package ru.itmo.soa.flats.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "coordinates")
@XmlType(propOrder = {"x", "y"})
public class Coordinates {
    private Double x; //Максимальное значение поля: 607, Поле не может быть null
    private Float y; //Значение поля должно быть больше -560, Поле не может быть null

    public Coordinates() {
    }

    public Coordinates(Double x, Float y) {
        this.x = x;
        this.y = y;
    }

    public Coordinates copy() {
        return new Coordinates(x, y);
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }
}
