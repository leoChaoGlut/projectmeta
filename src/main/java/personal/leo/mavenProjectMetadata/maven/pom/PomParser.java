package personal.leo.mavenProjectMetadata.maven.pom;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import personal.leo.mavenProjectMetadata.git.Git;

/**
 * @author leo
 * @date 2019-05-12
 */
public class PomParser {

    public static Pom parse(String gitRepoUrl) {
        final String outputDirPath = Git.clone(gitRepoUrl);
        final File outputDir = new File(outputDirPath);
        final File[] files = outputDir.listFiles((dir, name) -> Pom.POM_FILE_NAME.equals(name));
        if (ArrayUtils.isEmpty(files)) {
            return null;
        } else {
            final File pomFile = files[0];
            return parse(pomFile);
        }
    }

    public static Pom parse(File pomFile) {
        SAXReader reader = new SAXReader();
        Document document;
        try {
            document = reader.read(pomFile);
        } catch (DocumentException e) {
            throw new RuntimeException("pom file not found");
        }

        final Element project = document.getRootElement();

        final Element artifactId = project.element("artifactId");

        final Element groupId = parseFromParentElementIfNull(project, "groupId");
        final Element version = parseFromParentElementIfNull(project, "version");
        final Element packaging = project.element("packaging");

        final Element modules = project.element("modules");

        final Pom pom = new Pom()
            .setGroupId(groupId.getStringValue())
            .setArtifactId(artifactId.getStringValue())
            .setVersion(version.getStringValue())
            .setPomFile(pomFile);

        if (packaging != null) {
            pom.setPackaging(packaging.getStringValue());
        }

        if (modules != null) {
            final List<Element> moduleList = modules.elements("module");
            if (CollectionUtils.isNotEmpty(moduleList)) {
                final String[] subModuleNames = moduleList.stream()
                    .map(Element::getStringValue)
                    .collect(Collectors.toList())
                    .toArray(ArrayUtils.EMPTY_STRING_ARRAY);

                pom.setSubModuleNames(subModuleNames);
            }
        }

        return pom;
    }

    private static Element parseFromParentElementIfNull(Element project, String elementName) {
        Element childOfProject = project.element(elementName);

        if (childOfProject == null) {
            final Element parent = project.element("parent");
            childOfProject = parent.element(elementName);
        }

        return childOfProject;
    }
}
