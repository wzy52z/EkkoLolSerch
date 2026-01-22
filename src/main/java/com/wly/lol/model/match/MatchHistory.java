package com.wly.lol.model.match;

import lombok.Data;
import java.util.List;
/**
 * LCU 的战绩数据设计有一个逻辑，初学者很容易晕，我给你理一下：
 *
 * 先找人 (ParticipantIdentity)： 在 participantIdentities 列表里，通过你的 summonerId 找到你对应的 participantId (比如是 3 号玩家)。
 *
 * 再找数据 (MatchParticipant)： 拿着这个 3 号，去 participants 列表里找 participantId == 3 的那条数据。
 *
 * 最后看结果 (MatchStats)： 在那条数据里的 stats 对象里，才能看到你是赢了还是输了 (win)，以及 KDA。
 */

@Data
public class MatchHistory {

    // 对应 JSON 最外层的 "games" 对象
    private GamesWrapper games;

    @Data
    public static class GamesWrapper {
        // 对应 JSON 里第二层的 "games" 数组
        private List<MatchGame> games;
        // 这里的 gameIndexBegin/End 可以不写，暂时用不到
    }

    @Data
    public static class MatchGame {
        private long gameId;
        private String gameMode;   // 游戏模式 (ARAM, CLASSIC)
        private long gameCreation; // 游戏开始时间戳

        // 【关键】游戏时长 (秒)，用于计算“肝度”
        private long gameDuration;

        // 【关键】参与者详细数据 (装备、KDA、胜负都在这里)
        private List<MatchParticipant> participants;

        // 【关键】身份对应表 (用来把 召唤师ID 翻译成 participantId)
        private List<ParticipantIdentity> participantIdentities;
    }

    // --- 下面是辅助内部类 ---

    /**
     * 身份映射类
     * 作用：告诉你 "Faker" 是 1号位还是 5号位
     */
    @Data
    public static class ParticipantIdentity {
        private int participantId; // 例如: 1
        private Player player;
    }

    @Data
    public static class Player {
        private long summonerId;   // 我们用这个来匹配目标玩家
        private String summonerName;
        private String gameName;   // 新版 ID
        private String tagLine;
    }

    /**
     * 游戏内数据类
     * 作用：记录 1号位 买了什么装备，杀了多少人
     */
    @Data
    public static class MatchParticipant {
        private int participantId; // 例如: 1
        private int teamId;        // 100(蓝方) 或 200(红方)
        private int championId;    // 英雄ID
        private MatchStats stats;  // 核心统计数据
    }

    /**
     * 统计数据类
     */
    @Data
    public static class MatchStats {
        private boolean win; // true=胜利
        private int kills;
        private int deaths;
        private int assists;

        // 你还可以加这些字段来丰富功能：
        // private long totalDamageDealtToChampions; // 伤害
        // private int goldEarned; // 经济
        // private int item0; // 装备栏0
        // ...
    }
}