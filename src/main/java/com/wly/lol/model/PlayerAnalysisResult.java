package com.wly.lol.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAnalysisResult {
    // 基础信息
    private String name;
    private String tag;
    private int level;
    private String avatarUrl;

    // 排位信息
    private int teamId;
    private String rankTier;
    private int leaguePoints;
    private String seasonStats; // 这里我们存的是"近期胜率"

    // 评分核心
    private String scoreTitle;     // 称号
    private double kda;
    private String kdaDescription; // 备注原始 KDA
    private double liverHours;

    // 总数据 (用于前端可能的其他展示)
    private int totalKills;
    private int totalDeaths;
    private int totalAssists;

    // 最近战绩列表 (只存最近10场)
    private List<MatchBrief> recentMatches;

    // 静态内部类：直接定义在这里，不需要额外的 vo 包
    // 这种写法在不想创建太多文件时非常常用
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchBrief {
        private long gameId;        // 对局ID
        private String championUrl; // 英雄头像
        private boolean isWin;      // 输赢
        private String kdaStr;      // "10/2/5"
        private String gameMode;    // 模式
    }
}