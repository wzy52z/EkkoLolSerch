package com.wly.lol.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wly.lol.context.GameContext;
import com.wly.lol.manager.HeroManager;
import com.wly.lol.model.CurrentGameInfo;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import com.wly.lol.model.PlayerAnalysisResult;
import com.wly.lol.service.GameAnalysisService;
import com.wly.lol.service.LolApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameMonitorTask {

    private final LolApiService service;
    private final GameAnalysisService analysisService;
    private final GameContext gameContext;
    private final GameAnalysisService gameAnalysisService;

    private String lastPhase = "None";



    @Scheduled(initialDelay = 1000, fixedRate = 3000) // 改成3秒一次，减轻一点压力
    public void checkGameStatus() {
        try {
            String currentPhase = service.getGameFlowPhase();

            // 状态变更日志
            if (!currentPhase.equals(lastPhase)) {
                log.info("⚡ 状态变更为: {}", currentPhase);
                if ("Lobby".equals(currentPhase) || "None".equals(currentPhase)) {
                    gameContext.clear(); // 回到大厅清空数据
                }
                lastPhase = currentPhase;
            }

            // --- 核心逻辑：根据状态每轮都执行 ---

            if ("ChampSelect".equals(currentPhase)) {
                // 选人阶段：只查队友
                analyzeChampSelect();
            } else if ("InProgress".equals(currentPhase)) {
                // 游戏中：查全员 10 人
                analyzeInProgress();
            }

        } catch (Exception e) {
            // 忽略连接异常
        }
    }

    // --- 选人阶段 (查队友) ---
    private void analyzeChampSelect() {
        String json = service.getChampSelectSession();
        if (json == null) return;

        JSONObject root = JSONObject.parseObject(json);
        JSONArray myTeam = root.getJSONArray("myTeam");

        if (myTeam != null) {
            for (int i = 0; i < myTeam.size(); i++) {
                try {
                    JSONObject player = myTeam.getJSONObject(i);
                    long summonerId = player.getLongValue("summonerId");
                    long championId = player.getLongValue("championId");

                    if (summonerId == 0) continue;

                    // 直接分析，不去重！
                    analyzeSinglePlayer(summonerId, championId);
                } catch (Exception e) {
                    log.error("分析队友失败", e);
                }
            }
        }
    }

    // --- 游戏中 (查全员) ---
    private void analyzeInProgress() {
        // 尝试获取对局信息
        CurrentGameInfo game = service.getCurrentGameInfo();
        if (game == null) return;

        List<CurrentGameInfo.CurrentGameParticipant> participants = game.getParticipants();
        if (participants == null) return;

        for (CurrentGameInfo.CurrentGameParticipant p : participants) {
            try {
                // 直接分析，不去重！
                analyzeSinglePlayer(p.getSummonerId(), p.getChampionId());
            } catch (Exception e) {
                log.error("分析玩家失败", e);
            }
        }
    }

    // --- 统一分析方法 ---
    private void analyzeSinglePlayer(long summonerId, long championId) {
        try {
            // 🔥 以前这里写了一大堆计算 KDA 的代码，现在全部删掉！
            // 🔥 直接委托给 Service 去做，一行代码搞定！
            PlayerAnalysisResult result = analysisService.analyzeBySummonerId(summonerId, championId);

            if (result != null) {
                gameContext.addPlayer(result);
            }
        } catch (Exception e) {
            log.error("分析玩家失败: " + summonerId, e);
        }
    }
}