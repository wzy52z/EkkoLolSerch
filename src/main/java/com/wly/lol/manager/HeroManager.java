package com.wly.lol.manager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HeroManager {

    private static final String HERO_LIST_URL = "https://game.gtimg.cn/images/lol/act/img/js/heroList/hero_list.js";

    // 【关键】定义本地存储的文件路径 (当前项目目录下的 data 文件夹)
    private static final String CACHE_DIR = "data";
    private static final String CACHE_FILE = "data/hero_cache.json";

    // 内存缓存
    private Map<Long, String> heroMap = new ConcurrentHashMap<>();

    private final OkHttpClient httpClient = new OkHttpClient();

    @PostConstruct
    public void init() {
        // 1. 优先尝试从本地文件加载
        boolean loadSuccess = loadFromLocalFile();

        // 2. 如果本地没数据（第一次运行），或者读取失败，则联网下载
        if (!loadSuccess) {
            log.info("本地缓存不存在，准备联网下载...");
            downloadAndSave();
        } else {
            log.info("已从本地文件加载 {} 个英雄数据，跳过下载。", heroMap.size());
        }
    }

    /**
     * 方式一：从本地 JSON 文件读取
     */
    private boolean loadFromLocalFile() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) {
            return false; // 文件不存在
        }

        try {
            log.info("正在读取本地缓存文件: {}", file.getAbsolutePath());
            // 读取文件内容
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            // 反序列化为 Map (这里需要用 TypeReference 指定类型)
            Map<Long, String> localData = JSON.parseObject(json, new TypeReference<Map<Long, String>>(){});

            if (localData != null && !localData.isEmpty()) {
                heroMap.putAll(localData);
                return true;
            }
        } catch (Exception e) {
            log.warn("本地缓存文件损坏或格式错误: {}", e.getMessage());
            // 如果读取出错，删除坏文件，强制重新下载
            file.delete();
        }
        return false;
    }

    /**
     * 方式二：联网下载并保存到本地
     */
    private void downloadAndSave() {
        log.info("正在从腾讯官网下载最新英雄列表...");
        Request request = new Request.Builder().url(HERO_LIST_URL).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();

                // 解析逻辑 (和你原来一样)
                JSONObject root = JSON.parseObject(json);
                JSONArray heroes = root.getJSONArray("hero");

                for (int i = 0; i < heroes.size(); i++) {
                    JSONObject hero = heroes.getJSONObject(i);
                    long heroId = Long.parseLong(hero.getString("heroId"));
                    // 注意：game.gtimg.cn 里，name=卡牌大师, title=崔斯特
                    // 通常为了显示清晰，我们可能更习惯用 title (崔斯特) 或者 name (卡牌)
                    // 这里我两个都存一下，方便你选
                    String name = hero.getString("name");   // 称号: 黑暗之女
                    String title = hero.getString("title"); // 名字: 安妮
                    String finalName = title; // 这里选择显示 "安妮"

                    heroMap.put(heroId, finalName);
                }

                log.info("下载完成，共 {} 个英雄。", heroMap.size());

                // 【核心步骤】下载完后，立刻存入本地文件
                saveToLocalFile();

            }
        } catch (Exception e) {
            log.error("联网下载失败: {}", e.getMessage());
        }
    }

    /**
     * 将内存数据写入本地文件
     */
    private void saveToLocalFile() {
        try {
            // 1. 确保目录存在
            File dir = new File(CACHE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. 将 Map 转为漂亮的 JSON 字符串 (PrettyFormat)
            String jsonString = JSON.toJSONString(heroMap);

            // 3. 写入文件
            Path path = Paths.get(CACHE_FILE);
            Files.writeString(path, jsonString, StandardCharsets.UTF_8);

            log.info("已将英雄数据缓存到本地: {}", path.toAbsolutePath());

        } catch (IOException e) {
            log.error("保存本地缓存失败", e);
        }
    }

    public String getHeroName(long championId) {
        if (championId == 0) return "未选择";
        return heroMap.getOrDefault(championId, "未知(" + championId + ")");
    }
}