package no.java.ems.client.android.lib.retrofit;


import no.java.ems.client.android.model.EMSCollection;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Url;

public interface EMSService {

    // http://amundsen.com/media-types/collection/format/

    public static String EMS_ROOT = "http://javazone.no/ems/server/";

    @GET("/ems/server/")
    Call<EMSCollection> getRootDocument();

    @GET
    Call<EMSCollection> loadCollection(@Url String link);

}
