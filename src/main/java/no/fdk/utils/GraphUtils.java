package no.fdk.utils;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.sparql.graph.GraphFactory;

import java.io.StringReader;
import java.io.StringWriter;

public class GraphUtils {

    public static Graph stringToGraph(String string, Lang language) {
        Graph graph = GraphFactory.createDefaultGraph();
        RDFParserBuilder.create().source(new StringReader(string)).lang(language).parse(graph);
        return graph;
    }

    public static String graphToString(Graph graph, Lang language) {
        StringWriter stringWriter = new StringWriter();
        RDFDataMgr.write(stringWriter, graph, language);
        return stringWriter.toString();
    }

}
