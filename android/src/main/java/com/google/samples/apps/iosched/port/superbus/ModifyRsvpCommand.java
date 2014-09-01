package com.google.samples.apps.iosched.port.superbus;

import android.content.Context;
import android.util.Log;

import com.google.samples.apps.iosched.gcm.ServerUtilities;
import com.google.samples.apps.iosched.port.tasks.BasicIdResult;
import com.google.samples.apps.iosched.port.tasks.DataHelper;
import com.google.samples.apps.iosched.port.tasks.RsvpRequests;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import co.touchlab.android.threading.eventbus.EventBusExt;
import retrofit.RestAdapter;

/**
 * Created by kgalligan on 8/31/14.
 */
public abstract class ModifyRsvpCommand extends CheckedCommand
{
    protected Long eventId;
    private String errorCode;

    /**
     * Kotlin would handle this multi-constructor crap so much better.
     * @param eventId
     */
    protected ModifyRsvpCommand(Long eventId)
    {
        this();
        this.eventId = eventId;
    }

    protected ModifyRsvpCommand()
    {
        setPriority(HIGHER_PRIORITY);
    }

    /**
     * We may add/remove multiple times in sequence (user presses button multiple times).  Operation is
     * idempotent in any case, so if somehow we get multiple of the same, no problem.
     * @param command
     * @return
     */
    @Override
    public boolean same(Command command) {
        return false;
    }

    @Override
    public String logSummary() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean handlePermanentError(Context context, PermanentException exception) {
        //TODO: Show notification. Also unroll op and notify data.
        if(exception instanceof DataHelper.AppPermanentException)
        {
            errorCode = ((DataHelper.AppPermanentException)exception).getResponseBody();
            EventBusExt.getDefault().post(this);
        }
        return true;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        if(eventId != null)
        {
            RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
            RsvpRequests rsvpRequests = restAdapter.create(RsvpRequests.class);
            runCommand(rsvpRequests);
        }
        else
        {
            throw new PermanentException("Some value is null: "+ eventId);
        }

        ServerUtilities.notifyUserDataChanged(context);
    }

    protected abstract BasicIdResult runCommand(RsvpRequests rsvpRequests) throws TransientException, PermanentException;
}
