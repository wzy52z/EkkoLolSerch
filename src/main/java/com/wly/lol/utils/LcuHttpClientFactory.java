package com.wly.lol.utils;

import lombok.Getter;
import okhttp3.OkHttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class LcuHttpClientFactory {
    private static volatile OkHttpClient instance;
    //获取单例client (双重检查锁)
    public static OkHttpClient getInstance() {
        if(instance == null){
            synchronized(LcuHttpClientFactory.class){
                if(instance == null){
                    instance =createUnsafeClient();
                }
            }
        }
        return instance;
    }

    private static OkHttpClient createUnsafeClient() {
        try{
            X509TrustManager trustAll = new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null,new TrustManager[]{trustAll},new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),trustAll)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
