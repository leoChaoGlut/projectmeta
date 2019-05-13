package personal.leo.mavenProjectMetadata;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import personal.leo.mavenProjectMetadata.git.Git;

/**
 * @author leo
 * @date 2019-05-12
 */
public class GitTest {
    @Test
    public void gitCloneTest() {
        final String outputDirPath = Git.clone("git@github.com:leoChaoGlut/presto-etl-executor.git");
        final File outputDir = new File(outputDirPath);
        final File[] files = outputDir.listFiles((dir, name) -> "pom.xml".equals(name));
        if (ArrayUtils.isNotEmpty(files)) {
            final File pomFile = files[0];

        }
    }
}
