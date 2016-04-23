package no.java.ems.client.android.model;

public class Room extends EMSBaseEntity {

    //TODO - fix ems returns 404 for room links.

    public Room(Item item) {
        super(item);
    }


    @Override
    public String toString() {
        return "Room{} " + super.toString();
    }


}
