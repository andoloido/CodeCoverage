package com.andoloido.coverage.utils;

import com.android.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * mapping id generator
 */
public class MappingIdGen {

    private AtomicInteger lastIndex;
    private Map<Method, String> method2IdMapping = new HashMap<>(70000);
    private Map<String, Method> id2MethodMapping = new HashMap<>(70000);

    String latestFilePath;
    String mappingFilePath;

    /**
     * constructor
     * @param latestFilePath path
     * @param mappingFilePath path
     * @throws IOException exception
     */
    public MappingIdGen(String latestFilePath, String mappingFilePath) throws IOException {
        this.latestFilePath = latestFilePath;
        this.mappingFilePath = mappingFilePath;

        // 这个功能是增量用的，但CI貌似没法保存之前的文件，写的时候不太熟悉CI，所以并没有用....
        FileUtil.readTxtByLine(latestFilePath, s -> {
            String[] fields = s.split(" ");
            String id = fields[0];
            String className = fields[1];
            String desc = fields[2];
            String methodName = fields[3];
            Method method = new Method(className, methodName, desc);
            method2IdMapping.put(method, id);
            id2MethodMapping.put(id, method);
            return true;
        });
        lastIndex = new AtomicInteger(method2IdMapping.size());
    }

    public synchronized int genMappingId(String className, String methodName, String desc) {
        // desc暂时用方法序号来代替..
        Method m = new Method(className, methodName, desc);
        String id = method2IdMapping.get(m);
        if (id == null) {
            int i = lastIndex.incrementAndGet();
            int realId = i << 11;
            String stringId = String.valueOf(realId);
            m.used = true;
            id2MethodMapping.put(stringId, m);
            return realId;
        } else {
            // 复用原来的id
            id2MethodMapping.get(id).used = true;
            return Integer.valueOf(id);
        }
    }

    public void saveMapping() throws IOException {
        File newFile = new File(mappingFilePath);
        File latestFile = new File(latestFilePath);
        FileUtil.checkFile(newFile);
        FileUtil.checkFile(latestFile);
        try (FileWriter fileWriter = new FileWriter(newFile, false)) {
            for (Map.Entry<String, Method> stringMethodEntry : id2MethodMapping.entrySet()) {
                Method method = stringMethodEntry.getValue();
                if (method.used) {
                    fileWriter.write(stringMethodEntry.getKey() + " " + method.className + " " + method.desc + " " + method.methodName + "\n");
                }
            }
            fileWriter.flush();
            if (id2MethodMapping.size() >= method2IdMapping.size()) {
                FileUtils.copyFile(newFile, latestFile);
            }
        }
    }

    public void clean(){
        id2MethodMapping = null;
        method2IdMapping = null;
    }

    private static class Method {
        private String className;
        private String methodName;
        public String desc;
        // 是否使用过
        private boolean used = false;

        public Method(String className, String methodName, String desc) {
            this.className = className;
            this.methodName = methodName;
            this.desc = desc;
        }

        @Override
        public int hashCode() {
            return (className + " " + methodName + " " + desc).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            Method m = (Method) o;
            return m.className.equals(this.className) && m.methodName.equals(this.methodName) && m.desc.equals(this.desc);
        }
    }
}

