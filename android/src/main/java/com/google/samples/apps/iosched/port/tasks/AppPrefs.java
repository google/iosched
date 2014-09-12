package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.net.URI;

/**
 * Created by kgalligan on 8/17/14.
 */
public class AppPrefs
{
    public static final String USER_UUID = "USER_UUID";
    public static final String USER_ID = "USER_ID";
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String OFFER_SHOWN = "OFFER_SHOWN";
    public static final String OFFER_COUNTDOWN = "OFFER_COUNTDOWN";
    public static final String GCM_REGISTRATION_KEY = "GCM_REGISTRATION_KEY";
    public static final String GCM_REGISTRATION_ID = "GCM_REGISTRATION_ID";
    public static final String GCM_APP_VERSION = "GCM_APP_VERSION";
    public static final String GCM_REGISTRATION_TS = "GCM_REGISTRATION_TS";

    public static final String TICKET_STATUS = "TICKET_STATUS";

    public static final int NO_VALUE = -1;
    public static final int DEFAULT_OFFER_COUNTDOWN = 5;

    private static AppPrefs instance;

    private SharedPreferences prefs;

    public static synchronized AppPrefs getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new AppPrefs();
            instance.prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE);
        }

        return instance;
    }

    public boolean isLoggedIn()
    {
        return !TextUtils.isEmpty(getUserUuid());
    }

    public String getUserUuid()
    {
        return prefs.getString(USER_UUID, null);
    }

    public void setUserUuid(String uuid)
    {
        prefs.edit().putString(USER_UUID, uuid).apply();
    }

    public int getOfferCountdown()
    {
        int countdown = prefs.getInt(OFFER_COUNTDOWN, -1);
        if(countdown == -1)
        {
            resetOfferCountdown();
            countdown = DEFAULT_OFFER_COUNTDOWN;
        }
        return countdown;
    }

    public void resetOfferCountdown()
    {
        prefs.edit().putInt(OFFER_COUNTDOWN, DEFAULT_OFFER_COUNTDOWN).apply();
    }

    public void minusOfferCountdown()
    {
        if(!isLoggedIn())
            return;
        int offerCountdown = getOfferCountdown();
        prefs.edit().putInt(OFFER_COUNTDOWN, offerCountdown - 1).apply();
    }

    public boolean isUserRegistered()
    {
        return prefs.getBoolean(USER_REGISTERED, false);
    }

    public void setUserRegistered(boolean registered)
    {
        prefs.edit().putBoolean(USER_REGISTERED, registered).apply();
    }

    public boolean isOfferShown()
    {
        return prefs.getBoolean(OFFER_SHOWN, false);
    }

    public void setOfferShown(boolean registered)
    {
        prefs.edit().putBoolean(OFFER_SHOWN, registered).apply();
    }

    public String getTicketStatus()
    {
        return prefs.getString(OFFER_SHOWN, null);
    }

    public void setTicketStatus(String ticketJson)
    {
        prefs.edit().putString(TICKET_STATUS, ticketJson).apply();
    }

    public Ticket getTicket()
    {
        String ticketStatus = getTicketStatus();
        return getTicket(ticketStatus);
    }

    public static Ticket getTicket(String ticketStatus)
    {
        return new Gson().fromJson(ticketStatus, Ticket.class);
    }

    public Long getUserId()
    {
        long id = prefs.getLong(USER_ID, -1);
        return id == -1 ? null : id;
    }

    public void setUserId(Long id)
    {
        prefs.edit().putLong(USER_ID, id).apply();
    }

    public String getGcmRegistrationKey()
    {
        return prefs.getString(GCM_REGISTRATION_KEY, "");
    }
    public String getGcmRegistrationId()
    {
        return prefs.getString(GCM_REGISTRATION_ID, "");
    }

    public void setGcmRegistrationId(String regId, String gcmKey, int appVersion, long regTs)
    {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(GCM_REGISTRATION_ID, regId);
        edit.putString(GCM_REGISTRATION_KEY, gcmKey);
        edit.putInt(GCM_APP_VERSION, appVersion);
        edit.putLong(GCM_REGISTRATION_TS, regTs);
        edit.apply();
    }

    public void clearGcmRegistrationId()
    {
        prefs.edit().remove(GCM_REGISTRATION_ID).apply();
    }

    public long getGcmRegistrationTs()
    {
        return prefs.getLong(GCM_REGISTRATION_TS, 0);
    }

    public int getGcmAppVersion()
    {
        return prefs.getInt(GCM_APP_VERSION, NO_VALUE);
    }
}
