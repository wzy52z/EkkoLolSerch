package com.wly.lol.service;

import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import com.wly.lol.model.PlayerAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
//如果在未来的某一天，你发现真的需要第二个实现类了，IDE（IntelliJ IDEA）提供了一键重构功能：
//
//右键类名 -> Refactor -> Extract Interface
//
//它会自动帮你把类变成接口+实现。  
@Slf4j
@Service
@RequiredArgsConstructor
public class GameAnalysisService {

    private final LolApiService apiService;

    /**
     * 🔍 核心功能：根据名字查询并分析玩家
     * @param nameTag 格式可以是 "Faker" 或 "Faker#KR1"
     */
    public PlayerAnalysisResult analyzePlayer(String nameTag) {
        // 1. 查找召唤师
        Summoner summoner = apiService.getSummonerByName(nameTag);
        if (summoner == null) {
            throw new RuntimeException("未找到玩家: " + nameTag + " (请确认游戏已登录且能搜到该玩家)");
        }

        String puuid = summoner.getPuuid();

        // 2. 获取段位信息
        RankInfo rankInfo = apiService.getRankInfo(puuid);
        String rankTier = "Unranked";
        int lp = 0;
        String seasonStats = "0场";

        if (rankInfo != null && rankInfo.getSoloQueue() != null) {
            RankInfo.RankItem solo = rankInfo.getSoloQueue();
            if (solo.getTier() != null) {
                rankTier = solo.getTier() + " " + solo.getDivision();
                lp = solo.getLeaguePoints();
                int total = solo.getWins() + solo.getLosses();
                seasonStats = String.format("%d场 (%s胜率)", total, solo.getWinRate());
            }
        }

        // 3. 获取并计算历史战绩
        MatchHistory history = apiService.getMatchHistory(puuid);

        int analyzeCount = 0;
        int wins = 0;
        int k = 0, d = 0, a = 0;
        long totalDuration = 0;

        if (history != null && history.getGames() != null && history.getGames().getGames() != null) {
            List<MatchHistory.MatchGame> games = history.getGames().getGames();
            analyzeCount = Math.min(games.size(), 20); // 只看最近20场

            for (int i = 0; i < analyzeCount; i++) {
                MatchHistory.MatchGame game = games.get(i);
                totalDuration += game.getGameDuration();

                // 提取该玩家在这局的数据
                MatchHistory.MatchStats stats = getStatsBySummonerId(game, summoner.getSummonerId());
                if (stats != null) {
                    if (stats.isWin()) wins++;
                    k += stats.getKills();
                    d += stats.getDeaths();
                    a += stats.getAssists();
                }
            }
        }

        // --- 计算数值 ---
        double kda = (double) (k + a) / Math.max(1, d);
        double hours = (double) totalDuration / 3600.0;
        String title = getEvolutionTitle(kda); // 获取生物评级

        // 4. 构建并返回结果对象
        return PlayerAnalysisResult.builder()
                .name(summoner.getGameName())
                .tag(summoner.getTagLine())
                .level(summoner.getSummonerLevel())
                // 拼接官方头像 URL
                .avatarUrl("https://wegame.gtimg.com/g.26-r.c2d3c/helper/lol/asis/images/resources/usericon/" + summoner.getProfileIconId() + ".png")
                .rankTier(rankTier)
                .leaguePoints(lp)
                .seasonStats(seasonStats)
                .scoreTitle(title)
                .kda(Double.parseDouble(String.format("%.1f", kda)))
                .kdaDescription(String.format("近%d场%d胜", analyzeCount, wins))
                .liverHours(Double.parseDouble(String.format("%.1f", hours)))
                .totalKills(k).totalDeaths(d).totalAssists(a)
                .build();
    }

    // --- 🧬 评级逻辑 ---
    private String getEvolutionTitle(double kda) {
        if (kda < 1.5) return "🦠草履虫";
        else if (kda < 3.0) return "🚙炮车兵";
        else if (kda < 5.0) return "🙋普通人类";
        else if (kda < 8.0) return "🐯剑齿虎";
        else return "👼通天代";
    }

    // --- 🛠️ 辅助方法：从单局提取数据 ---
    private MatchHistory.MatchStats getStatsBySummonerId(MatchHistory.MatchGame game, long summonerId) {
        if (game.getParticipantIdentities() == null) return null;
        int participantId = -1;

        // 1. 找ID
        for (var identity : game.getParticipantIdentities()) {
            if (identity.getPlayer().getSummonerId() == summonerId) {
                participantId = identity.getParticipantId();
                break;
            }
        }

        // 2. 找数据
        if (participantId != -1 && game.getParticipants() != null) {
            for (var p : game.getParticipants()) {
                if (p.getParticipantId() == participantId) {
                    return p.getStats();
                }
            }
        }
        return null;
    }
}