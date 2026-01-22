package com.wly.lol.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Logger;
@Slf4j
public class ProcessManager {
    /**
     * 获取指定进程的命令行参数
     */
    public static String getProcessCommandLine(String processName){
        try {
        //使用ProcessBuilder代替Runtime.exec
            ProcessBuilder builder = new ProcessBuilder(
                    "wmic","process","where","name='"+processName+"'"
                    ,"get","commandline"
            );
            Process p = builder.start();
            try(BufferedReader br = new BufferedReader
                    (new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
                return sb.toString();
            }

        } catch (Exception e) {
            log.error("获取进程 [{}] 命令行失败", processName, e);
            //错误的降级处理
            return "";
        }
    }
}
