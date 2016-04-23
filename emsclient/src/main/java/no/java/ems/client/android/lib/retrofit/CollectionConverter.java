package no.java.ems.client.android.lib.retrofit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.java.ems.client.android.model.EMSCollection;
import no.java.ems.client.android.model.Item;

/**
 *
 * Maps a generic Collection+json response to a typed java List of the collections´ items.
 * @param <T>
 */
public class CollectionConverter<T> {

    private Factory<T> factory;

    public CollectionConverter(Factory<T> factory){
        this.factory = factory;
    }

    public List<T> convert(EMSCollection collection) {

        if (collection == null )
            return Collections.emptyList();

        List<Item> events = collection.collection.items;
        if (events != null) {
            ArrayList<T> result = new ArrayList<T>();
            for (Item item : events) {
                result.add(factory.create(item));
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

}
