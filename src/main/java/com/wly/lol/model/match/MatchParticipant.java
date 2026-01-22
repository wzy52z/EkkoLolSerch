package com.wly.lol.model.match;

import lombok.Data;

@Data
public  class MatchParticipant {
    private int participantId;
    private int teamId;      // 100 是蓝方，200 是红方
    private int championId;  // 英雄 ID

    // 核心数据都在 stats 里
    private MatchStats stats;

    // 甚至还有 spell1Id (召唤师技能1), spell2Id 等...
}
