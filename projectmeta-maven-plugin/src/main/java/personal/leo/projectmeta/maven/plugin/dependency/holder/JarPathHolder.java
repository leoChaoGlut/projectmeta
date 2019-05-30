package personal.leo.projectmeta.maven.plugin.dependency.holder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import lombok.Cleanup;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;

/**
 * @author 谦扬
 * @date 2019-05-24
 */
public class JarPathHolder {

    @Getter
    private static final Set<String> absoluteJarPaths = Collections.synchronizedSet(new TreeSet<>());

    public static final String JAR_FILE_EXTENSION = ".jar";
    public static final String LOCAL_REPOSITORY_DIR;

    static {
        ProcessBuilder pb = new ProcessBuilder(
            "mvn",
            "help:evaluate",
            "-Dexpression=settings.localRepository",
            "-q",
            "-DforceStdout"
        );

        try {
            final Process process = pb.start();
            @Cleanup
            final InputStream is = process.getInputStream();
            LOCAL_REPOSITORY_DIR = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void add(String absoluteJarPath) {
        absoluteJarPaths.add(absoluteJarPath);
    }

    public static void parseAndAdd(Artifact artifact) {
        final String groupId = artifact.getGroupId();
        final String artifactId = artifact.getArtifactId();
        final String version = artifact.getVersion();

        String absoluteJarPath = LOCAL_REPOSITORY_DIR + File.separator +
            groupId.replace(".", File.separator) + File.separator +
            artifactId + File.separator +
            version + File.separator +
            artifactId + "-" + version + JAR_FILE_EXTENSION;

        absoluteJarPaths.add(absoluteJarPath);
    }

}
