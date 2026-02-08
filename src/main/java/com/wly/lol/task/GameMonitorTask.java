package com.wly.lol.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wly.lol.manager.HeroManager;
import com.wly.lol.model.CurrentGameInfo;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import com.wly.lol.service.LolApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameMonitorTask {

    private final LolApiService service;
    private final HeroManager heroManager;

    private String lastPhase = "None";
    // 用来防止同一局游戏重复刷屏，存储已经分析过的 PUUID
    private Set<String> analyzedPuuids = new HashSet<>();

    @Scheduled(initialDelay = 1000, fixedRate = 2000)
    //定时检查游戏状态
    public void checkGameStatus() {
        try {
            String currentPhase = service.getGameFlowPhase();

            // 如果状态变了，或者进入了新游戏，重置缓存
            if (!currentPhase.equals(lastPhase)) {
                log.info("⚡ 状态变更为: {}", currentPhase);
                if ("Lobby".equals(currentPhase) || "None".equals(currentPhase)) {
                    analyzedPuuids.clear(); // 回到大厅清空缓存
                }
                handlePhaseChange(currentPhase);
                lastPhase = currentPhase;
            }
        } catch (Exception e) {
            // 忽略连接异常
        }
    }

    private void handlePhaseChange(String phase) {
        // --- 阶段一：选人 (能看队友，能破除队友匿名) ---
        if ("ChampSelect".equals(phase)) {
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // 等一下数据同步
                    analyzeChampSelect();
                } catch (Exception e) {
                    log.error("选人分析出错", e);
                }
            }).start();
        }

        // --- 阶段二：游戏中 (能看敌方，能兜底队友) ---
        else if ("InProgress".equals(phase)) {
            new Thread(() -> {
                try {
                    log.info("🎮 游戏加载中，正在尝试获取全员(含敌方)数据...");
                    Thread.sleep(3000); // 进游戏多等一会，防止404
                    analyzeInProgress();
                } catch (Exception e) {
                    log.error("对局分析出错", e);
                }
            }).start();
        }
    }

    /**
     * 🕵️‍♀️ 选人阶段分析逻辑 (Session接口)
     */
    private void analyzeChampSelect() {
        String json = service.getChampSelectSession();
        if (json == null) return;

        JSONObject root = JSONObject.parseObject(json);
        JSONArray myTeam = root.getJSONArray("myTeam");

        if (myTeam != null) {
            log.info("[选人阶段] 获取到 {} 名队友信息", myTeam.size());
            for (int i = 0; i < myTeam.size(); i++) {
                try {
                    JSONObject player = myTeam.getJSONObject(i);
                    long summonerId = player.getLongValue("summonerId");
                    long championId = player.getLongValue("championId");

                    if (summonerId == 0) continue; // 过滤掉异常数据

                    // 调用统一分析方法
                    analyzeSinglePlayer(summonerId, championId, "队友(选人)");

                } catch (Exception e) {
                    log.warn(" 分析第 {} 个队友失败: {}", i + 1, e.getMessage());
                }
            }
        }
    }

    /**
     * ⚔️ 游戏中分析逻辑 (ActiveGame接口)
     */
    private void analyzeInProgress() {
        // 尝试重试几次，因为刚进游戏接口可能报404
        CurrentGameInfo game = null;
        for (int i = 0; i < 5; i++) {
            game = service.getCurrentGameInfo();
            if (game != null) break;
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }

        if (game == null) {
            log.warn("❌ 无法获取对局数据 (可能是人机或接口延迟)");
            return;
        }

        log.info("⚔️ [对局开始] 获取到全员 {} 人信息", game.getParticipants().size());

        for (CurrentGameInfo.CurrentGameParticipant p : game.getParticipants()) {
            try {
                // 如果是队友，可能在选人阶段分析过了，这里会通过 Set 去重
                // 如果是敌人，这里是第一次分析
                analyzeSinglePlayer(p.getSummonerId(), p.getChampionId(), "玩家(对局)");
            } catch (Exception e) {
                log.error("❌ 分析某位玩家失败", e);
            }
        }
    }

    /**
     * 💎 统一的核心分析方法
     */
    private void analyzeSinglePlayer(long summonerId, long championId, String sourceTag) {
        // 1. 获取 Summoner 基本信息 (这里是破除匿名的关键)
        // 即使是主播模式，getSummonerById 返回的也是真实信息
        Summoner summoner = service.getSummonerById(summonerId);
        if (summoner == null) return;

        String puuid = summoner.getPuuid();

        // 2. 去重检查 (防止一个人打两次日志)
        if (analyzedPuuids.contains(puuid)) {
            return;
        }
        analyzedPuuids.add(puuid); // 标记已分析

        String name = summoner.getNiceName();
        String heroName = heroManager.getHeroName(championId);

        // 3. 查段位
        RankInfo rankInfo = service.getRankInfo(puuid);
        String rankStr = "无段位";
        if (rankInfo != null && rankInfo.getSoloQueue() != null) {
            rankStr = rankInfo.getSoloQueue().getTier() + " " + rankInfo.getSoloQueue().getDivision();
        }

        // 4. 查历史战绩
        MatchHistory history = service.getMatchHistory(puuid);
        String recentStats = analyzeRecentHistory(history, summonerId);

        // 5. 打印最终结果
        log.info("📊 [{}] {} (英雄: {}) | 段位: {} | {}",
                sourceTag, name, heroName, rankStr, recentStats);
    }

    // --- 你的战绩分析算法 (保持不变) ---
    private String analyzeRecentHistory(MatchHistory history, long targetSummonerId) {
        if (history == null || history.getGames() == null || history.getGames().getGames() == null) {
            return "无战绩";
        }

        List<MatchHistory.MatchGame> games = history.getGames().getGames();
        if (games.isEmpty()) return "无战绩";

        int analyzeCount = Math.min(games.size(), 20);
        int wins = 0;
        int k = 0, d = 0, a = 0;
        long totalDurationSeconds = 0;

        for (int i = 0; i < analyzeCount; i++) {
            MatchHistory.MatchGame game = games.get(i);
            totalDurationSeconds += game.getGameDuration();

            int participantId = -1;
            for (MatchHistory.ParticipantIdentity identity : game.getParticipantIdentities()) {
                if (identity.getPlayer().getSummonerId() == targetSummonerId) {
                    participantId = identity.getParticipantId();
                    break;
                }
            }

            if (participantId != -1) {
                for (MatchHistory.MatchParticipant p : game.getParticipants()) {
                    if (p.getParticipantId() == participantId) {
                        MatchHistory.MatchStats stats = p.getStats();
                        if (stats.isWin()) wins++;
                        k += stats.getKills();
                        d += stats.getDeaths();
                        a += stats.getAssists();
                        break;
                    }
                }
            }
        }

        double kda = (double) (k + a) / Math.max(1, d);
        double totalHours = (double) totalDurationSeconds / 3600.0;

        return String.format("[%s] 近%d场%d胜(KDA %.1f) | 肝度: %.1fh",
                getEvolutionTitle(kda), analyzeCount, wins, kda, totalHours);
    }

    private String getEvolutionTitle(double kda) {
        if (kda < 1.5) return "草履虫";
        else if (kda < 3.0) return "蚯蚓";
        else if (kda < 5.0) return "🙋普通人类";
        else if (kda < 8.0) return "🐯剑齿虎";
        else return "👼通天代";
    }
}