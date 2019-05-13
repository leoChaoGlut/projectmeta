package personal.leo.mavenProjectMetadata.maven;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import personal.leo.mavenProjectMetadata.Traversal;

/**
 * @author leo
 * @date 2019-05-12
 */
@Getter
public class MavenModuleTraversal implements Traversal<MavenModule> {

    public static final String JAVA_FILE_SUFFIX = ".java";
    public static final String SOURCES_ROOT_DIR_PATH = "/src/main/java";
    public static final String PACKAGE_SEPARATOR = ".";

    /**
     * TODO 在汇总索引的时候,需要判断 moduleId 是否已存在,存在则报错
     *
     * key: moduleId(groupId+artifactId+version)
     * value: 该模块下的所有类(暂不处理test)
     */
    private Map<String, Set<String>> moduleMapClass = new HashMap<>();
    /**
     * key: classId(如 personal.leo.projectmeta.maven.MavenModuleTraversal)
     * value: 类所属的 moduleId(groupId+artifactId+version)
     */
    private Map<String, Set<String>> classMapModule = new HashMap<>();

    @Override
    public void traversalModule(MavenModule mavenModule) {
        final String moduleId = mavenModule.buildModuleId();

        if (mavenModule.packagingIsNotPom()) {
            final Set<String> classSet = findAllClassUnderModule(mavenModule.getModuleDir(), moduleId);

            if (CollectionUtils.isNotEmpty(classSet)) {
                moduleMapClass.put(moduleId, classSet);
            }
        }

        if (mavenModule.hasSubModule()) {
            final MavenModule[] subMavenModules = mavenModule.buildSubModules();
            for (MavenModule subMavenModule : subMavenModules) {
                traversalModule(subMavenModule);
            }
        }
    }

    private Set<String> findAllClassUnderModule(File moduleDir, String moduleId) {
        try {
            final File sourcesRoot = new File(moduleDir.getAbsolutePath() + SOURCES_ROOT_DIR_PATH);
            final Path sourcesRootPath = sourcesRoot.toPath();

            return Files.walk(sourcesRootPath)
                .filter(path -> path.toString().endsWith(JAVA_FILE_SUFFIX))
                .map(javaPath -> {
                    final String sourcesRootPathStr = sourcesRootPath.toAbsolutePath().toString();
                    final String javaPathStr = javaPath.toAbsolutePath().toString();

                    final String packageWithJavaName = javaPathStr.substring(
                        sourcesRootPathStr.length() + 1,
                        javaPathStr.length() - JAVA_FILE_SUFFIX.length()
                    ).replace(File.separator, PACKAGE_SEPARATOR);

                    addClassMapModule(moduleId, packageWithJavaName);

                    return packageWithJavaName;
                })
                .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addClassMapModule(String moduleId, String packageWithJavaName) {
        final Set<String> moduleIds = classMapModule.computeIfAbsent(
            packageWithJavaName,
            key -> new HashSet<>()
        );

        moduleIds.add(moduleId);
    }
}
