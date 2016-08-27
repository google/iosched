

package no.java.schedule.v2.io.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EMSItem {


    public String getValue(String key) {

        // Todo add lookup map
        for (EMSData emsData : data) {
            if (key.equalsIgnoreCase(emsData.name)) {
                return emsData.value;
            }
        }

        return null;
    }

    public String[] getArray(String key) {

        // Todo add lookup map
        for (EMSData emsData : data) {
            if (key.equalsIgnoreCase(emsData.name)) {
                return emsData.array;
            }
        }

        return null;
    }

    public URI href;

    public EMSData[] data;
    public EMSLink[] links;

    public EMSLink getLink(final String rel) {
        for (EMSLink link : links) {
            if (rel.equalsIgnoreCase(link.rel)) {
                return link;
            }
        }
        return null;
    }

    public List<EMSLink> getLinkList(final String rel) {
        List<EMSLink> relevantLinks = new ArrayList<>();
        for (EMSLink link : links) {
            if (rel.equalsIgnoreCase(link.rel)) {
                relevantLinks.add(link);
            }
        }
        return relevantLinks;
    }

    public String getLinkHref(final String rel) {
        EMSLink link = getLink(rel);
        if (link != null) {
            return link.href;
        } else {
            return null;
        }
    }

    public List<EMSLink> getLinkHrefList(final String rel) {
        List<EMSLink> getAllRelevantHrefList = new ArrayList<>();
        getAllRelevantHrefList.addAll(getLinkList(rel));
        return getAllRelevantHrefList;
    }
}
