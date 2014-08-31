package com.google.samples.apps.iosched.port.superbus;

import com.google.samples.apps.iosched.port.tasks.BasicIdResult;
import com.google.samples.apps.iosched.port.tasks.RsvpRequests;

import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;

/**
 * Push a new rsvp.
 *
 *
 *
 * Created by kgalligan on 8/17/14.
 */
public class AddRsvpCommand extends ModifyRsvpCommand
{
    public AddRsvpCommand(Long eventId)
    {
        super(eventId);
    }

    public AddRsvpCommand()
    {
    }

    protected BasicIdResult runCommand(RsvpRequests rsvpRequests) throws TransientException, PermanentException
    {
        return rsvpRequests.addRsvp(eventId);
    }
}
