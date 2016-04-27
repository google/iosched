package no.java.ems.client.android.model;

import android.support.annotation.Nullable;

import java.net.URI;

public class Slot extends EMSBaseEntity {


    public static final String SLOT_COLLECTION = "slot collection";
    public static final String DURATION = "duration";
    public static final String END = "end";
    public static final String START = "start";

    public Slot(Item item) {
        super(item);
    }

    @Override
    public String toString() {
        return "Slot{} " + super.toString();
    }

    @Nullable
    public String getStart() {
        return getDataValue(START);
    }

    @Nullable
    public String getEnd() {
        return getDataValue(END);
    }

    @Nullable
    public String getDuration() {
        return getDataValue(DURATION);
    }

    public URI getChildreSlotsHref(){
        return getLinkHref(SLOT_COLLECTION);
    }
}
