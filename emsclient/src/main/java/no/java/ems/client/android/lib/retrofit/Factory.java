package no.java.ems.client.android.lib.retrofit;

import no.java.ems.client.android.model.Item;

public abstract class Factory<T> {
    public abstract T create(Item item);
}
