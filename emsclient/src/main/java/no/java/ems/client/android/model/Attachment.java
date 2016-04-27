package no.java.ems.client.android.model;

public class Attachment extends EMSBaseEntity {

    public static final String SIZE = "size";

    public Attachment(Item item) {
        super(item);
    }


    @Override
    public String toString() {
        return "Attachment{} " + super.toString();
    }

    public String getSize() {
        return getDataValue(SIZE,"0");
    }

    public String getType() {
        return getDataValue("type", "None");
    }
}
