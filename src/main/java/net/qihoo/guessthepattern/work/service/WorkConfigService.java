package net.qihoo.guessthepattern.work.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.work.domain.WorkRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkConfigService {

    public Path resolveConfigRoot(WorkRow work) {
        String dir = work.getConfigDir();
        if (!StringUtils.hasText(dir)) {
            dir = "config/works/" + work.getId();
        }
        Path p = Paths.get(dir);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
        }
        return p;
    }

    public boolean hasFieldConfig(WorkRow work) {
        return Files.isRegularFile(resolveConfigRoot(work).resolve("fields.json"));
    }

    public JSONObject loadFieldsJson(WorkRow work) {
        Path fields = resolveConfigRoot(work).resolve("fields.json");
        if (!Files.isRegularFile(fields)) {
            throw new BizException("未找到字段配置：" + fields);
        }
        try {
            String text = new String(Files.readAllBytes(fields), StandardCharsets.UTF_8);
            return JSON.parseObject(text);
        } catch (IOException e) {
            throw new BizException("读取 fields.json 失败：" + e.getMessage());
        }
    }

    public Map<String, Object> loadStudioConfig(WorkRow work) {
        JSONObject root = loadFieldsJson(work);
        Map<String, Object> out = new HashMap<>();
        out.put("workId", work.getId());
        out.put("titleCn", work.getTitleCn());
        out.put("configDir", work.getConfigDir());
        out.put("fields", root);

        Map<String, Object> enums = new HashMap<>();
        Path configRoot = resolveConfigRoot(work);
        if (root.containsKey("attrs")) {
            for (Object item : root.getJSONArray("attrs")) {
                JSONObject field = (JSONObject) item;
                String enumFile = field.getString("enumFile");
                if (!StringUtils.hasText(enumFile)) {
                    continue;
                }
                String key = field.getString("key");
                enums.put(key, readJsonFile(configRoot.resolve(enumFile)));
            }
        }
        out.put("enums", enums);
        return out;
    }

    private Object readJsonFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return JSON.parse(text);
        } catch (IOException e) {
            throw new BizException("读取枚举文件失败：" + path);
        }
    }

    public void copyConfigFromTemplate(String templateWorkId, String newWorkId, String titleCn) {
        Path from = Paths.get("config/works", templateWorkId);
        if (!from.isAbsolute()) {
            from = Paths.get(System.getProperty("user.dir")).resolve(from).normalize();
        }
        Path to = Paths.get("config/works", newWorkId);
        if (!to.isAbsolute()) {
            to = Paths.get(System.getProperty("user.dir")).resolve(to).normalize();
        }
        if (!Files.isDirectory(from)) {
            throw new BizException("模板配置目录不存在：" + from);
        }
        if (Files.exists(to)) {
            throw new BizException("目标配置目录已存在：" + to);
        }
        try {
            copyDirectory(from, to);
            JSONObject fields = JSON.parseObject(
                    new String(Files.readAllBytes(to.resolve("fields.json")), StandardCharsets.UTF_8));
            fields.put("workId", newWorkId);
            fields.put("titleCn", titleCn);
            Files.write(to.resolve("fields.json"),
                    JSON.toJSONString(fields, true).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BizException("复制配置目录失败：" + e.getMessage());
        }
    }

    private static void copyDirectory(Path from, Path to) throws IOException {
        Files.walk(from).forEach(source -> {
            try {
                Path target = to.resolve(from.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
