package no.java.ems.client.android.api;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import no.java.ems.client.android.lib.retrofit.EMSCollectionConverters;
import no.java.ems.client.android.lib.retrofit.EMSService;
import no.java.ems.client.android.model.Attachment;
import no.java.ems.client.android.model.Collection;
import no.java.ems.client.android.model.EMSCollection;
import no.java.ems.client.android.model.Event;
import no.java.ems.client.android.model.Link;
import no.java.ems.client.android.model.Room;
import no.java.ems.client.android.model.Session;
import no.java.ems.client.android.model.Slot;
import no.java.ems.client.android.model.Speaker;
import retrofit.Retrofit;

import static no.java.ems.client.android.lib.retrofit.EMSCollectionConverters.speakers;
import static no.java.ems.client.android.lib.retrofit.RetrofitUtillity.createRetrofit;

public class EMSClient {

    public static final String NAME = "name";
    public static final String SESSION_COLLECTION = "session collection";
    private final EMSService ems;

    public EMSClient() {
        this(createOkHttpClient());
    }

    public EMSClient(OkHttpClient client){
        Retrofit retrofit = createRetrofit(client);
        ems = retrofit.create(EMSService.class);
    }


    @NonNull
    private static OkHttpClient createOkHttpClient() {
        return new OkHttpClient();
    }

    @Nullable
    private EMSCollection eventCollection() throws IOException {
        // TODO: 07.11.2015 Implement root document
        List<Link> links = loadRoot().links;
        for (Link link : links) {
         if ("event collection".equals(link.rel)){
             return loadCollection(link.href);
         }
        }
        return null;
    }

    private Collection loadRoot() throws java.io.IOException {
        return ems.getRootDocument().execute().body().collection;
    }

    public Event getEvent(URL id) throws IOException {
        if (id!=null) {
            for (Event e : events()) {
                if (id.equals(e.getHref())) {
                    return e;
                }
            }
        }
        return null;
    }

    public Event getEventByName(String name) throws IOException {
        if (name!=null) {
            for (Event e : events()) {
                if (name.equals(e.getName())) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     *
     * @return JavaZone events
     */
    public List<Event> events() throws IOException {
        return EMSCollectionConverters.events.convert(eventCollection());
    }

    public List<Session> getSessionList(URI uri) throws IOException {
        return EMSCollectionConverters.sessions.convert(loadCollection(uri));
    }

    public List<Speaker> getSpeakers(URI uri) throws IOException {
        return speakers.convert(loadCollection(uri));
    }

    public List<Slot> getSlots(URI uri) throws IOException {
        return EMSCollectionConverters.slots.convert(loadCollection(uri));
    }

    public List<Attachment> getAttachments(URI uri) throws IOException {
        return EMSCollectionConverters.attachments.convert(loadCollection(uri));
    }

    public List<Room> getRooms(URI uri) throws IOException {
        return EMSCollectionConverters.rooms.convert(loadCollection(uri));
    }


    private EMSCollection loadCollection(URI href) throws IOException {
        return ems.loadCollection(href.toString()).execute().body();
    }

}
