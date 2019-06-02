package personal.leo.projectmeta.maven.plugin.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import personal.leo.projectmeta.maven.plugin.constants.OutputPath;

/**
 * @author 谦扬
 * @date 2019-05-24
 */
public class JavaParserUtil {
    @Getter
    private static final JavaParserFacade javaParserFacade;

    static {
        try {
            final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            combinedTypeSolver.add(new ReflectionTypeSolver());

            final List<String> jarPaths = ObjectUtil.readJsonList(
                OutputPath.JAR_PATH_JSON_FOR_JAVA_PARSER,
                String.class
            );
            List<String> notFoundJars = new ArrayList<>();
            for (String jarPath : jarPaths) {
                final File jarFile = new File(jarPath);
                if (jarFile.exists()) {
                    combinedTypeSolver.add(new JarTypeSolver(jarFile));
                } else {
                    notFoundJars.add(jarPath);
                }
            }

            if (CollectionUtils.isNotEmpty(notFoundJars)) {
                System.err.println("======not found jars as bellow======");
                System.err.println(String.join("\n", notFoundJars));
                System.err.println("======not found jars as above======");

                ObjectUtil.writeJson(
                    OutputPath.NOT_FOUND_JARS_JSON_FILE_PATH,
                    notFoundJars
                );
            }

            javaParserFacade = JavaParserFacade.get(combinedTypeSolver);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
