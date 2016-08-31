package no.java.ems.client.android.model;

import java.net.URL;
import java.util.List;

public class Collection {

    public String version;
    public URL href;
    public List<Item> items;
    public List<Link> links;
    public List<Query> queries;

    @Override
    public String toString() {
        return "Collection{" +
                "version='" + version + '\'' +
                ", href=" + href +
                ", items=" + items +
                ", links=" + links +
                ", queries=" + queries +
                '}';
    }
}
