package com.wly.lol.service;

import com.wly.lol.manager.HeroManager;
import com.wly.lol.model.PlayerAnalysisResult;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameAnalysisService {

    private final LolApiService apiService;
    private final HeroManager heroManager;

    /**
     * 入口1：手动查询 (名字)
     */
    public PlayerAnalysisResult analyzePlayer(String nameTag) {
        if (nameTag != null) {
            // 简单暴力清洗：只留汉字、字母、数字、#
            StringBuilder sb = new StringBuilder();
            for (char c : nameTag.toCharArray()) {
                if ((c >= 0x4e00 && c <= 0x9fa5) || Character.isLetterOrDigit(c) || c == '#') {
                    sb.append(c);
                }
            }
            nameTag = sb.toString();
        }
        Summoner summoner = apiService.getSummonerByName(nameTag);
        if (summoner == null) throw new RuntimeException("未找到玩家: " + nameTag);
        return coreAnalyze(summoner, 0);
    }

    /**
     * 入口2：实时监控 (ID)
     */
    public PlayerAnalysisResult analyzeBySummonerId(long summonerId, long championId) {
        Summoner summoner = apiService.getSummonerById(summonerId);
        if (summoner == null) return null;
        return coreAnalyze(summoner, championId);
    }

    /**
     * 🧠 核心分析逻辑
     */
    private PlayerAnalysisResult coreAnalyze(Summoner summoner, long currentChampionId) {
        String puuid = summoner.getPuuid();

        // 1. 获取段位
        RankInfo rankInfo = apiService.getRankInfo(puuid);
        String rankTier = "Unranked";
        if (rankInfo != null && rankInfo.getSoloQueue() != null) {
            RankInfo.RankItem solo = rankInfo.getSoloQueue();
            rankTier = solo.getTier() + " " + solo.getDivision();
        }

        // 2. 获取并分析历史战绩
        MatchHistory history = apiService.getMatchHistory(puuid);
        int analyzeCount = 0;
        int wins = 0;
        int k = 0, d = 0, a = 0;
        long totalDuration = 0;

        // 准备列表存最近战绩
        List<PlayerAnalysisResult.MatchBrief> recentMatches = new ArrayList<>();

        if (history != null && history.getGames() != null && history.getGames().getGames() != null) {
            List<MatchHistory.MatchGame> games = history.getGames().getGames();
            analyzeCount = Math.min(games.size(), 20); // 分析最近20场数据

            for (int i = 0; i < analyzeCount; i++) {
                MatchHistory.MatchGame game = games.get(i);
                totalDuration += game.getGameDuration();

                MatchHistory.MatchStats stats = getParticipantBySummonerId(game, summoner.getSummonerId());
                if (stats != null) {
                    // 累加总数据
                    if (stats.isWin()) wins++;
                    k += stats.getKills();
                    d += stats.getDeaths();
                    a += stats.getAssists();

                    // 🔥【新增】提取前 10 场战绩详情用于前端展示
                    if (recentMatches.size() < 10) {
                        // 使用 CommunityDragon 的 CDN 获取英雄头像
                        String champIcon = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/" + stats.getChampionId() + ".png";

                        // 构建内部类对象
                        recentMatches.add(PlayerAnalysisResult.MatchBrief.builder()
                                .gameId(game.getGameId())
                                .isWin(stats.isWin())
                                .kdaStr(stats.getKills() + "/" + stats.getDeaths() + "/" + stats.getAssists())
                                .championUrl(champIcon)
                                .gameMode(game.getGameMode())
                                .build());
                    }
                }
            }
        }

        // 3. 🔥【应用你的新算法】🔥
        // 逻辑：(K+A) / D * 3，死亡0算作1
        long safeDeaths = (d == 0) ? 1 : d;
        double rawKda = (double) (k + a) / safeDeaths; // 原始 KDA
        double myScore = rawKda * 3.0;                 // 你的 x3 得分

        double hours = (double) totalDuration / 3600.0;

        // 根据新分数获取称号
        String title = getEvolutionTitle(myScore);

        // 计算近期胜率字符串
        String seasonStats;
        if (analyzeCount > 0) {
            int winRate = (wins * 100) / analyzeCount;
            seasonStats = String.format("近%d场 %d胜 %d%%", analyzeCount, wins, winRate);
        } else {
            seasonStats = "近期无战绩";
        }

        // 获取当前选择的英雄名
        String heroName = (currentChampionId == 0) ? "选人中..." : heroManager.getHeroName(currentChampionId);

        // 4. 构建返回
        return PlayerAnalysisResult.builder()
                .name(summoner.getGameName())
                .tag(summoner.getTagLine())
                .level(summoner.getSummonerLevel())
                .avatarUrl("https://wegame.gtimg.com/g.26-r.c2d3c/helper/lol/asis/images/resources/usericon/" + summoner.getProfileIconId() + ".png")
                .rankTier(rankTier)
                .seasonStats(seasonStats)
                .scoreTitle(title)
                .kda(Double.parseDouble(String.format("%.1f", myScore))) // 显示 x3 后的分
                .kdaDescription(String.format("原始KDA: %.1f", rawKda))   // 备注原始分
                .liverHours(Double.parseDouble(String.format("%.1f", hours)))
                .recentMatches(recentMatches) // 填入战绩列表
                .totalKills(k).totalDeaths(d).totalAssists(a)
                .build();
    }

    /**
     * 🔥 评级门槛调整 (适配 x3 后的分数)
     */
    private String getEvolutionTitle(double score) {
        // 原始 1.5 -> 新分 4.5
        if (score < 4.5) return "🦠草履虫";
            // 原始 3.0 -> 新分 9.0
        else if (score < 9.0) return "🪱蚯蚓";
            // 原始 5.0 -> 新分 15.0
        else if (score < 15.0) return "🙋普通人类";
            // 原始 8.0 -> 新分 24.0
        else if (score < 24.0) return "🐯剑齿虎";
            // > 24.0
        else return "👼通天代";
    }

    // --- 辅助方法：从单局提取数据 ---
    private MatchHistory.MatchStats getParticipantBySummonerId(MatchHistory.MatchGame game, long summonerId) {
        if (game.getParticipantIdentities() == null) return null;
        int participantId = -1;
        // 1. 找ID
        for (MatchHistory.ParticipantIdentity identity : game.getParticipantIdentities()) {
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