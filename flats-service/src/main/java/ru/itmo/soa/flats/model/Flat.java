package ru.itmo.soa.flats.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Date;

@XmlRootElement(name = "flat")
@XmlType(propOrder = {"id", "name", "coordinates", "creationDate", "area", "price", "balcony",
        "numberOfRooms", "furnish", "view", "transport", "house"})
public class Flat {
    private Long id; //Поле не может быть null, Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; //Поле не может быть null
    private Date creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически
    private long area; //Максимальное значение поля: 982, Значение поля должно быть больше 0
    private long price; //Значение поля должно быть больше 0
    private boolean balcony;
    private long numberOfRooms; //Значение поля должно быть больше 0
    private Furnish furnish; //Поле может быть null
    private View view; //Поле может быть null
    private Transport transport; //Поле не может быть null
    private House house; //Поле может быть null

    public Flat copy() {
        Flat copy = new Flat();
        copy.id = id;
        copy.name = name;
        copy.coordinates = coordinates == null ? null : coordinates.copy();
        copy.creationDate = creationDate == null ? null : new Date(creationDate.getTime());
        copy.area = area;
        copy.price = price;
        copy.balcony = balcony;
        copy.numberOfRooms = numberOfRooms;
        copy.furnish = furnish;
        copy.view = view;
        copy.transport = transport;
        copy.house = house == null ? null : house.copy();
        return copy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public long getArea() {
        return area;
    }

    public void setArea(long area) {
        this.area = area;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public boolean isBalcony() {
        return balcony;
    }

    public void setBalcony(boolean balcony) {
        this.balcony = balcony;
    }

    public long getNumberOfRooms() {
        return numberOfRooms;
    }

    public void setNumberOfRooms(long numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public Furnish getFurnish() {
        return furnish;
    }

    public void setFurnish(Furnish furnish) {
        this.furnish = furnish;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public House getHouse() {
        return house;
    }

    public void setHouse(House house) {
        this.house = house;
    }
}
