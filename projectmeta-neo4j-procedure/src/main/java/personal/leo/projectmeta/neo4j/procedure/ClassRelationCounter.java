package personal.leo.projectmeta.neo4j.procedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 * This is an example showing how you could expose Neo4j's full text indexes as
 * two procedures - one for updating indexes, and one for querying by label and
 * the lucene query language.
 */
public class ClassRelationCounter {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    private static final char RELATION_SEPARATOR = 0;
    private static final String NAME = "name";
    private static final String APP = "app";

    @Procedure(value = "ClassRelationCounter.count")
    public Stream<Count> count(
        @Name(APP) String app
    ) {
        final String cql1 = String.format(
            "MATCH(:App{name:'%s'})"
                + "<-[:BELONG_TO]-(:Module)"
                + "<-[:BELONG_TO]-(:Package)"
                + "<-[:BELONG_TO]-(c:Class) "
                + "RETURN c",
            app
        );
        System.out.println(cql1);
        final Node[] nodes = db.execute(
            cql1,
            new HashMap<>()
        ).stream()
            .map(entry -> (Node)entry.get("c"))
            .toArray(Node[]::new);

        final ArrayList<Count> counts = new ArrayList<>();

        if (ArrayUtils.isNotEmpty(nodes)) {
            long havingDirectRelationsCount = 0L;
            long havingNoDirectRelationsCount = 0L;

            final String cql2 = String.format(
                "MATCH (a:App {name: '%s'})\n"
                    + "WITH a\n"
                    + "MATCH p = (c1:Class)-[:CALL]-(c2:Class)\n"
                    + "  WHERE (a)<-[:BELONG_TO]-(:Module)<-[:BELONG_TO]-(:Package)<-[:BELONG_TO]-(c1)\n"
                    + "  AND (a)<-[:BELONG_TO]-(:Module)<-[:BELONG_TO]-(:Package)<-[:BELONG_TO]-(c2)\n"
                    + "RETURN p",
                app
            );
            System.out.println(cql2);
            final Set<String> relations = db.execute(
                cql2,
                new HashMap<>()
            ).stream()
                .map(entry -> {
                    final Path path = (Path)entry.get("p");
                    return buildRelation(path.startNode(), path.endNode());
                })
                .collect(Collectors.toSet());

            for (int fromIndex = 0; fromIndex < nodes.length; fromIndex++) {
                final Node startNode = nodes[fromIndex];
                for (int toIndex = fromIndex + 1; toIndex < nodes.length; toIndex++) {
                    final Node endNode = nodes[toIndex];
                    final String relation = buildRelation(startNode, endNode);
                    if (relations.contains(relation)) {
                        havingDirectRelationsCount++;
                    } else {
                        havingNoDirectRelationsCount++;
                    }
                }
            }

            counts.add(
                new Count(
                    havingDirectRelationsCount,
                    havingNoDirectRelationsCount
                )
            );
        }

        return counts.stream();

    }

    private String buildRelation(Node startNode, Node endNode) {
        String startNodeName = (String)startNode.getProperty(NAME);
        String endNodeName = (String)endNode.getProperty(NAME);
        if (startNodeName.compareTo(endNodeName) > 0) {
            return endNodeName + RELATION_SEPARATOR + startNodeName;
        } else {
            return startNodeName + RELATION_SEPARATOR + endNodeName;
        }
    }

    public static class Count {
        public long havingDirectRelations;
        public long havingNoDirectRelations;

        public Count(long havingDirectRelations, long havingNoDirectRelations) {
            this.havingDirectRelations = havingDirectRelations;
            this.havingNoDirectRelations = havingNoDirectRelations;
        }
    }

}
