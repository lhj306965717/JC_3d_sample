package com.danikula.videocache.optimize;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by LiaoHongjie on 2017/7/14.
 * 单例的方式
 */

public class MyOkHttpClient {
    private static OkHttpClient mOkHttpClient = null;

    public static OkHttpClient getInstance() {
        if (mOkHttpClient == null) {
            synchronized (OkHttpUrlSource.class) {
                if (mOkHttpClient == null) {
                    HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(60, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                            .addInterceptor(new RetryIntercepter(3)) // 设置重试次数
                            .build();
                    MyOkHttpClient.mOkHttpClient = okHttpClient;
                }
            }
        }
        return mOkHttpClient;
    }
}
