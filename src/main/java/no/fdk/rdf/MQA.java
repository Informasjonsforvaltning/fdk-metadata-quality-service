package no.fdk.rdf;

public class MQA {
    public static final String NAME = "MQA";
    public static final String NS = "http://data.norge.no/mqa#";
    public static final String PREFIX = "mqa";

    private MQA() {}

    public static String uri(String name) {
        return NS + name;
    }
}
