package com.andoloido.coverage.utils;

import org.gradle.api.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class PatternUtil {
    public static final String MATCH_ALL = ".+";
    public static final Pattern PATTERN_MATCH_ALL = Pattern.compile(MATCH_ALL);
    private static final Pattern rClassSimpleNamePattern = Pattern.compile("R(\\$.+)?");
    private static final Pattern r2ClassSimpleNamePattern = Pattern.compile("R2?(\\$.+)?");

    @Deprecated
    public static boolean isReleaseBuild(Project project) {
        List<String> taskNames = project.getGradle().getStartParameter().getTaskNames();
        for (int index = 0; index < taskNames.size(); ++index) {
            String taskName = taskNames.get(index);
            if (taskName.toLowerCase().contains("release")) {
                return true;
            }
        }
        return false;
    }

    public static String convertToPatternString(String input) {
        // ?	Zero or one character
        // *	Zero or more of character
        // +	One or more of character
        Map<Character, String> map = new HashMap<>(4);
        map.put('.', "\\.");
        map.put('?', ".?");
        map.put('*', ".*");
        map.put('+', ".+");
        StringBuilder sb = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            Character ch = input.charAt(i);
            String replacement = map.get(ch);
            sb.append(replacement == null ? ch : replacement);
        }
        return sb.toString();
    }

    public static String resolveDollarChar(String s) {
        // 内部类的类名定义用的是$做分隔符，由于反斜杠在 Java 中是转义字符，因此要在代码中使用两个反斜杠表示一个真正的反斜杠字符。例如，"\\\\" 表示一个真正的反斜杠字符。
        s = s.replaceAll("\\$", "\\\\\\$");
        return s;
    }

    public static String getPackage(String className) {
        int packageEnd = className.lastIndexOf("/");
        return packageEnd > 0 ? className.substring(0, packageEnd) : "";
    }

    public static boolean isRFile(String relativePath) {
        int end = relativePath.lastIndexOf(".class");
        return end > 0 && isRClass(relativePath.substring(0, end));
    }

    public static boolean isR2File(String relativePath) {
        int end = relativePath.lastIndexOf(".class");
        return end > 0 && isR2Class(relativePath.substring(0, end));
    }

    public static boolean isRClass(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf("/");
        return rClassSimpleNamePattern.matcher(name.substring(classNameStart + 1)).matches();
    }

    public static boolean isR2Class(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf("/");
        return r2ClassSimpleNamePattern.matcher(name.substring(classNameStart + 1)).matches();
    }

    public static boolean isRClassName(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf(".");
        return rClassSimpleNamePattern.matcher(name.substring(classNameStart + 1)).matches();
    }

    public static boolean isRStyleableClass(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf("/");
        return "R$styleable".equals(name.substring(classNameStart + 1));
    }

    public static String getInnerRClass(String className) {
        if (className == null || className.isEmpty()) return "";
        int innerClassStart = className.lastIndexOf("$");
        if (innerClassStart == -1) return "";
        return className.substring(innerClassStart + 1);
    }

    public static String replaceDot2Slash(String str) {
        return str.replace('.', '/');
    }

    public static String replaceSlash2Dot(String str) {
        return str.replace('/', '.');
    }

    public static boolean inSamePackage(String classA, String classB) {
        return Objects.equals(getPackage(classA), getPackage(classB));
    }
}

