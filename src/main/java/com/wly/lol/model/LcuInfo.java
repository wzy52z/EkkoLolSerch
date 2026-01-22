package com.wly.lol.model;

import lombok.Getter;
import okhttp3.Credentials;

@Getter // Lombok 注解：自动为所有字段生成 getXxx() 方法
public class LcuInfo {

    // 加上 final，表示它们一旦赋值不可修改
    private final String port;
    private final String password;
    private final String protocol = "https";
    private final String url;
    private final String authHeader;

    public LcuInfo(String port, String password) {
        this.port = port;
        this.password = password;

        // 在构造函数里一次性计算好，以后只管用，不再改
        this.url = protocol + "://127.0.0.1:" + port;
        this.authHeader = Credentials.basic("riot", password);
    }
}