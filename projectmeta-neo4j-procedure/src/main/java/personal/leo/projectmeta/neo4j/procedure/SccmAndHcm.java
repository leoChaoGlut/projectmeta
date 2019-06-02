package personal.leo.projectmeta.neo4j.procedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 * This is an example showing how you could expose Neo4j's full text indexes as
 * two procedures - one for updating indexes, and one for querying by label and
 * the lucene query language.
 */
public class SccmAndHcm {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    private static final char RELATION_SEPARATOR = 0;
    private static final String NAME = "name";
    private static final String APP = "app";

    @Procedure(value = "SccmAndHcm.score")
    public Stream<Score> score(
        @Name(APP) String app
    ) {
        final List<Score> scores = new ArrayList<>();

        final ProcessData processData = processData(app).findFirst().orElseGet(null);
        if (processData == null) {
            throw new RuntimeException("processData is null");
        }

        final Double sccmScore = Sccm.calc(
            processData.havingNoDirectRelationCount,
            processData.havingDirectRelationCount
        );
        final Double hcmScore = Hcm.calc(processData.relationsCountInApp, processData.appRelationCount);

        scores.add(new Score(sccmScore, hcmScore));

        return scores.stream();

    }

    @Procedure(value = "SccmAndHcm.processData")
    public Stream<ProcessData> processData(
        @Name(APP) String app
    ) {
        final Node[] nodes = buildNodes(app);

        final ArrayList<ProcessData> processDatum = new ArrayList<>();

        if (ArrayUtils.isNotEmpty(nodes)) {
            long havingDirectRelationsCount = 0L;
            long havingNoDirectRelationsCount = 0L;
            AtomicLong relationsCountInApp = new AtomicLong();

            final Set<String> noDirectRelations = buildNoDirectRelations(relationsCountInApp, app);

            for (int fromIndex = 0; fromIndex < nodes.length; fromIndex++) {
                final Node startNode = nodes[fromIndex];
                for (int toIndex = fromIndex + 1; toIndex < nodes.length; toIndex++) {
                    final Node endNode = nodes[toIndex];
                    final String relation = noDirectRelation(startNode, endNode);
                    if (noDirectRelations.contains(relation)) {
                        havingDirectRelationsCount++;
                    } else {
                        havingNoDirectRelationsCount++;
                    }
                }
            }

            final Long appRelationCount = calcAppRelationCount(app);

            final ProcessData processData = new ProcessData(
                havingDirectRelationsCount,
                havingNoDirectRelationsCount,
                relationsCountInApp.get(),
                appRelationCount
            );

            processDatum.add(processData);
        }

        return processDatum.stream();

    }

    private Node[] buildNodes(String app) {
        final String cql = String.format(
            "MATCH(:App{name:'%s'})"
                + "<-[:BELONG_TO]-(:Module)"
                + "<-[:BELONG_TO]-(:Package)"
                + "<-[:BELONG_TO]-(c:Class) "
                + "RETURN c",
            app
        );

        return db.execute(cql, new HashMap<>())
            .stream()
            .map(entry -> (Node)entry.get("c"))
            .toArray(Node[]::new);
    }

    private Set<String> buildNoDirectRelations(AtomicLong relationsCountInApp, String app) {
        final Set<String> relationCountRecord = new HashSet<>();

        final String cql = String.format(
            "MATCH (a:App {name: '%s'})\n"
                + "WITH a\n"
                + "MATCH p = (c1:Class)-[:CALL]->(c2:Class)\n"
                + "  WHERE (a)<-[:BELONG_TO]-(:Module)<-[:BELONG_TO]-(:Package)<-[:BELONG_TO]-(c1)\n"
                + "  AND (a)<-[:BELONG_TO]-(:Module)<-[:BELONG_TO]-(:Package)<-[:BELONG_TO]-(c2)\n"
                + "RETURN p",
            app
        );

        return db.execute(cql, new HashMap<>())
            .stream()
            .map(entry -> {
                final Path path = (Path)entry.get("p");
                final String noDirectRelation = noDirectRelation(path.startNode(), path.endNode());

                path.relationships().forEach(relationship -> {
                    final String directRelation = directRelation(path.startNode(), path.endNode())
                        + RELATION_SEPARATOR +
                        relationship.getProperty("toMethod");
                    System.out.println(directRelation);
                    if (!relationCountRecord.contains(directRelation)) {
                        relationsCountInApp.incrementAndGet();
                        relationCountRecord.add(directRelation);
                    }
                });

                return noDirectRelation;
            })
            .collect(Collectors.toSet());
    }

    private Long calcAppRelationCount(String app) {
        String cql = String.format(
            "MATCH(a:App {name: '%s'})\n"
                + "MATCH p = (ac:Class)-[c:CALL]-(oc:Class)\n"
                + "  WHERE (ac)-[:BELONG_TO]->(:Package)-[:BELONG_TO]->(:Module)-[:BELONG_TO]->(a)\n"
                + "  AND NOT (oc)-[:BELONG_TO]->(:Package)-[:BELONG_TO]->(:Module)-[:BELONG_TO]->(a)\n"
                + "RETURN COUNT(p) AS cnt",
            app
        );
        final Result result = db.execute(cql, new HashMap<>());
        if (result == null) {
            return 0L;
        } else {
            if (result.hasNext()) {
                return (Long)result.next().get("cnt");
            } else {
                return 0L;
            }
        }
    }

    private String noDirectRelation(Node startNode, Node endNode) {
        String startNodeName = (String)startNode.getProperty(NAME);
        String endNodeName = (String)endNode.getProperty(NAME);
        if (startNodeName.compareTo(endNodeName) > 0) {
            return endNodeName + RELATION_SEPARATOR + startNodeName;
        } else {
            return startNodeName + RELATION_SEPARATOR + endNodeName;
        }
    }

    private String directRelation(Node startNode, Node endNode) {
        String startNodeName = (String)startNode.getProperty(NAME);
        String endNodeName = (String)endNode.getProperty(NAME);
        return startNodeName + RELATION_SEPARATOR + endNodeName;
    }

    public static class ProcessData {
        public long havingDirectRelationCount;
        public long havingNoDirectRelationCount;
        public long relationsCountInApp;
        public long appRelationCount;

        public ProcessData(long havingDirectRelationCount, long havingNoDirectRelationCount, long relationsCountInApp,
            long appRelationCount) {
            this.havingDirectRelationCount = havingDirectRelationCount;
            this.havingNoDirectRelationCount = havingNoDirectRelationCount;
            this.relationsCountInApp = relationsCountInApp;
            this.appRelationCount = appRelationCount;
        }
    }

    public static class Sccm {
        private long pr;
        private long qr;

        public static Double calc(long pr, long qr) {
            return 1.0 * qr / (qr + pr);
        }

    }

    public static class Hcm {
        private long qrcomk;
        private long orcomk;

        public static Double calc(long qrcomk, long orcomk) {
            return 1.0 * qrcomk / (qrcomk + orcomk);
        }

    }

    public static class Score {
        public double sccm;
        public double hcm;

        public Score(double sccm, double hcm) {
            this.sccm = sccm;
            this.hcm = hcm;
        }
    }

}
