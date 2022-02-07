package no.fdk.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static Set<Model> extractRdfModels(Graph graph, Resource rdfType) {
        final Model model = ModelFactory.createModelForGraph(graph);

        final Set<Model> rdfModels = new HashSet<>();
        final StmtIterator stmts = model.listStatements(null, RDF.type, rdfType);
        while ( stmts.hasNext() ) {
            final Model rdfModel = ModelFactory.createDefaultModel();

            Statement stmt = stmts.next();
            rdfDFS(stmt.getSubject(), stmt, rdfModel, rdfType, 0, -1);

            if( rdfType == DCAT.Dataset ) {
                Optional<Resource> optionalRecord = model
                    .listSubjectsWithProperty(FOAF.primaryTopic, stmt.getSubject())
                    .toList()
                    .stream()
                    .filter(r -> r.hasProperty(RDF.type, DCAT.CatalogRecord))
                    .findFirst();
                if(optionalRecord.isPresent()) {
                    Resource record = optionalRecord.get();
                    rdfDFS(record, record.getProperty(FOAF.primaryTopic), rdfModel, rdfType, 0, 1);
                }
            }

            rdfModels.add(rdfModel);
        }

       return rdfModels;
    }

    public static void rdfDFS(RDFNode node, Statement statement, Model model, Resource rdfType, int depth, int maxDepth) {
        if ( !model.contains( statement )) {
            model.add( statement );
            if ( node.isResource() && ((depth < maxDepth) || (maxDepth == -1)) ) {

                if( rdfType == DCAT.Dataset && !statement.getSubject().hasProperty(RDF.type, DCAT.Dataset) ) {
                    StmtIterator stmts = node.asResource().listProperties();
                    while (stmts.hasNext()) {
                        Statement stmt = stmts.next();
                        rdfDFS(stmt.getObject(), stmt, model, rdfType, depth++, maxDepth);
                    }
                }
            }
        }
    }

}
