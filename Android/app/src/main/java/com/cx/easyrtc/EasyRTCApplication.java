package com.cx.easyrtc;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.cx.easyrtc.socket.SocketWrapper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by cx on 2018/12/22.
 */
public class EasyRTCApplication extends Application {

    private static final String TAG = "EasyRTCApplication";
    private static Context mContext;

    private static final String SOCKET_BASE_LINK = "https://8e86-182-68-185-44.ngrok.io";

    @Override
    public void onCreate() {
        Log.e(TAG, "EasyRTCApplication SocketWrapper setURL");
        super.onCreate();

        mContext = getApplicationContext();

        SocketWrapper.shareContext().connectToURL(SOCKET_BASE_LINK);
        initSocketHeartBeat();
    }

    public static Context getContext() {
        return mContext;
    }

    private void initSocketHeartBeat() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SocketWrapper.shareContext().keepAlive();
            }
        }, 0, 2000);
    }
}
