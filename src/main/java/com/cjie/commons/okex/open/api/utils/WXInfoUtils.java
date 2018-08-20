package com.cjie.commons.okex.open.api.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.net.URLEncoder;

/**
 * Order Id Utils
 *
 * @author Tony Tian
 * @version 1.0.0
 * @date 2018/3/14 10:02
 */
@Slf4j
public class WXInfoUtils {

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();

    private static String SEND_URL = "https://sc.ftqq.com/"
            + "SCU30795T1c383b0ea9299143742a48562ebb458c5b753d9868225" + ".send";


    /**
     * 推送微信服务
     *
     * @param text
     * @param desp
     */
    public static void sendInfo(String text, String desp) {
        WXInfoUtils.log.info("send weixin info start info text:{},desp:{}", text, desp);
        try {
            final Request request = new Request.Builder()
                    .url(WXInfoUtils.SEND_URL + "?text=" + text + "&desp=" + URLEncoder.encode(desp))
                    .get()
                    .build();
            okhttp3.Response response = null;
            response = WXInfoUtils.OK_HTTP_CLIENT.newCall(request).execute();
            if (response.isSuccessful()) {
                WXInfoUtils.log.info("send weixin info is success");
            }
        } catch (Exception e) {
            WXInfoUtils.log.error("send weixin info is error ,message is :{}", e.getMessage());
        }
    }
}
