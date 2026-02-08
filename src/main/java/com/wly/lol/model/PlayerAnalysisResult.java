package com.wly.lol.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 专门用于返回给前端的视图对象 (View Object)
 */
@Data
@Builder
public class PlayerAnalysisResult {
    // --- 👤 基础信息 ---
    private String name;        // 游戏名 (例如: Faker)
    private String tag;         // Tag (例如: KR1)
    private String avatarUrl;   // 头像图片链接
    private int level;          // 召唤师等级

    // --- 🏆 段位信息 ---
    private String rankTier;    // 段位 (例如: GOLD IV)
    private int leaguePoints;   // 胜点 (例如: 56)
    private String seasonStats; // 赛季总场次描述 (例如: "200场 (55%胜率)")

    // --- 🧬 核心分析数据 (生物进化论) ---
    private String scoreTitle;      // 评级称号 (例如: "🐯剑齿虎")
    private double kda;             // KDA数值 (例如: 4.5)
    private String kdaDescription;  // KDA描述 (例如: "近20场12胜")
    private double liverHours;      // 肝度/游戏时长 (例如: 5.2)

    // --- 📊 原始数据 (留给前端画图用) ---
    private int totalKills;
    private int totalDeaths;
    private int totalAssists;
}