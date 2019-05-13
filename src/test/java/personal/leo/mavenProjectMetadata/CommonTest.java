package personal.leo.mavenProjectMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

/**
 * @author leo
 * @date 2019-05-12
 */
public class CommonTest {
    @Test
    public void test0() throws IOException {
        final File file = new File("/Users/leo/gitRepo/project/test/test-knowledge-graph");
        final File file1 = new File(file.getAbsolutePath() + "/src/main/java");
        final Path p1 = file1.toPath();
        Files.walk(p1)
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(path -> {
                final String s1 = path.toAbsolutePath().toString();
                final String s2 = p1.toAbsolutePath().toString();
                final String s3 = s1.substring(s2.length() + 1, s1.length() - 5);
                final String s4 = s3.replace(File.separator, ".");
                System.out.println(s4);
                System.out.println(File.separator);
                //
            });
        System.out.println(this.getClass().getPackage().getName());
        //.forEach(System.out::println);
    }

    @Test
    public void test1() {
        Map<String, Set<String>> map = new HashMap<>();

        final Set<String> s1 = map.computeIfAbsent(
            "1",
            s -> new HashSet<>()
        );

        s1.add("a");

        final Set<String> s2 = map.computeIfAbsent(
            "2",
            s -> new HashSet<>()
        );

        s2.add("b");

        final Set<String> s3 = map.computeIfAbsent(
            "2",
            s -> new HashSet<>()
        );

        s3.add("c");

        System.out.println(map);
    }

    @Test
    public void test2() throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File("/Users/leo/gitRepo/project/test/pom.xml"));
        final Element rootElement = document.getRootElement();
        System.out.println(rootElement.getName());
        //rootElement.get
        final Element groupId = rootElement.element("groupId");
        System.out.println(groupId.getStringValue());
        final Element modules = rootElement.element("modules");
        final List<Element> module = modules.elements("module");
        for (Element element : module) {
            System.out.println(element.getStringValue());
        }
    }

}
