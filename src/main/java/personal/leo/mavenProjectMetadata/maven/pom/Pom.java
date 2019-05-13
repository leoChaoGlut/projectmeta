package personal.leo.mavenProjectMetadata.maven.pom;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author leo
 * @date 2019-05-12
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class Pom {

    public static final String POM_FILE_NAME = "pom.xml";

    private File pomFile;

    private String groupId;
    private String artifactId;
    private String version;
    /**
     * 默认 jar
     */
    private String packaging = "jar";
    private String[] subModuleNames;

    /**
     * TODO 效率可优化
     *
     * @param subModuleName
     * @return
     */
    public boolean containsSubModule(String subModuleName) {
        if (ArrayUtils.isEmpty(subModuleNames)) {
            return false;
        } else {
            return Arrays.asList(subModuleNames).contains(subModuleName);
        }
    }

    public Pom[] buildSubPoms() {
        final File[] subModuleFiles = pomFile.getParentFile()
            .listFiles(file -> file.isDirectory() && containsSubModule(file.getName()));

        if (ArrayUtils.isEmpty(subModuleFiles)) {
            return new Pom[] {};
        } else {
            return Arrays.stream(subModuleFiles)
                .map(subModuleFile -> {
                    final File[] pomFiles = subModuleFile.listFiles((dir, name) -> POM_FILE_NAME.equals(name));
                    if (ArrayUtils.isEmpty(pomFiles)) {
                        return null;
                    } else {
                        final File pomFile = pomFiles[0];
                        return PomParser.parse(pomFile);
                    }
                })
                .filter(Objects::nonNull)
                .toArray(Pom[]::new);
        }
    }

}
