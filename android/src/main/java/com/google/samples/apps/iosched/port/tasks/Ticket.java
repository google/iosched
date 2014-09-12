package com.google.samples.apps.iosched.port.tasks;

/**
 * Created by kgalligan on 9/12/14.
 */
public class Ticket
{
    public enum Type
    {
        Gold, Silver, Whammy
    }

    public Long id;
    public Type ticketType;
    public String message;
    public String ticketCode;
}
