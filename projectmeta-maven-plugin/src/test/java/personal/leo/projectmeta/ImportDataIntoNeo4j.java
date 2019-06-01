package personal.leo.projectmeta;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Stopwatch;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.StatementResult;
import personal.leo.projectmeta.maven.plugin.constants.OutputPath;
import personal.leo.projectmeta.maven.plugin.index.ClassRelation;
import personal.leo.projectmeta.maven.plugin.index.MetaIndex;
import personal.leo.projectmeta.maven.plugin.util.ObjectUtil;

/**
 * @author 谦扬
 * @date 2019-06-02
 */
public class ImportDataIntoNeo4j {
    Driver driver;

    @Before
    public void before() {
        driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j"));
    }

    @Test
    public void buildConstraints() {
        String[] cqls = new String[] {
            "CREATE CONSTRAINT ON (p:Product) ASSERT p.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (a:App) ASSERT a.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (m:Module) ASSERT m.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (pkg:Package) ASSERT pkg.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (c:Class) ASSERT c.name IS UNIQUE;",
        };

        for (String cql : cqls) {
            runCql(cql);
        }
    }

    @Test
    public void buildNodes() throws IOException {
        final MetaIndex index = ObjectUtil.readJsonObject(OutputPath.META_INDEX_JSON_FILE_PATH, MetaIndex.class);

        appMapModule(index);
        moduleMapPackage(index);
        packageMapClass(index);
    }

    @Test
    public void buildEdges() throws IOException {
        final List<ClassRelation> classRelations = ObjectUtil.readJsonList(
            OutputPath.CLASS_RELATION_JSON_FILE_PATH,
            ClassRelation.class
        );

        String tmpl = "MATCH(fromApp:App {name: '%s'})\n"
            + "       <-[:BELONG_TO]-(fromModule:Module {name: '%s'})\n"
            + "       <-[:BELONG_TO]-(fromPkg:Package {name: '%s'})\n"
            + "       <-[:BELONG_TO]-(fromClz:Class {name: '%s'})\n"
            + "         MATCH(toApp:App {name: '%s'})\n"
            + "       <-[:BELONG_TO]-(toModule:Module {name: '%s'})\n"
            + "       <-[:BELONG_TO]-(toPkg:Package {name: '%s'})\n"
            + "       <-[:BELONG_TO]-(toClz:Class {name: '%s'})\n"
            + "MERGE (fromClz)-[:CALL {toMethod: '%s'}]->(toClz)";

        final Stopwatch watch = Stopwatch.createUnstarted();

        for (int i = 0; i < classRelations.size(); i++) {
            watch.reset().start();
            final ClassRelation classRelation = classRelations.get(i);

            final String cql = String.format(tmpl,
                classRelation.getFromApp(),
                classRelation.getFromModule(),
                classRelation.getFromPackage(),
                classRelation.getFromClass(),
                classRelation.getToApp(),
                classRelation.getToModule(),
                classRelation.getToPackage(),
                classRelation.getToClass(),
                classRelation.getToMethod()
            );

            runCql(cql);

            watch.stop();
            System.out.println(i + "/" + classRelations.size() + ":" + watch);
        }

    }

    private void packageMapClass(MetaIndex index) {
        index.getPackageMapClasses().forEach((pkg, classes) -> {
            StringBuilder cql = new StringBuilder();

            cql.append(String.format("MERGE(p:Package{name:'%s'})\n", pkg))
                .append("WITH p \n");
            classes.forEach(clz -> {
                cql.append(String.format("MERGE(:Class{name:'%s'})-[:BELONG_TO]->(p)\n", clz));
            });

            runCql(cql.toString());
        });
    }

    private void moduleMapPackage(MetaIndex index) {
        index.getModuleMapPackages().forEach((module, packages) -> {
            StringBuilder cql = new StringBuilder();

            cql.append(String.format("MERGE(m:Module{name:'%s'})\n", module))
                .append("WITH m \n");
            packages.forEach(pkg -> {
                cql.append(String.format("MERGE(:Package{name:'%s'})-[:BELONG_TO]->(m)\n", pkg));
            });

            runCql(cql.toString());
        });
    }

    private void appMapModule(MetaIndex index) {
        index.getAppMapModules().forEach((app, modules) -> {
            StringBuilder cql = new StringBuilder();

            cql.append(String.format("MERGE(a:App{name:'%s'})\n", app))
                .append("WITH a \n");
            modules.forEach(module ->
                cql.append(String.format("MERGE(:Module{name:'%s'})-[:BELONG_TO]->(a)\n", module))
            );

            runCql(cql.toString());
        });
    }

    private StatementResult runCql(String cql) {
        return driver.session().run(cql);
    }

}
