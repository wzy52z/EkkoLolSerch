package com.wly.lol.model.match;

import lombok.Data;

import java.util.List;

@Data
public class MatchGame {
    private long gameId;
    private String gameMode; // 比如 "CLASSIC", "ARAM"
    private long gameCreation; // 游戏时间戳
    private List<MatchParticipant> participants; // 参与者信息(包含赢没赢、KDA)

}
