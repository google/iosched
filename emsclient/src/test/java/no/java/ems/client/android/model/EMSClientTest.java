package no.java.ems.client.android.model;

import com.squareup.okhttp.OkHttpClient;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.java.ems.client.android.api.EMSClient;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class EMSClientTest {

    private static String JAVA_ZONE_2015 = "JavaZone 2015";
    private EMSClient client;

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        RetrofitTestUtilities.enableCache(okHttpClient);
        RetrofitTestUtilities.enableForceCache(okHttpClient);

        client = new EMSClient(okHttpClient);
        //client = new EMSClient(createDebugOkHttpClient()); //Uncomment to enable request logging
    }

    @Test
    public void getEvent() throws IOException {
        List<Event> events = client.events();

        for (Event e : events) {
            System.out.println(e.getName());
        }

        assertNotNull(events);
    }

    @Test
    public void testJavaZone2015() throws IOException {

        Event jz15 = client.getEventByName(JAVA_ZONE_2015);
        assertNotNull(jz15);
        assertEquals(jz15.getName(), JAVA_ZONE_2015);
    }

    @Test
    public void testJavaZone2015Sessions() throws IOException {
        List<Session> sessions = getJavaZone2015Sessions();
        assertNotNull(sessions);
        assertEquals(164,sessions.size());
    }

    private List<Session> getJavaZone2015Sessions() throws IOException {
        Event jz15 = client.getEventByName(JAVA_ZONE_2015);
        return client.getSessionList(jz15.getSessionHref());
    }

    /**
     *
     * Iterates over all sessions registered for Javazone 2015 and validates they parse
     *
     * @throws IOException
     */
    @Test
    public void testSessions() throws IOException {

        Set<URI> speakerUris = new HashSet<>();

        for (Session session : getJavaZone2015Sessions()) {
            assertSessionValid(session);
            //assertRoomValid(session.getRoomHref()); //TODO Room refs return 404
            assertSlotValid(session.getSlotHref());
            assertAttachmentsValid(session.getAttachmentsHref());
            speakerUris.add(session.getSpeakersHref());
        }

        for (URI speakerUri : speakerUris) {
            assertSessionSpeaker(speakerUri);
        }
    }

    private void assertAttachmentsValid(URI attachmentsHref) throws IOException {
        List<Attachment> attachmentList = client.getAttachments(attachmentsHref);

        for (Attachment a : attachmentList) {
            assertAttachmentValid(a);
        }
    }

    private void assertAttachmentValid(Attachment a) {
        assertBaseEntity(a);
        assertNotNull(a.getSize());
        assertNotNull(a.getType());


    }

    private void assertSlotValid(URI slotHref) throws IOException {
        List<Slot> slotList = client.getSlots(slotHref);
        assertEquals(1, slotList.size());

        Slot slot = slotList.get(0);
        // assertBaseEntity(slot); // TODO Slot has no name
        assertNotNull(slot.getStart());

    }

    private void assertBaseEntity(EMSBaseEntity slot) {
        assertNotNull(slot.getName());
        assertNotNull(slot.getHref());
    }

    private void assertRoomValid(URI roomHref) throws IOException {
        List<Room> roomList = client.getRooms(roomHref);
        assertEquals(1, roomList.size());

        Room room = roomList.get(0);
        assertBaseEntity(room);
    }

    private void assertSessionValid(Session session)  {

        String msg = "Error validating "+session.getHref();

        // Attributes
        assertNotNull(msg, session.getTitle());
        assertNotNull(msg, session.getSummary());
        assertNotNull(msg, session.getBody());
        assertNotNull(msg, session.getOutline());
        assertNotNull(msg, session.getAudience());
        assertNotNull(msg, session.getEquipment());
        assertNotNull(msg, session.getKeywords());
        assertNotNull(msg, session.getPublished());
        assertNotNull(msg, session.getLanguage());
        assertNotNull(msg, session.getFormat());
        assertNotNull(msg, session.getState());
        assertNotNull(msg, session.getLevel());

        // Link hrefs

        assertNotNull(msg, session.getAttachmentsHref());
        assertNotNull(msg, session.getSpeakersHref());
        assertNotNull(msg, session.getRoomHref());
        assertNotNull(msg, session.getSlotHref());
        assertNotNull(msg, session.getSpeakersHref());
        //assertNotNull(msg,session.getVideoHref());

    }


    private void assertSessionSpeaker(URI speakersHref) throws IOException {
        List<Speaker> speakers = client.getSpeakers(speakersHref);
        for (Speaker s : speakers) {
            String msg = "Error validating "+s.getHref();

            // Attributes
            assertNotNull(msg,s.getName());
            assertNotNull(msg,s.getBio());

            // Links
            assertNotNull(msg,s.getThumbnailHref());
        }


    }

}