package no.java.ems.client.android.model;

public class Query extends Link {
    public String prompt;
    //TODO data field describing


    @Override
    public String toString() {
        return "Query{" +
                "prompt='" + prompt + '\'' +
                "} " + super.toString();
    }
}
