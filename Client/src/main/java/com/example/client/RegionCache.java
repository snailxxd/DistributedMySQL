package com.example.client;

import java.util.HashMap;
import java.util.Map;

public class RegionCache {

    /**
     * RegionServer 缓存，键值对：table - RegionLocation
     */
    private Map<String, RegionLocation> cache;

    /**
     * 使用哈希 Map 初始化缓存
     */
    public RegionCache() {
        cache = new HashMap<String, RegionLocation>();
    }

    /**
     * 设置缓存内容
     * @param key table 表名
     * @param value RegionLocation
     */
    public void set(String key, RegionLocation value) {
        cache.put(key, value);
        System.out.println("RegionCache set: " + key + " - " + value);
    }

    /**
     * 查找缓存
     * @param key table 表名
     * @return RegionLocation，如果没有找到则返回 null
     */
    public RegionLocation find(String key) {
        return cache.get(key);
    }

}
