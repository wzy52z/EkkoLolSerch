package com.wly.lol.model;
import lombok.Data;

import java.util.List;

@Data
public class RankInfo {

    // 对应 JSON 里的 "queues" 数组
    private List<RankItem> queues;

    /**
     * 内部类：代表具体的某一种队列（单排/灵活排/云顶）
     */
    @Data
    public static class RankItem {
        private String queueType;    // 类型: RANKED_SOLO_5x5 (单双排), RANKED_FLEX_SR (灵活排)
        private String tier;         // 大段位: GOLD, SILVER, PLATINUM
        private String division;     // 小段位: I, II, III, IV
        private int leaguePoints;    // 胜点
        private int wins;            // 胜场
        private int losses;          // 负场

        // 计算胜率的小工具
        public String getWinRate() {
            int total = wins + losses;
            if (total == 0) return "0%";
            return (wins * 100 / total) + "%";
        }
    }

    /**
     * 【便捷方法】直接获取单双排数据
     * 这样你在 Task 里就不用写循环了，直接调这个方法
     */
    public RankItem getSoloQueue() {
        if (queues == null) return null;
        for (RankItem item : queues) {
            if ("RANKED_SOLO_5x5".equals(item.getQueueType())) {
                return item;
            }
        }
        return null; // 没打单双排
    }
}