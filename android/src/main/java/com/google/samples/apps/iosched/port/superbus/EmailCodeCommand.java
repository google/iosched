package com.google.samples.apps.iosched.port.superbus;

import android.content.Context;

import com.google.samples.apps.iosched.port.tasks.DataHelper;
import com.google.samples.apps.iosched.port.tasks.RsvpRequests;

import org.jetbrains.annotations.NotNull;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;

/**
 * Created by kgalligan on 9/8/14.
 */
public class EmailCodeCommand extends CheckedCommand
{
    @Override
    public boolean handlePermanentError(@NotNull Context context, @NotNull PermanentException exception)
    {
        return true;
    }

    @Override
    public String logSummary()
    {
        return EmailCodeCommand.class.getSimpleName();
    }

    @Override
    public boolean same(@NotNull Command command)
    {
        return command instanceof EmailCodeCommand;
    }

    @Override
    public void callCommand(@NotNull Context context) throws TransientException, PermanentException
    {
        RsvpRequests rsvpRequests = DataHelper.makeRequestAdapter(context).create(RsvpRequests.class);
        rsvpRequests.sendDiscountEmail();
    }
}
