package personal.leo.mavenProjectMetadata;

import java.io.File;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.junit.Test;
import personal.leo.mavenProjectMetadata.maven.MavenModule;
import personal.leo.mavenProjectMetadata.maven.MavenModuleBuilder;
import personal.leo.mavenProjectMetadata.maven.MavenModuleTraversal;
import personal.leo.mavenProjectMetadata.maven.pom.Pom;
import personal.leo.mavenProjectMetadata.maven.pom.PomParser;

/**
 * @author leo
 * @date 2019-05-12
 */
public class ProjectMetaTest {
    @Test
    public void buildProjectMetaByPom() {
        final Pom pom = PomParser.parse(new File("/Users/leo/gitRepo/project/test/pom.xml"));
        System.out.println(pom);
        final MavenModule mavenModule = MavenModuleBuilder.build(pom);
        final MavenModuleTraversal traversal = new MavenModuleTraversal();
        traversal.traversalModule(mavenModule);

        final Map<String, Set<String>> classMapModule = traversal.getClassMapModule();
        final Map<String, Set<String>> moduleMapClass = traversal.getModuleMapClass();

        System.out.println(JSON.toJSONString(classMapModule, SerializerFeature.PrettyFormat));
        System.out.println(JSON.toJSONString(moduleMapClass, SerializerFeature.PrettyFormat));
    }

    @Test
    public void buildProjectMetaByGitRepoUrl() {
        final Pom pom = PomParser.parse("git@github.com:leoChaoGlut/spring-cloud-demo.git");
        System.out.println(pom);
        final MavenModule mavenModule = MavenModuleBuilder.build(pom);
        final MavenModuleTraversal traversal = new MavenModuleTraversal();
        traversal.traversalModule(mavenModule);

        final Map<String, Set<String>> classMapModule = traversal.getClassMapModule();
        final Map<String, Set<String>> moduleMapClass = traversal.getModuleMapClass();

        System.out.println(JSON.toJSONString(classMapModule, SerializerFeature.PrettyFormat));
        System.out.println(JSON.toJSONString(moduleMapClass, SerializerFeature.PrettyFormat));
    }
}
