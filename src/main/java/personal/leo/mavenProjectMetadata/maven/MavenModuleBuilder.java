package personal.leo.mavenProjectMetadata.maven;

import java.util.Arrays;

import personal.leo.mavenProjectMetadata.maven.pom.Pom;

/**
 * @author leo
 * @date 2019-05-12
 */
public class MavenModuleBuilder {
    public static MavenModule build(Pom pom) {
        return new MavenModule()
            .setPom(pom)
            .setModuleDir(pom.getPomFile().getParentFile());

    }

    public static MavenModule[] build(Pom[] poms) {
        return Arrays.stream(poms)
            .map(MavenModuleBuilder::build)
            .toArray(MavenModule[]::new);
    }
}
