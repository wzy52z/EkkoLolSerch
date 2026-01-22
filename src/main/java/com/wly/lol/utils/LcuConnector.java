package com.wly.lol.utils;


import com.wly.lol.model.LcuInfo;
import kotlin.text.Regex;



public class LcuConnector {
    private static final String PORT_REGEX = "--app-port=([0-9]+)";
    private static final String TOKEN_REGEX ="--remoting-auth-token=([\\w-]+)";
    public static LcuInfo connect() throws Exception {
        System.out.println("通过WMIC扫描LOL进程");
        String commandLine = ProcessManager.getProcessCommandLine("LeagueClientUx.exe");
        if(commandLine == null||!commandLine.contains("--remoting-auth-token")){
            throw new RuntimeException("未找到游戏进程，是否启动");
        }
        String port = RegexUtils.extract(commandLine, PORT_REGEX);
        String token = RegexUtils.extract(commandLine, TOKEN_REGEX);
        /*英雄联盟的客户端（LCU）本质上是一个浏览器（基于 Chromium），它在本地启动了一个微型的 HTTPS 服务器。
        为了防止随便哪个程序都能控制你的客户端（比如乱改符文、乱发消息），拳头公司（Riot）给这个本地服务器设了一道门禁：
        随机密码：每次客户端启动，都会生成一个随机的密码（Token）。
        通过参数传递：客户端启动时，会把自己生成的这个密码，通过命令行参数告诉自己。
        启动命令长得像这样（这就是 commandLine 变量里的内容）：
        Bash"C:\Riot Games\League of Legends\LeagueClientUx.exe" --remoting-auth-token=ib_s7wW5s-1-abc12345 --app-port=12345 ...
        你的代码逻辑是： 如果抓到的命令里没有 --remoting-auth-token，说明：
        你抓错进程了（抓成了 LeagueClient.exe 而不是 LeagueClientUx.exe）。
        游戏还没完全启动，Token 还没生成。
        根本没抓到任何东西。
        */
        //检查获取到的进程启动命令字符串 (commandLine) 中，是否包含了 --remoting-auth-token 这个参数。
        System.out.println("连接成功");
        return new LcuInfo(port, token);
    }


}

