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

    @Procedure(value = "SccmAndHcm.scoreOfAllApps")
    public Stream<Score> scoreOfAllApps() {
        final String cql = "MATCH(a:App) RETURN a.name AS appName";
        return db.execute(cql, new HashMap<>())
            .stream()
            .map(entry -> {
                final String appName = (String)entry.get("appName");
                return score(appName).findFirst().get();
            });
    }

    @Procedure(value = "SccmAndHcm.processDataOfAllApps")
    public Stream<ProcessData> processDataOfAllApps() {
        final String cql = "MATCH(a:App) RETURN a.name AS appName";
        return db.execute(cql, new HashMap<>())
            .stream()
            .map(entry -> {
                final String appName = (String)entry.get("appName");
                return processData(appName).findFirst().get();
            });
    }

    @Procedure(value = "SccmAndHcm.score")
    public Stream<Score> score(
        @Name(APP) String app
    ) {
        final List<Score> scores = new ArrayList<>();

        final ProcessData processData = processData(app).findFirst().orElseGet(null);
        if (processData == null) {
            throw new RuntimeException("processData is null");
        }

        long classCount = countClass(app);

        final Double sccmScore = Sccm.calc(
            processData.havingNoDirectRelationCount,
            processData.havingDirectRelationCount
        );
        final String sccmLevel = Sccm.level(sccmScore, classCount);

        final Double hcmScore = Hcm.calc(processData.relationsCountInApp, processData.appRelationCount);
        final String hcmLevel = Hcm.level(hcmScore);

        scores.add(new Score(app, sccmScore, hcmScore, sccmLevel, hcmLevel));

        return scores.stream();

    }

    private long countClass(String app) {
        final String cql = String.format(
            "MATCH(:App {name: '%s'})<-[:BELONG_TO]-"
                + "(:Module)<-[:BELONG_TO]-"
                + "(:Package)<-[:BELONG_TO]-"
                + "(c:Class)\n"
                + "RETURN count(c) AS classCount",
            app
        );

        return db.execute(cql, new HashMap<>())
            .map(entry -> (long)entry.get("classCount"))
            .next();
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
                app,
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
        public String appName;
        public long havingDirectRelationCount;
        public long havingNoDirectRelationCount;
        public long relationsCountInApp;
        public long appRelationCount;

        public ProcessData(String appName, long havingDirectRelationCount, long havingNoDirectRelationCount,
            long relationsCountInApp, long appRelationCount) {
            this.appName = appName;
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

        public static String level(Double score, long classCount) {
            final int sccmThreshold = 6;
            double n = 1.0 * classCount;
            if (classCount >= sccmThreshold) {
                if (2.0 / n <= score && score <= (16 + 2.0 * n) / (n * (n - 1))) {
                    return "Good: Simple module with low complexity and good cohesion: 2/N<=SCCM<=16+2N/N(N-1)";
                } else if ((16 + 2.0 * n) / (n * (n - 1)) <= score && score <= (36 + 2.0 * n) / (n * (n - 1))) {
                    return "Acceptable: Good cohesion,but a little complex: 16+2N/N(N-1)<=SCCM<=36+2N/N(N-1)";
                } else if (score <= 2.0 / n) {
                    return "Bad: Very low cohesion and low complexity: SCC<=2/N";
                } else if (score >= (36 + 2.0 * n) / (n * (n - 1))) {
                    return "Bad: High cohesion,but too complex: SCC>=36+2N/N(N-1)";
                } else {
                    return "score is illgal:" + score;
                }
            } else {
                if (0.66f < score && score <= 1) {
                    return "Good (0.66,1]";
                } else if (0.5f <= score && score <= 0.66f) {
                    return "Acceptable [0.5,0.66]";
                } else if (0 <= score && score < 0.5f) {
                    return "Acceptable [0.5,0.66]";
                } else {
                    return "score is illgal:" + score;
                }
            }
        }

    }

    public static class Hcm {
        private long qrcomk;
        private long orcomk;

        public static Double calc(long qrcomk, long orcomk) {
            return 1.0 * qrcomk / (qrcomk + orcomk);
        }

        public static String level(Double score) {
            if (0.5f <= score && score < 1) {
                return "Good [0.5,1)";
            } else if (0.33f <= score && score < 0.5f) {
                return "Acceptable [0.33,0.5)";
            } else if (0 <= score && score < 0.33f) {
                return "Bad(Low cohesion) [0,0.33)";
            } else if (score == 1) {
                return "Bad(The Component is useless) 1";
            } else {
                return "score is illgal:" + score;
            }
        }
    }

    public static class Score {
        public String appName;
        public double sccm;
        public double hcm;
        public String sccmLevel;
        public String hcmLevel;

        public Score(String appName, double sccm, double hcm, String sccmLevel, String hcmLevel) {
            this.appName = appName;
            this.sccm = sccm;
            this.hcm = hcm;
            this.sccmLevel = sccmLevel;
            this.hcmLevel = hcmLevel;
        }
    }

}
