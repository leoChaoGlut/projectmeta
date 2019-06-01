package personal.leo.projectmeta.maven.plugin.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 谦扬
 * @date 2019-06-01
 */
@Getter
@AllArgsConstructor
public enum ClassRelationCsvHeader {

    FROM_PRODUCT(0),
    FROM_APP(1),
    FROM_MODULE(2),
    FROM_PACKAGE(3),
    FROM_CLASS(4),
    TO_PRODUCT(5),
    TO_APP(6),
    TO_MODULE(7),
    TO_PACKAGE(8),
    TO_CLASS(9),
    TO_METHOD(10);

    private int index;

}
