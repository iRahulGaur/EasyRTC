package com.cx.easyrtc.activity

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cx.easyrtc.EasyRTCApplication
import com.cx.easyrtc.R
import com.cx.easyrtc.agent.Agent
import com.cx.easyrtc.socket.SocketWrapper
import com.cx.easyrtc.socket.SocketWrapper.SocketDelegate
import com.cx.easyrtc.webRTC.WebRTCWrapper
import com.cx.easyrtc.webRTC.WebRTCWrapper.RtcListener
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.VideoRenderer
import org.webrtc.VideoRendererGui
import java.util.*

class RTCActivity : AppCompatActivity(), SocketDelegate, RtcListener {

    companion object {
        private const val TAG = "RTCActivity"
    }

    private var mCallStatus: String? = null
    private var mIfNeedAddStream = false
    private var mRtcWrapper: WebRTCWrapper? = null
    private var mRtcLocalRender: VideoRenderer.Callbacks? = null
    private var mRtcRemoteRender: VideoRenderer.Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtc)
        Log.e(TAG, "RTCActivity onCreate")
        setUI()
        callStatus
        type
        setGLView()
        setVideoRender()
    }

    override fun onStop() {
        Log.e(TAG, "RTCActivity onStop")
        SocketWrapper.shareContext().removeListener(this)
        super.onStop()
    }

    private fun setUI() {
        setButton()
    }

    private fun setButton() {
        val mCancelButton = findViewById<ImageButton>(R.id.CancelButton)
        mCancelButton.setOnClickListener {
            Log.e(TAG, "cancel button clicked")
            try {
                SocketWrapper.shareContext().emit("exit", "yes")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            mRtcWrapper!!.exitSession()
            val intent = Intent(this@RTCActivity, CallActivity::class.java)
            startActivity(intent)
        }
    }

    private val callStatus: Unit
        get() {
            mCallStatus = intent.extras!!.getString("status")
        }
    private val type: Unit
        get() {
            val remoteType = intent.extras!!.getString("type")
            mIfNeedAddStream = remoteType != "camera"
        }

    private fun setGLView() {
        val mGLView = findViewById<GLSurfaceView>(R.id.glview)
        mGLView.preserveEGLContextOnPause = true
        mGLView.keepScreenOn = true
        VideoRendererGui.setView(mGLView) { setRtcWrapper() }
    }

    private fun setVideoRender() {
        mRtcLocalRender = VideoRendererGui.create(
            0, 0, 100, 100,
            RendererCommon.ScalingType.SCALE_ASPECT_FILL, true
        )
        mRtcRemoteRender = VideoRendererGui.create(
            0, 0, 100, 100,
            RendererCommon.ScalingType.SCALE_ASPECT_FILL, false
        )
    }

    private fun setSocketWrapper() {
        SocketWrapper.shareContext().addListener(this)
    }

    private fun setRtcWrapper() {
        Log.e(TAG, "RTCActivity setRtcWrapper")
        mRtcWrapper = WebRTCWrapper(
            this,
            mIfNeedAddStream
        )
        setSocketWrapper()
        createOfferOrAck()
    }

    private fun createOfferOrAck() {
        if (mCallStatus == "send") {
            mRtcWrapper!!.createOffer()
        } else if (mCallStatus == "recv") {
            SocketWrapper.shareContext().ack(true)
        }
    }

    private fun processSignalMsg(target: String, type: String, value: String) {
        Log.e(TAG, "RTCActivity processSignalMsg $type")
        if (target == SocketWrapper.shareContext().uid) {
            if (type == "offer") {
                Log.e(TAG, "RTCActivity receive offer $mRtcWrapper")
                mRtcWrapper!!.setRemoteSdp(type, value)
                mRtcWrapper!!.createAnswer()
            }
            if (type == "answer") {
                mRtcWrapper!!.setRemoteSdp(type, value)
            }
            if (type == "exit") {
                mRtcWrapper!!.exitSession()
                val intent = Intent(this@RTCActivity, CallActivity::class.java)
                startActivity(intent)
            }
        } else {
            Log.e(TAG, "RTCActivity get error tag")
        }
    }

    override fun onUserAgentsUpdate(agents: ArrayList<Agent>) {}
    override fun onDisConnect() {
        Log.e(TAG, "RTCActivity onDisConnect")
        runOnUiThread {
            Toast.makeText(
                EasyRTCApplication.getContext(),
                "can't connect to server",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onRemoteEventMsg(source: String, target: String, type: String, value: String) {
        processSignalMsg(target, type, value)
    }

    override fun onRemoteCandidate(label: Int, mid: String, candidate: String) {
        Log.e(TAG, "RTCActivity onRemoteCandidate")
        mRtcWrapper!!.setCandidate(label, mid, candidate)
    }

    override fun onLocalStream(mediaStream: MediaStream) {
        Log.e(TAG, "RTCActivity onLocalStream")
        mediaStream.videoTracks[0].addRenderer(VideoRenderer(mRtcLocalRender))
        VideoRendererGui.update(
            mRtcLocalRender, 75, 75, 25, 25,
            RendererCommon.ScalingType.SCALE_ASPECT_FILL, true
        )
    }

    override fun onAddRemoteStream(mediaStream: MediaStream) {
        Log.e(TAG, "RTCActivity onAddRemoteStream")
        mediaStream.videoTracks[0].addRenderer(VideoRenderer(mRtcRemoteRender))
        VideoRendererGui.update(
            mRtcRemoteRender, 0, 0, 75, 75,
            RendererCommon.ScalingType.SCALE_ASPECT_FILL, true
        )
        if (mIfNeedAddStream) {
            VideoRendererGui.update(
                mRtcLocalRender, 75, 75, 25, 25,
                RendererCommon.ScalingType.SCALE_ASPECT_FILL, true
            )
        }
    }

    override fun onRemoveRemoteStream(mediaStream: MediaStream) {
        Log.e(TAG, "RTCActivity onRemoveRemoteStream")
        VideoRendererGui.update(
            mRtcLocalRender, 75, 75, 25, 25,
            RendererCommon.ScalingType.SCALE_ASPECT_FILL, true
        )
    }

    override fun onCreateOfferOrAnswer(type: String, sdp: String) {
        Log.e(TAG, "RTCActivity onCreateOfferOrAnswer")
        try {
            SocketWrapper.shareContext().emit(type, sdp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onIceCandidate(label: Int, id: String, candidate: String) {
        Log.e(TAG, "RTCActivity onIceCandidate")
        try {
            val msg = JSONObject()
            msg.put("source", SocketWrapper.shareContext().uid)
            msg.put("target", SocketWrapper.shareContext().target)
            msg.put("label", label)
            msg.put("mid", id)
            msg.put("candidate", candidate)
            SocketWrapper.shareContext().emit("candidate", msg)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}