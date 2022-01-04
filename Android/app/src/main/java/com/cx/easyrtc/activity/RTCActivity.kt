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
import com.cx.easyrtc.socket.SocketWraper
import com.cx.easyrtc.socket.SocketWraper.SocketDelegate
import com.cx.easyrtc.webRTC.WebRTCWraper
import com.cx.easyrtc.webRTC.WebRTCWraper.RtcListener
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.MediaStream
import org.webrtc.VideoRenderer
import org.webrtc.VideoRendererGui
import java.util.*

class RTCActivity : AppCompatActivity(), SocketDelegate, RtcListener {

    //    private final String TAG = RTCActivity.class.getName();
    private var mCallStatus: String? = null
    private var mIfNeedAddStream = false
    private var mRtcWrapper: WebRTCWraper? = null
    private var mRtcLocalRender: VideoRenderer.Callbacks? = null
    private var mRtcRemoteRender: VideoRenderer.Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtc)
        Log.e("skruazzz", "RTCActivity onCreate")
        setUI()
        callStatus
        type
        setGLView()
        setVideoRender()
    }

    override fun onStop() {
        Log.e("skruazzz", "RTCActivity onStop")
        SocketWraper.shareContext().removeListener(this)
        super.onStop()
    }

    private fun setUI() {
        setButton()
    }

    private fun setButton() {
        val mCancelButton = findViewById<ImageButton>(R.id.CancelButton)
        mCancelButton.setOnClickListener {
            Log.e("sliver", "cancel button clicked")
            try {
                SocketWraper.shareContext().emit("exit", "yes")
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
            VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true
        )
        mRtcRemoteRender = VideoRendererGui.create(
            0, 0, 100, 100,
            VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false
        )
    }

    private fun setSocketWrapper() {
        SocketWraper.shareContext().addListener(this)
    }

    private fun setRtcWrapper() {
        Log.e("sliver", "RTCActivity setRtcWraper")
        mRtcWrapper = WebRTCWraper(this, VideoRendererGui.getEGLContext(), mIfNeedAddStream)
        setSocketWrapper()
        createOfferOrAck()
    }

    private fun createOfferOrAck() {
        if (mCallStatus == "send") {
            mRtcWrapper!!.createOffer()
        } else if (mCallStatus == "recv") {
            SocketWraper.shareContext().ack(true)
        }
    }

    private fun processSignalMsg(target: String, type: String, value: String) {
        Log.e("sliver", "RTCActivity processSignalMsg $type")
        if (target == SocketWraper.shareContext().uid) {
            if (type == "offer") {
                Log.e("sliver", "RTCActivity receive offer $mRtcWrapper")
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
            Log.e("sliver", "RTCActivity get error tag")
        }
    }

    override fun onUserAgentsUpdate(agents: ArrayList<Agent>) {}
    override fun onDisConnect() {
        Log.e("sliver", "RTCActivity onDisConnect")
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
        Log.e("sliver", "RTCActivity onRemoteCandidate")
        mRtcWrapper!!.setCandidate(label, mid, candidate)
    }

    override fun onLocalStream(mediaStream: MediaStream) {
        Log.e("sliver", "RTCActivity onLocalStream")
        mediaStream.videoTracks[0].addRenderer(VideoRenderer(mRtcLocalRender))
        VideoRendererGui.update(
            mRtcLocalRender, 75, 75, 25, 25,
            VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true
        )
    }

    override fun onAddRemoteStream(mediaStream: MediaStream) {
        Log.e("sliver", "RTCActivity onAddRemoteStream")
        mediaStream.videoTracks[0].addRenderer(VideoRenderer(mRtcRemoteRender))
        VideoRendererGui.update(
            mRtcRemoteRender, 0, 0, 75, 75,
            VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true
        )
        if (mIfNeedAddStream) {
            VideoRendererGui.update(
                mRtcLocalRender, 75, 75, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true
            )
        }
    }

    override fun onRemoveRemoteStream(mediaStream: MediaStream) {
        Log.e("sliver", "RTCActivity onRemoveRemoteStream")
        VideoRendererGui.update(
            mRtcLocalRender, 75, 75, 25, 25,
            VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true
        )
    }

    override fun onCreateOfferOrAnswer(type: String, sdp: String) {
        Log.e("sliver", "RTCActivity onCreateOfferOrAnswer")
        try {
            SocketWraper.shareContext().emit(type, sdp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onIceCandidate(label: Int, id: String, candidate: String) {
        Log.e("sliver", "RTCActivity onIceCandidate")
        try {
            val msg = JSONObject()
            msg.put("source", SocketWraper.shareContext().uid)
            msg.put("target", SocketWraper.shareContext().target)
            msg.put("label", label)
            msg.put("mid", id)
            msg.put("candidate", candidate)
            SocketWraper.shareContext().emit("candidate", msg)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}