package com.andoloido.coverage;

import java.util.List;

/**
 * 插件的 extension，方便自定义配置
 */
public class CoverageExtension {
    // 白名单，控制哪些类主要插桩
    public List<String> whiteList;

    // 只插桩 static 代码块
    public boolean clinitOnly;
}
