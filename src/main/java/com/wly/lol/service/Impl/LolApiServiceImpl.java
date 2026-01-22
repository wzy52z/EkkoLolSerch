package com.wly.lol.service.Impl;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wly.lol.model.CurrentGameInfo;
import com.wly.lol.model.LcuInfo;
import com.wly.lol.model.RankInfo;
import com.wly.lol.model.Summoner;
import com.wly.lol.model.match.MatchHistory;
import com.wly.lol.service.LolApiService;
import com.wly.lol.utils.LcuConnector;
import com.wly.lol.utils.LcuHttpClientFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson2.JSON;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class LolApiServiceImpl implements LolApiService {
    private OkHttpClient client;
    private String baseUrl;
    private String authHeader;
    private boolean isConnected = false;
    //初始化连接
    @PostConstruct
     public void init() {
        try{
            log.info("正在连接LOL客户端");
            LcuInfo lcuInfo = LcuConnector.connect();
            this.baseUrl = lcuInfo.getUrl();
            this.authHeader = lcuInfo.getAuthHeader();
            this.isConnected = true;
            this.client = LcuHttpClientFactory.getInstance();
            log.info("LCU 连接成功，端口：{}",lcuInfo.getPort());
        } catch (Exception e) {
            log.warn("LCU 连接失败，是否启动游戏:{}",e.getMessage());
            // ❌ 【删除这一行】 throw new RuntimeException(e);
            // 这一行是罪魁祸首！它导致 Spring 启动失败。

            // ✅ 【改为】只打印警告，不抛出异常
            log.warn("启动时未发现运行中的游戏客户端 (这很正常，程序将继续启动)");
            this.isConnected = false;
        }
    }
    //封装GET方法
    private String executeGet(String endpoint){
        if(!isConnected){
            init();
            if(!isConnected){
                throw new RuntimeException("未启动游戏客户端");
            }
        }
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .header("Authorization", authHeader)
                .get()
                .build();
        try(Response response = client.newCall(request).execute()) {
            if(response.isSuccessful()&&response.body()!=null){
                return response.body().string();
            }else{
                log.error("请求失败:Code = {} Msg = {}",response.code(),response.message());
                return null;
            }
        } catch (IOException e) {
           log.error("请求异常",e);
           return null;
        }
    }
    @Override
    public Summoner getCurrentSummoner() {
        String json = executeGet("/lol-summoner/v1/current-summoner");
        return JSON.parseObject(json, Summoner.class);
    }

    @Override
    public Summoner getSummonerByName(String name) {
        // LCU 的搜索 API
        String json = executeGet("/lol-summoner/v1/summoners?name=" + name);
        return JSON.parseObject(json, Summoner.class);
    }

    @Override
    public MatchHistory getMatchHistory(String puuid) {
        // 获取最近 20 场
        String url = String.format("/lol-match-history/v1/products/lol/%s/matches?begIndex=0&endIndex=20", puuid);
        String json = executeGet(url);
        return JSON.parseObject(json, MatchHistory.class);
    }
    @Override
    public Summoner getSummonerById(long summonerId) {
        // LCU 提供的标准接口：通过 ID 查人
        String json = executeGet("/lol-summoner/v1/summoners/" + summonerId);
        log.info("召唤师原始数据 (ID: {}): {}", summonerId, json);
        return JSON.parseObject(json, Summoner.class);
    }
    @Override
    public RankInfo getRankInfo(String puuid) {
        String json = executeGet("/lol-ranked/v1/ranked-stats/" + puuid);
        // 这里可以直接转，也可以根据需要处理单双排/灵活排的筛选逻辑
        return JSON.parseObject(json, RankInfo.class);
    }

    @Override
    public CurrentGameInfo getCurrentGameInfo() {
        String json = executeGet("/lol-spectator/v1/spectate/active-game");
        // 注意：如果没有在游戏中，这个接口会返回 404，json 为 null
        if (json != null) {
            return JSON.parseObject(json,CurrentGameInfo.class);
        }
        log.warn("标准观战接口404，切换到Session接口抓数据");
        String sessionJson = executeGet("/lol-gameflow/v1/session");
        if (sessionJson == null) {
            log.info("均未找到");
            return null;
        }
        try{
            JSONObject sessionObj = JSON.parseObject(sessionJson);
            //核心数据提取
            JSONObject gameData = sessionObj.getJSONObject("gameData");
            if(gameData==null||gameData.isEmpty()){
                return null;
            }
            CurrentGameInfo info = new CurrentGameInfo();
            info.setGameId(gameData.getLongValue("gameId"));
            info.setGameMode(gameData.getString("gameMode"));
            info.setGameStartTime(gameData.getLongValue("gameStartTime"));
            //处理双边玩家
            List<CurrentGameInfo.CurrentGameParticipant> allPlayers = new ArrayList<>();
            JSONArray teamOne = gameData.getJSONArray("teamOne");
            JSONArray teamTwo = gameData.getJSONArray("teamTwo");
            if(teamOne!=null) allPlayers.addAll(teamOne.toJavaList(CurrentGameInfo.CurrentGameParticipant.class));
            if(teamTwo!=null) allPlayers.addAll(teamTwo.toJavaList(CurrentGameInfo.CurrentGameParticipant.class));
            info.setParticipants(allPlayers);
            JSONArray bans = gameData.getJSONArray("bannedChampions");
            if(bans!=null){
                info.setBannedChampions(bans.toJavaList(CurrentGameInfo.BannedChampion.class));
            }else{
                log.info("ban位信息提取为空");
                info.setBannedChampions(Collections.emptyList());
            }
            log.info("通过Session接口返回对局信息，玩家数:{}",allPlayers.size());
            return info;

            }catch (Exception e){
            log.error("Session解析失败，:{}",e.getMessage());
            return null;
        }
    }
    /**
     * 获取游戏状态
     */

    @Override
    public String getGameFlowPhase() {
        // 调用 LCU 的游戏流接口
        String status = executeGet("/lol-gameflow/v1/gameflow-phase");

        // 注意：API 返回的字符串是带双引号的，比如 "Lobby"
        // 为了方便后续判断，建议把引号去掉
        if (status != null) {
            return status.replace("\"", "");
        }
        return "None";
    }
}
