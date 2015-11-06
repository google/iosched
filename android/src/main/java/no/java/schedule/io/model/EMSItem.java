

package no.java.schedule.io.model;

import java.net.URI;

public class EMSItem {


  public String getValue(String key){

    // Todo add lookup map
    for (EMSData emsData : data) {
      if (key.equalsIgnoreCase(emsData.name)){
        return emsData.value;
      }
    }

    return null;
  }

  public String[] getArray(String key){

     // Todo add lookup map
     for (EMSData emsData : data) {
       if (key.equalsIgnoreCase(emsData.name)){
         return emsData.array;
       }
     }

     return null;
   }

  public URI href;

  public EMSData[] data;
  public EMSLinks[] links;

  public EMSLinks getLink(final String rel) {
    for (EMSLinks link : links) {
      if(rel.equalsIgnoreCase(link.rel)){
        return link;
      }
    }

    return null;
  }

  public String getLinkHref(final String rel) {
    EMSLinks link = getLink(rel);
    if (link!=null){
      return link.href;
    } else {
      return null;
    }

  }
}
