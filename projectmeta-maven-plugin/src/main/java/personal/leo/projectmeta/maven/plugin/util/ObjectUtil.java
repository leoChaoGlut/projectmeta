package personal.leo.projectmeta.maven.plugin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;

import lombok.Cleanup;
import org.apache.commons.io.IOUtils;

/**
 * @author 谦扬
 * @date 2019-05-30
 */
public class ObjectUtil {

    public static <T> T readJsonObject(String filepath, Class<T> clz) throws IOException {
        final File file = new File(filepath);
        if (file.exists()) {
            @Cleanup
            final FileInputStream fis = new FileInputStream(file);
            final String json = IOUtils.toString(fis, StandardCharsets.UTF_8);
            return JSON.parseObject(json, clz);
        } else {
            return null;
        }
    }

    public static <T> List<T> readJsonList(String filepath, Class<T> clz) throws IOException {
        final File file = new File(filepath);
        if (file.exists()) {
            @Cleanup
            final FileInputStream fis = new FileInputStream(file);
            final String json = IOUtils.toString(fis, StandardCharsets.UTF_8);
            return JSON.parseArray(json, clz);
        } else {
            return new ArrayList<>();
        }
    }

    public static void writeJson(String filepath, Object obj) throws IOException {
        final FileOutputStream fos = new FileOutputStream(filepath);
        IOUtils.write(JSON.toJSONString(obj), fos, StandardCharsets.UTF_8);
    }

}
