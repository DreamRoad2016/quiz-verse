package net.quizverse.pack;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Build searchable pinyin keys for Chinese names / aliases.
 */
public final class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private PinyinUtils() {
    }

    /**
     * @return keys such as full concat {@code zhenhuan}, spaced {@code zhen huan}, initials {@code zh}
     */
    public static List<String> searchKeys(String... texts) {
        Set<String> keys = new LinkedHashSet<>();
        if (texts == null) {
            return List.of();
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            addKeysForText(keys, text.trim());
        }
        return new ArrayList<>(keys);
    }

    private static void addKeysForText(Set<String> keys, String text) {
        List<String> syllables = new ArrayList<>();
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                String py = firstPinyin(cp);
                if (py != null && !py.isEmpty()) {
                    syllables.add(py);
                    initials.append(py.charAt(0));
                }
            } else if (Character.isLetterOrDigit(cp)) {
                String ch = new String(Character.toChars(cp)).toLowerCase(Locale.ROOT);
                syllables.add(ch);
                initials.append(ch.charAt(0));
            }
            // skip punctuation / spaces in source
        }
        if (syllables.isEmpty()) {
            return;
        }
        String joined = String.join("", syllables);
        String spaced = String.join(" ", syllables);
        keys.add(joined);
        keys.add(spaced);
        String init = initials.toString();
        if (init.length() >= 2) {
            keys.add(init);
        }
    }

    private static String firstPinyin(int codePoint) {
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray((char) codePoint, FORMAT);
            if (arr == null || arr.length == 0) {
                return null;
            }
            return arr[0];
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return null;
        }
    }
}
