package com.wly.lol.model;

import lombok.Data;

@Data
public class ChampionMastery {
    private long championId;   // 英雄ID (例如 54 是石头人)
    private int championLevel; // 熟练度等级 (比如 7级狗牌)
    private long championPoints; // 熟练度点数
}
