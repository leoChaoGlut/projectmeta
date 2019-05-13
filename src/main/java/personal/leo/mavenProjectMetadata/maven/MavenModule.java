package personal.leo.mavenProjectMetadata.maven;

import java.io.File;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;
import personal.leo.mavenProjectMetadata.Module;
import personal.leo.mavenProjectMetadata.maven.pom.Packaging;
import personal.leo.mavenProjectMetadata.maven.pom.Pom;

/**
 * @author leo
 * @date 2019-05-12
 */
@Getter
@Setter
@Accessors(chain = true)
public class MavenModule implements Module {
    private Pom pom;
    private File moduleDir;

    public boolean hasSubModule() {
        return ArrayUtils.isNotEmpty(pom.getSubModuleNames());
    }

    public String buildModuleId() {
        return pom.getGroupId() + ":" + pom.getArtifactId() + ":" + pom.getVersion();
    }

    public MavenModule[] buildSubModules() {
        if (hasSubModule()) {
            final Pom[] subPoms = pom.buildSubPoms();
            return MavenModuleBuilder.build(subPoms);
        } else {
            return new MavenModule[0];
        }
    }

    public boolean packagingIsNotPom() {
        return !packagingIsPom();
    }

    public boolean packagingIsPom() {
        return Packaging.POM.getValue().equals(pom.getPackaging());
    }

}
