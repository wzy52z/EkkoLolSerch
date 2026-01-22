package com.wly.lol.task;

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

import java.util.List;
import java.util.Optional;

@Slf4j

@Component // 注册为组件
@RequiredArgsConstructor // 【大厂习惯】使用构造器注入，Spring 会自动把 service 传进来
public class GameMonitorTask {

    // 这里不需要 new，Spring 会自动注入 LcuServiceImpl
    private final LolApiService service;

    private final HeroManager heroManager;

    private String lastPhase = "None";

    // 【关键点 3】定时任务
    // initialDelay = 启动后延迟1秒开始
    // fixedRate = 每隔 3000 毫秒执行一次
    @Scheduled(initialDelay = 1000, fixedRate = 3000)
    public void checkGameStatus() {
        try {
            String currentPhase = service.getGameFlowPhase();

            if (!currentPhase.equals(lastPhase)) {
                log.info("⚡ 状态变更为: {}", currentPhase);
                handlePhaseChange(currentPhase);
                lastPhase = currentPhase;
            }
        } catch (Exception e) {
            // 忽略连接异常
        }
    }
    //获取对局数据
    private void handlePhaseChange(String phase) {
        if ("InProgress".equals(phase)) {
            log.info("游戏状态已变更，正在等待对局数据生成...");

            // 开启一个异步线程去重试，避免阻塞主监控线程
            new Thread(() -> {
                CurrentGameInfo game = null;

                // 【重试策略】最多尝试 10 次，每次间隔 2 秒
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(2000); // 先等 2 秒
                        log.info("第 {} 次尝试获取对局数据...", i + 1);

                        game = service.getCurrentGameInfo();


                        if (game != null) {
                            log.info("获取成功！对局信息: {}", game);
                            for(CurrentGameInfo.CurrentGameParticipant p : game.getParticipants()) {
                                analyzePlayer(p);
                            }
                            // TODO: 这里可以把数据存起来，或者发给前端
                            break; // 拿到了就跳出循环
                        }
                    } catch (Exception e) {
                        // 忽略过程中的报错
                    }
                }

                if (game == null) {
                    log.warn("尝试 10 次后仍未获取到对局数据（可能是人机或训练模式数据延迟，也可能游戏崩了）");
                }
            }).start();
        }
    }

    private void analyzePlayer(CurrentGameInfo.CurrentGameParticipant p) {
    try {
        long heroId = p.getChampionId();
        long summonerId = p.getSummonerId();
        //映射拿到英雄名字
        String heroName = heroManager.getHeroName(heroId);
        Summoner summoner = service.getSummonerById(summonerId);
        if (summoner == null) {return;}
        //获取召唤师名字和查战绩的id
        String name = summoner.getGameName();
        String puuid = summoner.getPuuid();
        // 4. 查段位
        RankInfo rankInfo = service.getRankInfo(puuid);

        String rankStr = "无段位";
        int lp = 0;
        String winRate = "0%";
        String seasonStats = "0场";
        if (rankInfo != null) {
            // 直接调用我们在 Model 里写的便捷方法
            RankInfo.RankItem solo = rankInfo.getSoloQueue();

            if (solo != null && solo.getTier() != null) {
                rankStr = solo.getTier() + " " + solo.getDivision();
                int totalGames = solo.getWins()+solo.getLosses();
                lp = solo.getLeaguePoints();

                winRate = solo.getWinRate(); // 顺便把胜率也拿到了
                seasonStats = String.format("%场(%d%%)", totalGames, winRate);
            }
        }
        MatchHistory history = service.getMatchHistory(puuid);
        String recentStats = analyzeRecentHistory(history,summonerId);
        log.info("玩家: [{} - {}] | 段位: {} | 赛季: {}({}) | {}",
                name, heroName, rankStr, seasonStats, winRate, recentStats);
    } catch (Exception e) {
        log.error("分析玩家出错: {}", e.getMessage());
    }
    }
    /**
     *  进阶分析：计算KDA和肝度
     */
    private String analyzeRecentHistory(MatchHistory history, long targetSummonerId) {
        // 数据校验
        if (history == null || history.getGames() == null || history.getGames().getGames() == null) {
            return "无战绩";
        }
        List<MatchHistory.MatchGame> games = history.getGames().getGames();
        if (games.isEmpty()) return "无战绩";

        // 只分析最近 20 场
        int analyzeCount = Math.min(games.size(), 20);

        int wins = 0;
        int totalKills = 0, totalDeaths = 0, totalAssists = 0;
        long totalDurationSeconds = 0;

        for (int i = 0; i < analyzeCount; i++) {
            MatchHistory.MatchGame game = games.get(i);
            totalDurationSeconds += game.getGameDuration();

            // 提取该玩家在这局的数据
            MatchHistory.MatchStats stats = getStatsBySummonerId(game, targetSummonerId);

            if (stats != null) {
                if (stats.isWin()) wins++;
                totalKills += stats.getKills();
                totalDeaths += stats.getDeaths();
                totalAssists += stats.getAssists();
            }
        }

        // --- 计算结果 ---

        // 1. KDA
        double kda = (double) (totalKills + totalAssists) / Math.max(1, totalDeaths);

        // 2. 游戏时长 (小时)
        double totalHours = (double) totalDurationSeconds / 3600.0;

        // 3. 获得称号 (生物进化论)
        String title = getEvolutionTitle(kda);

        // 4. 格式化输出
        // 例子: "[剑齿虎] 近20场12胜(KDA 6.5) | 肝度: 5.2h"
        return String.format("[%s] 近%d场%d胜(KDA %.1f) | 游戏时间: %.1fh",
                title, analyzeCount, wins, kda, totalHours);
    }

    /**
     * 🧬 生物进化论评级系统
     */
    private String getEvolutionTitle(double kda) {
        if (kda < 1.5) return "草履虫";
        else if (kda < 3.0) return "蚯蚓";
        else if (kda < 5.0) return "初具人形";
        else if (kda < 8.0) return "小代";
        else return "通天代";
    }

    /**
     * 🛠️ 辅助方法：从单局游戏中提取指定玩家的战绩数据
     */
    private MatchHistory.MatchStats getStatsBySummonerId(MatchHistory.MatchGame game, long summonerId) {
        if (game.getParticipantIdentities() == null || game.getParticipants() == null) return null;

        // 1. 先通过 summonerId 找到 participantId (比如: Faker 是 1号位)
        int participantId = -1;
        for (MatchHistory.ParticipantIdentity identity : game.getParticipantIdentities()) {
            if (identity.getPlayer().getSummonerId() == summonerId) {
                participantId = identity.getParticipantId();
                break;
            }
        }

        // 2. 再通过 participantId 找到 stats (比如: 1号位的数据是 5/0/3)
        if (participantId != -1) {
            for (MatchHistory.MatchParticipant p : game.getParticipants()) {
                if (p.getParticipantId() == participantId) {
                    return p.getStats();
                }
            }
        }
        return null;
    }
}