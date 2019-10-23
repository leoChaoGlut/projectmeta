package personal.leo.projectmeta.maven.plugin.index;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author 谦扬
 * @date 2019-06-01
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
@EqualsAndHashCode
public class ClassRelation {
    private String fromProduct;
    private String fromApp;
    private String fromModule;
    private String fromPackage;
    private String fromClass;
    private String toProduct;
    private String toApp;
    private String toModule;
    private String toPackage;
    private String toClass;
    private String toMethod;
}
