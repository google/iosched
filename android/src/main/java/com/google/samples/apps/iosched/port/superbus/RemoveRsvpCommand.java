package com.google.samples.apps.iosched.port.superbus;

import com.google.samples.apps.iosched.port.tasks.BasicIdResult;
import com.google.samples.apps.iosched.port.tasks.RsvpRequests;

import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;

/**
 * Created by kgalligan on 8/17/14.
 */
public class RemoveRsvpCommand extends ModifyRsvpCommand
{
    public RemoveRsvpCommand(Long eventId)
    {
        this.eventId = eventId;
    }

    public RemoveRsvpCommand()
    {
    }

    @Override
    protected BasicIdResult runCommand(RsvpRequests rsvpRequests) throws TransientException, PermanentException
    {
        return rsvpRequests.removeRsvp(eventId);
    }
}
