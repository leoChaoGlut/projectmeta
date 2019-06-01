package personal.leo.projectmeta.maven.plugin.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import personal.leo.projectmeta.maven.plugin.constants.OutputPath;
import personal.leo.projectmeta.maven.plugin.constants.Step;
import personal.leo.projectmeta.maven.plugin.index.MetaIndex;
import personal.leo.projectmeta.maven.plugin.util.ModuleUtil;
import personal.leo.projectmeta.maven.plugin.util.ObjectUtil;

import static personal.leo.projectmeta.maven.plugin.util.ModuleUtil.packagingIsNotPom;

/**
 * @author 谦扬
 * @date 2019-05-21
 * 构造单个项目元数据索引,如appMapModules,packageMapClasses
 * 注意,执行多个step2的时候,目前只能串行执行,否则会导致json文件写入混乱
 */
@Mojo(name = Step._2)
public class Step2Mojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject module;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Parameter(property = "appName")
    private String appName;

    public static final String SOURCES_ROOT_DIR_PATH = "/src/main/java";
    public static final String DOT = ".";
    public static final String JAVA_FILE_EXTENSION = ".java";
    public static final PathMatcher JAVA_FILE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.{java}");

    @Override
    public void execute() throws MojoExecutionException {
        if (StringUtils.isBlank(appName)) {
            throw new RuntimeException("add mvn param: -DappName=value");
        }

        if (packagingIsNotPom(module)) {
            final MetaIndex index = MetaIndex.getInstance();

            final String moduleId = ModuleUtil.buildModuleId(module);

            index.appMapModule(appName, moduleId);
            index.moduleMapApp(moduleId, appName);

            buildIndex(moduleId, module, index);

            mergeIndex(index);
        }
    }

    private void mergeIndex(MetaIndex index) {
        try {
            final MetaIndex existsIndex = ObjectUtil.readJsonObject(
                OutputPath.META_INDEX_JSON_FILE_PATH,
                MetaIndex.class
            );
            if (existsIndex != null) {
                index.merge(existsIndex);
            }

            ObjectUtil.writeJson(OutputPath.META_INDEX_JSON_FILE_PATH, index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildIndex(String moduleId, MavenProject module, MetaIndex index) {
        try {
            final File moduleDir = module.getBasedir();
            final File sourcesRootDir = new File(moduleDir.getCanonicalPath() + SOURCES_ROOT_DIR_PATH);

            Files.walk(sourcesRootDir.toPath())
                .filter(path -> JAVA_FILE_MATCHER.matches(path.getFileName()))
                .forEach(javaPath -> {
                    try {
                        final File javaFile = javaPath.toFile();
                        final String fullyQualifiedClassName = buildFullyQualifiedClassName(javaFile, sourcesRootDir);
                        String packageName = fullyQualifiedClassName
                            .substring(0, fullyQualifiedClassName.lastIndexOf(DOT));
                        String className = fullyQualifiedClassName
                            .substring(fullyQualifiedClassName.lastIndexOf(DOT) + 1);

                        index.moduleMapPackage(moduleId, packageName);
                        index.packageMapClass(packageName, className);

                        index.classMapPackage(fullyQualifiedClassName, packageName);
                        index.packageMapModule(packageName, moduleId);

                        index.classMapModule(fullyQualifiedClassName, moduleId);
                        index.moduleMapClass(moduleId, fullyQualifiedClassName);

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param javaFile       /src/main/java/personal/leo/Test.java
     * @param sourcesRootDir /src/main/java
     * @return personal.leo.Test
     * @throws IOException
     */
    private String buildFullyQualifiedClassName(File javaFile, File sourcesRootDir) throws IOException {
        final String javaFileCanonicalPath = javaFile.getCanonicalPath();
        return javaFileCanonicalPath
            .substring(
                sourcesRootDir.getCanonicalPath().length() + 1,
                javaFileCanonicalPath.length() - JAVA_FILE_EXTENSION.length()
            )
            .replace(File.separator, DOT);
    }

}
