package personal.leo.projectmeta.maven.plugin.mojo;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @author 谦扬
 * @date 2019-05-30
 */
@Mojo(name = "meta")
public class MetaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String str = reactorProjects.stream()
            .filter(this::isJar)
            .map(MavenProject::getId)
            .collect(Collectors.joining("\n"));

        System.out.println(str);
    }

    private boolean isJar(MavenProject mavenProject) {
        return "jar".equalsIgnoreCase(mavenProject.getPackaging());
    }
}
