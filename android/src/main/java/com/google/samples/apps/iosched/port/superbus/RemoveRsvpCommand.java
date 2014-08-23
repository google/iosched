package com.google.samples.apps.iosched.port.superbus;

import android.content.Context;
import android.util.Log;

import com.google.samples.apps.iosched.port.tasks.BasicIdResult;
import com.google.samples.apps.iosched.port.tasks.DataHelper;
import com.google.samples.apps.iosched.port.tasks.RemoveRsvpRequest;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import retrofit.RestAdapter;

/**
 * Created by kgalligan on 8/17/14.
 */
public class RemoveRsvpCommand extends CheckedCommand
{
    private Long eventId;

    public RemoveRsvpCommand(Long eventId)
    {
        this.eventId = eventId;
    }

    public RemoveRsvpCommand()
    {
    }

    @Override
    public boolean handlePermanentError(Context context, PermanentException exception) {
        //TODO: need logging strategy
        return true;
    }

    @Override
    public String logSummary() {
        return null;
    }

    @Override
    public boolean same(Command command) {
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
        RemoveRsvpRequest removeRsvp = restAdapter.create(RemoveRsvpRequest.class);

        if(eventId != null)
        {
            BasicIdResult basicIdResult = removeRsvp.removeRsvp(eventId);
            Log.w("asdf", "Result id: " + basicIdResult.id);
        }
        else
        {
            throw new PermanentException("Some value is null: "+ eventId);
        }
    }
}
