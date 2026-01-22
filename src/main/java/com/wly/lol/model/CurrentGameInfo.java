package com.wly.lol.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import java.util.List;

@Data
public class CurrentGameInfo {
    private long gameId;
    private String gameMode; // "CLASSIC", "ARAM"
    private long gameStartTime; // 游戏开始时间
    private List<BannedChampion> bannedChampions; // 禁用列表
    private List<CurrentGameParticipant> participants; // 玩家列表

    @Data
    public static class BannedChampion {
        private int championId;
        private int teamId; // 100或200
        private int pickTurn;
    }

    @Data
    public static class CurrentGameParticipant {
        // --- 核心字段适配 ---

        // Session 接口里通常叫 "name" 或 "gameName"，Spectator 里叫 "summonerName"
        @JSONField(alternateNames = {"name", "gameName", "summonerName", "internalName"})
        private String summonerName;

        // Session 接口里 ID 是一样的，叫 summonerId
        @JSONField(alternateNames = {"summonerId", "accountId"})
        private long summonerId;

        // 英雄 ID
        @JSONField(alternateNames = {"championId", "characterId"})
        private long championId;

        // 队伍 ID (100 或 200)
        @JSONField(alternateNames = {"teamId"})
        private long teamId;

        // --- 技能适配 (Session 接口的大坑) ---
        // 注意：在 Session 接口中，技能有时候不叫 spell1Id，而是藏在其它对象里
        // 但大部分情况，FastJson 的智能匹配能搞定，我们先加上别名试试
        @JSONField(alternateNames = {"spell1Id", "summonerSpell1"})
        private long spell1Id;

        @JSONField(alternateNames = {"spell2Id", "summonerSpell2"})
        private long spell2Id;

        // --- 符文 (Session 接口可能没有这个数据) ---
        // 如果 Session 没返回符文，这俩就是 0，这是正常的，不用强求
        @JSONField(alternateNames = {"perkStyle", "primaryRune"})
        private long perkStyle;

        @JSONField(alternateNames = {"perkSubStyle", "secondaryRune"})
        private long perkSubStyle;
    }
}