package com.google.samples.apps.iosched.port.superbus;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.PersistedApplication;
import co.touchlab.android.superbus.appsupport.CancellableCommand;
import co.touchlab.android.superbus.appsupport.CommandBusHelper;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.SuperbusProcessException;
import co.touchlab.android.superbus.errorcontrol.TransientException;

/**
 * Created by kgalligan on 9/1/14.
 */
public abstract class CancellableCheckedCommand extends CheckedCommand
{
    public static final String CANCEL_GLOBAL_UPDATE = "CANCEL_GLOBAL_UPDATE";
    transient private AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public final void callCommand(@NotNull Context context) throws TransientException, PermanentException
    {
        //If we're cancelled, we're done
        if(cancelled.get())
            return;

        setupData(context);

        PersistedApplication persistedApplication = (PersistedApplication) context.getApplicationContext();
        PostCancelRunnable postCancelRunnable = new PostCancelRunnable(context);
        persistedApplication.getConfig().getPersistenceProvider().runInTransaction(postCancelRunnable);
        if(postCancelRunnable.exception != null)
            throw postCancelRunnable.exception;
    }

    public void setCancelled(boolean cancel)
    {
        cancelled.set(cancel);
    }

    public boolean isCancelled()
    {
        return cancelled.get();
    }

    class PostCancelRunnable implements Runnable
    {
        Context context;
        PermanentException exception;

        PostCancelRunnable(@NotNull Context context)
        {
            this.context = context;
        }

        @Override
        public void run()
        {
            if(cancelled.get())
                return;

            try
            {
                commitData(context);
            }
            catch (PermanentException e)
            {
                exception = e;
            }
        }
    }

    @Override
    public void onRuntimeMessage(@NotNull Context context, String message)
    {
        if(message.equals(CANCEL_GLOBAL_UPDATE))
            setCancelled(true);
    }

    public static void cancelGlobalUpdates(Context context)
    {
        CommandBusHelper.sendMessage(context, CANCEL_GLOBAL_UPDATE);
    }

    protected abstract void setupData(@NotNull Context context)throws TransientException, PermanentException;

    protected abstract void commitData(@NotNull Context context)throws PermanentException;
}
