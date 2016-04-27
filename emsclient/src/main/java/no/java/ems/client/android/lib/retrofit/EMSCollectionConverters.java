package no.java.ems.client.android.lib.retrofit;

import no.java.ems.client.android.model.Attachment;
import no.java.ems.client.android.model.Event;
import no.java.ems.client.android.model.Session;
import no.java.ems.client.android.model.Slot;
import no.java.ems.client.android.model.Speaker;
import no.java.ems.client.android.model.Item;
import no.java.ems.client.android.model.Room;


public class EMSCollectionConverters {

    public static CollectionConverter<Event> events = new CollectionConverter<Event>(new Factory<Event>() {
        @Override
        public Event create(Item item) {
            return new Event(item);
        }
    });

    public static CollectionConverter<Session> sessions = new CollectionConverter<Session>(new Factory<Session>() {
        @Override
        public Session create(Item item) {
            return new Session(item);
        }
    });

    public static CollectionConverter<Speaker> speakers  = new CollectionConverter<Speaker>(new Factory<Speaker>() {
        @Override
        public Speaker create(Item item) {
            return new Speaker(item);
        }
    });

    public static CollectionConverter<Slot> slots  = new CollectionConverter<Slot>(new Factory<Slot>() {
        @Override
        public Slot create(Item item) {
            return new Slot(item);
        }
    });

    public static CollectionConverter<Attachment> attachments  = new CollectionConverter<Attachment>(new Factory<Attachment>() {
        @Override
        public Attachment create(Item item) {
            return new Attachment(item);
        }
    });

    public static CollectionConverter<Room> rooms  = new CollectionConverter<Room>(new Factory<Room>() {
        @Override
        public Room create(Item item) {
            return new Room(item);
        }
    });


}
