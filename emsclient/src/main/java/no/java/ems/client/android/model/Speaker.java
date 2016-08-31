package no.java.ems.client.android.model;

import java.net.URI;

public class Speaker extends EMSBaseEntity {

    public Speaker(Item item) {
        super(item);
    }


    @Override
    public String toString() {
        return "Speaker{} " + super.toString();
    }

    public String getBio(){
        return getDataValue("bio","");
    }

    public URI getThumbnailHref() {
        return getLinkHref("thumbnail");
    }
}
