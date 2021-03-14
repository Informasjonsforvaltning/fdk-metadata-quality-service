package no.fdk.rdf;

public class FDK {
    public static final String NAME = "FDK";
    public static final String NS = "http://data.norge.no/fdk#";
    public static final String PREFIX = "fdk";

    private FDK() {}

    public static String uri(String name) {
        return NS + name;
    }
}
