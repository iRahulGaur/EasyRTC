package com.cx.easyrtc

import android.app.Application
import android.util.Log
import com.cx.easyrtc.socket.SocketWrapper
import java.util.*

/**
 * Created by cx on 2018/12/22.
 */
class EasyRTCApplication : Application() {

    override fun onCreate() {
        Log.e(TAG, "EasyRTCApplication SocketWrapper setURL")
        super.onCreate()
        SocketWrapper.shareContext().connectToURL(SOCKET_BASE_LINK)
        initSocketHeartBeat()
    }

    private fun initSocketHeartBeat() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                SocketWrapper.shareContext().keepAlive()
            }
        }, 0, 2000)
    }

    companion object {
        private const val TAG = "EasyRTCApplication"

//        private const val SOCKET_BASE_LINK = "https://4b78-182-64-42-239.ngrok.io"
        private const val SOCKET_BASE_LINK = "http://192.168.1.16:1234"
    }
}