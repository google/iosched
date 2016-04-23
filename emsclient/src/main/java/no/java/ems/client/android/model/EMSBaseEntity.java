package no.java.ems.client.android.model;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class EMSBaseEntity {
    public static final String NAME = "name";

    private Item item;
    private URL url;


    public EMSBaseEntity(Item item) {
        this.item = item;
        if (item==null){
            throw new IllegalArgumentException();
        }

        try {
            url = new URL(item.href);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URL getHref()  {
        return url;
    }

    Data getData(String itemName){
        return item.getData(itemName);
    }

    public String getName() {
        return getData(NAME).value;

    }

    public Link getLink(String linkName) {
        return item.getLink(linkName);
    }

    protected String getDataValue(String name) {
        return getDataValue(name, null);
    }

    protected String getDataValue(String name,String defaultValue) {
        Data d = getData(name);
        if (d!=null && d.value!=null) {
            return d.value;
        } else {
            return defaultValue;
        }
    }


    protected URI getLinkHref(String relation) {
        Link link = getLink(relation);
        if (link!=null && link.href!=null){
            return link.href;
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        return "EMSBaseEntity{" +
                "item=" + item +
                ", url=" + url +
                ", href=" + getHref() +
                ", name='" + getName() + '\'' +
                '}';
    }
}
