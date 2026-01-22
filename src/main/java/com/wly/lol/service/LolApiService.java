package com.wly.lol.service;

import com.wly.lol.model.CurrentGameInfo;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;

public interface LolApiService {
    // 获取当前召唤师信息 (返回 JSON 字符串或对象)
    Summoner getCurrentSummoner();

    // 获取常用英雄
    Summoner getSummonerByName(String name);

    Summoner getSummonerById(long summonerId);
    // 获取历史战绩
    MatchHistory getMatchHistory(String puuid);

    RankInfo getRankInfo(String puuid);

    CurrentGameInfo getCurrentGameInfo();

    String getGameFlowPhase();
}
