package personal.leo.projectmeta.maven.plugin.util;

import org.apache.maven.project.MavenProject;
import personal.leo.projectmeta.maven.plugin.constants.Packaging;

/**
 * @author 谦扬
 * @date 2019-06-01
 */
public class ModuleUtil {
    private static final String ARTIFACT_ID_SEPARATOR = "|";

    public static String buildModuleId(MavenProject mavenProject) {
        return mavenProject.getGroupId() + ARTIFACT_ID_SEPARATOR +
            mavenProject.getArtifactId() + ARTIFACT_ID_SEPARATOR +
            mavenProject.getVersion();
    }

    public static boolean packagingIsNotPom(MavenProject module) {
        return !Packaging.POM.equalsIgnoreCase(module.getPackaging());
    }
}
