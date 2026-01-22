package com.wly.lol.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestLCU {

    public static class LcuInfo {
        public String port;
        public String password;
        public String url;
        public String authHeader;
    }

    // WMIC 获取参数方法 (保持不变)
    public static LcuInfo getLcuInfo() throws Exception {
        System.out.println("正在尝试通过 WMIC 获取 LCU 启动参数...");
        Process p = Runtime.getRuntime().exec("wmic process where name='LeagueClientUx.exe' get commandline");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("GBK")));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) output.append(line);
        reader.close();
        String commandLine = output.toString();

        if (commandLine.isEmpty() || !commandLine.contains("--remoting-auth-token")) {
            throw new RuntimeException("未能获取到启动参数！请确认游戏已登录。");
        }

        String port = findByRegex(commandLine, "--app-port=([0-9]+)");
        String token = findByRegex(commandLine, "--remoting-auth-token=([\\w-]+)");

        LcuInfo info = new LcuInfo();
        info.port = port;
        info.password = token;
        info.url = "https://127.0.0.1:" + info.port;
        info.authHeader = Credentials.basic("riot", info.password);
        return info;
    }

    private static String findByRegex(String source, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static OkHttpClient getOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ================== MAIN 方法修正版 ==================
    public static void main(String[] args) {
        try {
            OkHttpClient client = getOkHttpClient();
            Gson gson = new Gson();

            LcuInfo info = getLcuInfo();
            System.out.println("✅ 连接成功 Port: " + info.port);

            // 1. 查指定玩家 (示例：Hide on bush#KR1)
            String targetGameName = "泡芙奶喵喵";
            String targetTagLine = "KR1";
            System.out.println("\n🔍 正在查询玩家: " + targetGameName + "#" + targetTagLine);

            String encodedName = targetGameName.replace(" ", "%20") + "%23" + targetTagLine;
            String summonerUrl = info.url + "/lol-summoner/v1/summoners?name=" + encodedName;

            String puuid = "";
            long summonerId = 0; // 【关键】新增变量用来存 ID

            Request request1 = new Request.Builder().url(summonerUrl).header("Authorization", info.authHeader).build();
            try (Response resp = client.newCall(request1).execute()) {
                if (!resp.isSuccessful()) {
                    System.out.println("❌ 查无此人！");
                    return;
                }
                JsonObject summonerInfo = gson.fromJson(resp.body().string(), JsonObject.class);
                puuid = summonerInfo.get("puuid").getAsString();
                summonerId = summonerInfo.get("summonerId").getAsLong(); // 【关键】提取 summonerId
                System.out.println("✅ 找到目标！PUUID: " + puuid + " | ID: " + summonerId);
            }

            if (!puuid.isEmpty()) {
                // ==========================================
                // 🌟 修复后的功能 1: 查询英雄熟练度 (绝活检查)
                // ==========================================
                // 【关键修改】这里用 summonerId，而不是 puuid
                String masteryUrl = info.url + "/lol-collections/v1/inventories/" + summonerId + "/champion-mastery";
                Request masteryReq = new Request.Builder().url(masteryUrl).header("Authorization", info.authHeader).build();

                try (Response resp = client.newCall(masteryReq).execute()) {
                    if (resp.isSuccessful()) {
                        String json = resp.body().string();
                        // 【关键修改】接口直接返回数组，所以直接转 JsonArray，不要先转 Object
                        JsonArray masteries = gson.fromJson(json, JsonArray.class);

                        System.out.println("\n🔥 === 绝活英雄 (Top 3) ===");
                        for (int i = 0; i < Math.min(3, masteries.size()); i++) {
                            JsonObject m = masteries.get(i).getAsJsonObject();
                            int champId = m.get("championId").getAsInt();
                            long points = m.get("championPoints").getAsLong();
                            int level = m.get("championLevel").getAsInt();
                            System.out.println("英雄ID: " + champId + " | 等级: " + level + " | 熟练度: " + points);
                        }
                    } else {
                        System.out.println("❌ 获取熟练度失败: " + resp.code());
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ 解析熟练度数据出错: " + e.getMessage());
                }

                // ==========================================
                // 🌟 功能 2: 查询历史战绩 (URL 保持用 puuid)
                // ==========================================
                String historyUrl = info.url + "/lol-match-history/v1/products/lol/" + puuid + "/matches?begIndex=0&endIndex=3";
                Request historyReq = new Request.Builder().url(historyUrl).header("Authorization", info.authHeader).build();

                try (Response resp = client.newCall(historyReq).execute()) {
                    if (resp.isSuccessful()) {
                        JsonObject root = gson.fromJson(resp.body().string(), JsonObject.class);
                        // 战绩接口结构比较深：games -> games -> []
                        if (root.has("games") && !root.get("games").isJsonNull()) {
                            JsonObject gamesObj = root.getAsJsonObject("games");
                            if (gamesObj.has("games")) {
                                JsonArray gameList = gamesObj.getAsJsonArray("games");
                                System.out.println("\n📜 === 近期战绩 (最近 " + gameList.size() + " 场) ===");
                                for (int i = 0; i < gameList.size(); i++) {
                                    JsonObject game = gameList.get(i).getAsJsonObject();
                                    String mode = game.get("gameMode").getAsString();

                                    // 简单获取第一个玩家的数据演示 (实际需要匹配 participantId)
                                    JsonArray participants = game.getAsJsonArray("participants");
                                    JsonObject p = participants.get(0).getAsJsonObject();
                                    JsonObject stats = p.getAsJsonObject("stats");
                                    boolean win = stats.get("win").getAsBoolean();
                                    int k = stats.get("kills").getAsInt();
                                    int d = stats.get("deaths").getAsInt();
                                    int a = stats.get("assists").getAsInt();
                                    int cId = p.get("championId").getAsInt();

                                    System.out.println((win ? "✅ 胜利" : "❌ 失败") + " | 模式: " + mode + " | 英雄ID: " + cId + " | KDA: " + k + "/" + d + "/" + a);
                                }
                            }
                        }
                    } else {
                        System.out.println("❌ 获取战绩失败: " + resp.code());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}