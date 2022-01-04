package com.cx.easyrtc;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.cx.easyrtc.Socket.SocketWraper;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by cx on 2018/12/22.
 */

public class EasyRTCApplication extends Application{
    private static Context mContext;

    @Override
    public void onCreate() {
        Log.e("sliver", "EasyRTCApplication SocketWrpaer setURL");
        super.onCreate();

        mContext = getApplicationContext();

        SocketWraper.shareContext().connectToURL("https://b9c1-182-68-77-107.ngrok.io");
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
                SocketWraper.shareContext().keepAlive();
            }
        }, 0, 2000);
    }
}
