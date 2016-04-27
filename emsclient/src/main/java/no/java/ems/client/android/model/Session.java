package no.java.ems.client.android.model;

import android.support.annotation.Nullable;

import java.net.URI;

public class Session extends EMSBaseEntity {

    public static final String TITLE = "title";
    public static final String SUMMARY = "summary";
    public static final String BODY = "body";
    public static final String OUTLINE = "outline";
    public static final String AUDIENCE = "audience";
    public static final String EQUIPMENT = "equipment";
    public static final String KEYWORDS = "keywords";
    public static final String PUBLISHED = "published";
    public static final String LANGUAGE = "language";
    public static final String FORMAT = "format";
    public static final String STATE = "state";
    public static final String LEVEL = "level";
    public static final String DEFAULT_VALUE = "";
    public static final String DEFAULT_LANGUAGE = "no";
    public static final String SESSION_COLLECTION = "session collection";


    public Session(Item item) {
        super(item);
    }


    /**
     *
     *  Attributes
     *
     **/

    public String getTitle(){
        return getDataValue(TITLE);
    }

    public String getSummary(){
        return getDataValue(SUMMARY);
    }
    public String getBody(){
        return getDataValue(BODY);
    }

    public String getOutline(){
        return getDataValue(OUTLINE, DEFAULT_VALUE);
    }

    public String getAudience(){
        return getDataValue(AUDIENCE);
    }

    public String getEquipment(){
        return getDataValue(EQUIPMENT, DEFAULT_VALUE);
    }

    public String getKeywords(){
        return getDataValue(KEYWORDS, DEFAULT_VALUE);
    }

    public String getPublished(){
        return getDataValue(PUBLISHED);
    }

    public String getLanguage(){
        return getDataValue(LANGUAGE, DEFAULT_LANGUAGE);
    }

    public String getFormat(){
        return getDataValue(FORMAT);
    }

    public String getState(){
        return getDataValue(STATE);
    }

    public String getLevel(){
        return getDataValue(LEVEL);
    }

    /**
     *
     * Links
     *
     */

    public URI getAttachmentsHref(){
        return getLinkHref("attachment collection");
    }

    @Nullable
    public URI getVideoHref(){
        return getLinkHref("alternate video");
    }

    public URI getRoomHref(){
        return getLinkHref("room item");
    }

    public URI getSlotHref(){
        return getLinkHref("slot item");
    }

    public URI getSpeakersHref(){
        return getLinkHref("speaker item");
    }



    @Override
    public String toString() {
        return "Session{" +
                "title='" + getTitle() + '\'' +
                ", summary='" + getSummary() + '\'' +
                ", body='" + getBody() + '\'' +
                ", outline='" + getOutline() + '\'' +
                ", audience='" + getAudience() + '\'' +
                ", equipment='" + getEquipment() + '\'' +
                ", keywords='" + getKeywords() + '\'' +
                ", published='" + getPublished() + '\'' +
                ", language='" + getLanguage() + '\'' +
                ", format='" + getFormat() + '\'' +
                ", state='" + getState() + '\'' +
                ", level='" + getLevel() + '\'' +
                "} " + super.toString();
    }
}
