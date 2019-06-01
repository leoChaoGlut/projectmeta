package personal.leo.projectmeta.maven.plugin.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import personal.leo.projectmeta.maven.plugin.constants.OutputPath;
import personal.leo.projectmeta.maven.plugin.constants.Step;
import personal.leo.projectmeta.maven.plugin.index.ClassRelation;
import personal.leo.projectmeta.maven.plugin.index.MetaIndex;
import personal.leo.projectmeta.maven.plugin.util.JavaParserUtil;
import personal.leo.projectmeta.maven.plugin.util.ModuleUtil;
import personal.leo.projectmeta.maven.plugin.util.ObjectUtil;

import static personal.leo.projectmeta.maven.plugin.mojo.Step2Mojo.DOT;
import static personal.leo.projectmeta.maven.plugin.mojo.Step2Mojo.JAVA_FILE_MATCHER;
import static personal.leo.projectmeta.maven.plugin.mojo.Step2Mojo.SOURCES_ROOT_DIR_PATH;
import static personal.leo.projectmeta.maven.plugin.util.ModuleUtil.packagingIsNotPom;

/**
 * @author 谦扬
 * @date 2019-05-21
 * 解析类调用关系
 */
@Mojo(name = Step._3)
public class Step3Mojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject module;

    public static final String ALIBABA_PACKAGE_PREFIX = "com.alibaba";
    public static final String PARSE_ERROR_JSON_FILE_PATH_TMPL = "/tmp/projectMeta/parseError/%s.json";

    @Parameter(property = "appName")
    private String appName;
    @Parameter(property = "productName", defaultValue = "dataphin")
    private String productName;

    private static ClassRelation classRelation;
    private static String staticProductName;
    private static String staticAppName;
    private static String moduleId;
    private static MetaIndex index;

    @Override
    public void execute() {
        try {
            if (StringUtils.isBlank(appName)) {
                throw new RuntimeException("add mvn param: -DappName=value");
            }

            index = ObjectUtil.readJsonObject(OutputPath.META_INDEX_JSON_FILE_PATH, MetaIndex.class);
            if (index == null) {
                throw new RuntimeException("Run Step2Mojo first");
            }

            if (packagingIsNotPom(module)) {
                staticProductName = productName;
                staticAppName = appName;

                moduleId = ModuleUtil.buildModuleId(module);

                parseJavaFilesUnderModuleAndBuildClassRelations();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseJavaFilesUnderModuleAndBuildClassRelations() {
        try {
            final File moduleBasedir = module.getBasedir();
            final File sourcesRootDir = new File(moduleBasedir.getCanonicalPath() + SOURCES_ROOT_DIR_PATH);

            Files.walk(sourcesRootDir.toPath())
                .filter(path -> JAVA_FILE_MATCHER.matches(path.getFileName()))
                .forEach(javaPath -> {
                    try {
                        final File javaFile = javaPath.toFile();
                        final CompilationUnit cu = StaticJavaParser.parse(javaFile);
                        final String fullyQualifiedClassName = buildFullyQualifiedClassName(cu);

                        final MethodVisitor visitor = new MethodVisitor();
                        cu.accept(visitor, fullyQualifiedClassName);

                        mergeClassRelations(visitor.getClassRelations());

                        writeParseError(visitor.getParseError());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeParseError(Map<String, SortedSet<String>> parseError) throws IOException {
        if (parseError.isEmpty()) {
            return;
        } else {
            String parseErrorFilePath = String.format(PARSE_ERROR_JSON_FILE_PATH_TMPL, moduleId);
            ObjectUtil.writeJson(parseErrorFilePath, parseError);
        }
    }

    private void mergeClassRelations(List<ClassRelation> classRelations) throws IOException {
        if (CollectionUtils.isEmpty(classRelations)) {
            return;
        }

        final List<ClassRelation> existsClassRelations = ObjectUtil.readJsonList(
            OutputPath.CLASS_RELATION_JSON_FILE_PATH,
            ClassRelation.class
        );

        classRelations.addAll(existsClassRelations);

        ObjectUtil.writeJson(OutputPath.CLASS_RELATION_JSON_FILE_PATH, classRelations);
    }

    public static class MethodVisitor extends VoidVisitorAdapter<String> {
        @Getter
        private final List<ClassRelation> classRelations = new ArrayList<>();
        @Getter
        private final Map<String, SortedSet<String>> parseError = Collections.synchronizedMap(new HashMap<>());

        @Override
        public void visit(MethodCallExpr methodCall, String fromClass) {
            try {
                final SymbolReference<ResolvedMethodDeclaration> reference = JavaParserUtil
                    .getJavaParserFacade()
                    .solve(methodCall);

                final String toClass = reference.getCorrespondingDeclaration().declaringType().getQualifiedName();
                final String toMethod = methodCall.getName().getId();

                if (fromClass.startsWith(ALIBABA_PACKAGE_PREFIX)
                    && toClass.startsWith(ALIBABA_PACKAGE_PREFIX)
                    && !fromClass.equals(toClass)
                ) {
                    String toModule = index.getModuleByClass(toClass);
                    String toApp = index.getAppByModule(toModule);

                    final ClassRelation classRelation = new ClassRelation()
                        .setFromProduct(staticProductName)
                        .setFromApp(staticAppName)
                        .setFromModule(moduleId)
                        .setFromPackage(fromClass.substring(0, fromClass.lastIndexOf(DOT)))
                        .setFromClass(fromClass.substring(fromClass.lastIndexOf(DOT) + 1))
                        .setToProduct(staticProductName)
                        .setToApp(toApp)
                        .setToModule(toModule)
                        .setToPackage(toClass.substring(0, toClass.lastIndexOf(DOT)))
                        .setToClass(toClass.substring(toClass.lastIndexOf(DOT) + 1))
                        .setToMethod(toMethod);

                    classRelations.add(classRelation);
                }

            } catch (Exception e) {
                if (fromClass.startsWith(ALIBABA_PACKAGE_PREFIX)) {
                    final String errMsg = e.getMessage();
                    if (errMsg != null) {
                        parseError
                            .computeIfAbsent(fromClass, s -> new TreeSet<>())
                            .add(errMsg);
                        //System.err.println("parse failed:" + e.getMessage());
                    }
                }
            }
        }

    }

    private String buildFullyQualifiedClassName(CompilationUnit cu) {
        StringBuilder fullyQualifiedClassNameBuilder = new StringBuilder();
        cu.getPackageDeclaration().ifPresent(packageDeclaration ->
            fullyQualifiedClassNameBuilder
                .append(packageDeclaration.getName().toString())
                .append(DOT)
        );

        cu.getPrimaryTypeName().ifPresent(fullyQualifiedClassNameBuilder::append);

        return fullyQualifiedClassNameBuilder.toString();
    }
}
