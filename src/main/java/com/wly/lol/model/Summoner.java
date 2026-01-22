package com.wly.lol.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class Summoner {
    // --- 核心身份标识 ---
    private Long accountId;     // 账号ID
    private Long summonerId;    // 【关键】召唤师ID (查熟练度用)
    private String puuid;       // 【关键】全局唯一ID (查战绩、查段位用)

    // --- 显示信息 ---
    @JSONField(alternateNames = {"displayName", "name"})
    private String displayName; // 旧版昵称 (可能为空)
    @JSONField(alternateNames = {"gameName"})
    private String gameName;    // 新版 Riot ID 名字 (例如 "Faker")

    @JSONField(alternateNames = {"tagLine"})
    private String tagLine;     // 新版 Tag (例如 "KR1")

    // --- 个人资料 ---
    private int summonerLevel;  // 等级
    private int profileIconId;  // 头像 ID

    // --- 经验值相关 (可选) ---
    private long xpSinceLastLevel;
    private long xpUntilNextLevel;
    private int percentCompleteForNextLevel;

    /**
     * 获取完整的显示名称
     * 如果是新版 ID，返回 "Name#Tag"；如果是旧版，返回 displayName
     */
    public String getNiceName() {
        // 优先返回 "Faker#KR1" 这种格式
        if (gameName != null && !gameName.isEmpty()) {
            if (tagLine != null && !tagLine.isEmpty()) {
                return gameName + "#" + tagLine;
            }
            return gameName;
        }
        // 如果新版名字没有，再降级去拿旧版名字
        return displayName != null ? displayName : "未知玩家";
    }
}