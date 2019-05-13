package personal.leo.mavenProjectMetadata.maven.pom;

import lombok.Getter;

/**
 * @author leo
 * @date 2019-05-12
 */
@Getter
public enum Packaging {
    //
    POM("pom");

    private String value;

    Packaging(String value) {
        this.value = value;
    }
}
