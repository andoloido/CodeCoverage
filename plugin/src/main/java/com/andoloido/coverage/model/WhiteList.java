package com.andoloido.coverage.model;

import static com.andoloido.coverage.utils.PatternUtil.convertToPatternString;
import static com.andoloido.coverage.utils.PatternUtil.resolveDollarChar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WhiteList {
    public static WhiteList buildWithList(List<String> list) {
        WhiteList res = new WhiteList();
        res.fill(list);
        return res;
    }

    // 命中白名单的才会检查
    private final Map<String, List<Pattern>> patternList = new HashMap<>();

    private void fill(List<String> whiteList) {
        patternList.clear();
        if (whiteList == null) return;
        whiteList.forEach(s -> {
            String key = getKey(s);
            String patternStr = convertToPatternString(resolveDollarChar(s));
            patternList.computeIfAbsent(key, k -> new ArrayList<>()).add(Pattern.compile(patternStr));
        });
    }


    public boolean inWhiteList(String className) {
        if (className.isEmpty() || patternList.isEmpty()) return false;
        String key = getKey(className);
        for (Pattern pattern : patternList.getOrDefault(key, new ArrayList<>())) {
            if (pattern.matcher(className).matches()) return true;
        }
        return false;
    }

    private String getKey(String className) {
        if (className == null || className.isEmpty()) return "";
        return className.substring(0, 1);
    }

    public boolean isEmpty() {
        return patternList.isEmpty();
    }

    public void clear() {
        patternList.clear();
    }
}
