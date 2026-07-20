package net.quizverse.pack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PinyinUtilsTest {

    @Test
    void zhenhuanKeys() {
        List<String> keys = PinyinUtils.searchKeys("甄嬛", "嬛嬛");
        assertTrue(keys.contains("zhenhuan"), () -> "keys=" + keys);
        assertTrue(keys.stream().anyMatch(k -> k.startsWith("zh")), () -> "keys=" + keys);
    }

    @Test
    void aliasKeys() {
        List<String> keys = PinyinUtils.searchKeys("安陵容", "陵容");
        assertTrue(keys.contains("anlingrong") || keys.stream().anyMatch(k -> k.contains("lingrong")),
                () -> "keys=" + keys);
    }
}
