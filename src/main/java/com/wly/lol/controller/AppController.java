package com.wly.lol.controller;


import com.wly.lol.model.CurrentGameInfo;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import com.wly.lol.service.LolApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lol")
@RequiredArgsConstructor//自动生成构造函数注入Server,就不用Autowire了
public class AppController {
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

}
