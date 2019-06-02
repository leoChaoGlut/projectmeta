import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author 谦扬
 * @date 2019-06-02
 */
public class CommonTest {
    @Test
    public void test0() {
        Map<String, Long> map = new HashMap<>();

        Long a = map.computeIfAbsent("a", s -> 0L);
        a += 10;
    }
}
