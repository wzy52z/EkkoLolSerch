package com.wly.lol;

import com.wly.lol.service.LolApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Scanner;

@SpringBootApplication // 【关键点 4】标识这是一个 Spring Boot 应用
@EnableScheduling      // 【关键点 5】开启定时任务开关
public class LolApplication implements CommandLineRunner {

    @Autowired // 自动注入 Service
    private LolApiService service;

    public static void main(String[] args) {
        // 启动 Spring 容器
        SpringApplication.run(LolApplication.class, args);
    }

    // Spring 启动完成后，会执行这个 run 方法
    // 这里用来处理“手动输入”的主线程逻辑
    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("✅ Spring Boot 已启动。输入 search [ID] 查询：");

        while (true) {
            String input = scanner.nextLine();
            if ("exit".equals(input)) System.exit(0);

            if (input.startsWith("")) {
                // 直接使用自动注入的 service
                var s = service.getSummonerByName(input);
                System.out.println(s);
            }
        }
    }
}