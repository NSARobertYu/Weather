package com.example.administrator.weather.Util;

/**
 * Created by Administrator on 2017/2/20.
 */

public interface HttpCallbackListener {
    void onFinish(String response);

    void onError(Exception e);

}
