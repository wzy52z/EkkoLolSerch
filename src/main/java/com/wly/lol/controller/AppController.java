package com.wly.lol.controller;


import com.wly.lol.context.GameContext;
import com.wly.lol.model.CurrentGameInfo;
import com.wly.lol.model.PlayerAnalysisResult;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import com.wly.lol.service.GameAnalysisService;
import com.wly.lol.service.LolApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lol")
@RequiredArgsConstructor//自动生成构造函数注入Server,就不用Autowire了
public class AppController {
    private final GameAnalysisService gameAnalysisService;
    private final GameContext gameContext;
    private final LolApiService lolApiService;
    /**
     * 获取当前登录用户信息
     * 访问地址: GET http://localhost:8080/api/lol/me
     */
    @GetMapping("/me")
    public Summoner getMyInfo() {
        return lolApiService.getCurrentSummoner();
    }

    /**
     * 搜索召唤师
     * 访问地址: GET http://localhost:8080/api/lol/search?name=Faker
     */
    @GetMapping("/search")
    public Summoner searchSummoner(@RequestParam String name) {
        return lolApiService.getSummonerByName(name);
    }

    /**
     * 获取战绩
     * 访问地址: GET http://localhost:8080/api/lol/history/{puuid}
     */
    @GetMapping("/history/{puuid}")
    public MatchHistory getHistory(@PathVariable String puuid) {
        return lolApiService.getMatchHistory(puuid);
    }

    /**
     * 获取段位
     * 访问地址: GET http://localhost:8080/api/lol/rank/{puuid}
     */
    @GetMapping("/rank/{puuid}")
    public RankInfo getRank(@PathVariable String puuid) {
        return lolApiService.getRankInfo(puuid);
    }

    /**
     * 获取当前对局（实时）
     * 访问地址: GET http://localhost:8080/api/lol/game/current
     */
    @GetMapping("/game/current")
    public CurrentGameInfo getCurrentGame() {
        return lolApiService.getCurrentGameInfo();
    }
    /**
     * 1. 实时监控接口
     * 返回：GameContext 里存好的 10 个人的分析结果 (带称号、评分)
     * 地址：GET http://localhost:8080/api/lol/game/live
     */
    @GetMapping("/game/live")
    public List<PlayerAnalysisResult> getLiveGameAnalysis() {
        return gameContext.getPlayers();
    }

    /**
     * 2. 手动查询分析接口
     * 输入：Faker#KR1
     * 返回：单个人的分析结果 (带称号、评分)
     * 地址：GET http://localhost:8080/api/lol/analyze?name=Faker#KR1
     */
    @GetMapping("/analyze")
    public PlayerAnalysisResult analyzeOnePlayer(@RequestParam String name) {
        return gameAnalysisService.analyzePlayer(name);
    }

}
