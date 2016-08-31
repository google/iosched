package no.java.ems.client.android.model;

import java.util.List;

public class Item {

    public String href;
    public List<Data> data;
    public List<Link> links;

    public Data getData(String name) {
        if (name!=null){
            for (Data  d: data) {
                if (name.equals(d.name)){
                    return d;
                }
            }
        }
        return null;
    }

    public Link getLink(String relation) {
        if (relation!=null){
            for (Link  l: links) {
                if (relation.equals(l.rel)){
                    return l;
                }
            }
        }
        return null;
    }
}
