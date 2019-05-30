package personal.leo.projectmeta.maven.plugin.mojo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import personal.leo.projectmeta.maven.plugin.constants.OutputPath;
import personal.leo.projectmeta.maven.plugin.constants.Step;
import personal.leo.projectmeta.maven.plugin.dependency.builder.DependencyGraphBuilder;
import personal.leo.projectmeta.maven.plugin.dependency.exception.DependencyGraphBuilderException;
import personal.leo.projectmeta.maven.plugin.dependency.holder.JarPathHolder;
import personal.leo.projectmeta.maven.plugin.dependency.node.DependencyNode;
import personal.leo.projectmeta.maven.plugin.dependency.visitor.DoNothingVisitor;
import personal.leo.projectmeta.maven.plugin.util.ObjectUtil;

/**
 * @author 谦扬
 * @date 2019-05-21
 * 落盘工程所需的jar
 */
@Mojo(name = Step._1)
public class Step1Mojo extends AbstractMojo {
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * Contains the full list of projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(property = "scope")
    private String scope;

    private DependencyNode rootNode;

    @Override
    public void execute() throws MojoExecutionException {
        try {

            // TODO: note that filter does not get applied due to MSHARED-4
            ArtifactFilter artifactFilter = createResolvingArtifactFilter();

            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

            buildingRequest.setProject(project);

            rootNode = dependencyGraphBuilder.buildDependencyGraph(
                buildingRequest,
                artifactFilter,
                reactorProjects,
                Step._1
            );

            final DoNothingVisitor visitor = new DoNothingVisitor();

            rootNode.accept(visitor);

            final Set<String> jarPaths = JarPathHolder.getAbsoluteJarPaths();

            final List<String> existsJarPaths = ObjectUtil.readJsonList(
                OutputPath.JAR_PATH_JSON_FOR_JAVA_PARSER,
                String.class
            );

            jarPaths.addAll(existsJarPaths);

            ObjectUtil.writeJson(OutputPath.JAR_PATH_JSON_FOR_JAVA_PARSER, jarPaths);

        } catch (DependencyGraphBuilderException | IOException exception) {
            throw new MojoExecutionException("Cannot build project dependency graph", exception);
        }
    }

    private ArtifactFilter createResolvingArtifactFilter() {
        ArtifactFilter filter;

        // filter scope
        if (scope != null) {
            getLog().debug("+ Resolving dependency tree for scope '" + scope + "'");

            filter = new ScopeArtifactFilter(scope);
        } else {
            filter = null;
        }

        return filter;
    }

}
