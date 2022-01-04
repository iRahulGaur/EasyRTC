package com.cx.easyrtc.Activity;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cx.easyrtc.Agent.Agent;
import com.cx.easyrtc.EasyRTCApplication;
import com.cx.easyrtc.R;
import com.cx.easyrtc.Socket.SocketWraper;
import com.cx.easyrtc.WebRTC.WebRTCWraper;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;

public class RTCActivity extends AppCompatActivity implements SocketWraper.SocketDelegate, WebRTCWraper.RtcListener{

//    private final String TAG = RTCActivity.class.getName();

    private String mCallStatus;

    private boolean mIfNeedAddStream;

    private WebRTCWraper mRtcWrapper;

    private VideoRenderer.Callbacks mRtcLocalRender;

    private VideoRenderer.Callbacks mRtcRemoteRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtc);

        Log.e("skruazzz", "RTCActivity onCreate");
        setUI();
        getCallStatus();
        getType();
        setGLView();
        setVideoRender();
    }

    @Override
    protected void onStop() {
        Log.e("skruazzz", "RTCActivity onStop");
        SocketWraper.shareContext().removeListener(this);

        super.onStop();
    }

    private void setUI() {
        setButton();
    }

    private void setButton() {
        ImageButton mCancelButton = findViewById(R.id.CancelButton);
        mCancelButton.setOnClickListener(view -> {
            Log.e("sliver", "cancel button clicked");
            try {
                SocketWraper.shareContext().emit("exit", "yes");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mRtcWrapper.exitSession();
            Intent intent = new Intent(RTCActivity.this, CallActivity.class);
            startActivity(intent);
        });
    }

    private void getCallStatus() {
        mCallStatus = getIntent().getExtras().getString("status");
    }

    private void getType() {
        String remoteType = getIntent().getExtras().getString("type");
        mIfNeedAddStream = !remoteType.equals("camera");
    }

    private void setGLView() {
        GLSurfaceView mGLView = findViewById(R.id.glview);
        mGLView.setPreserveEGLContextOnPause(true);
        mGLView.setKeepScreenOn(true);
        VideoRendererGui.setView(mGLView, this::setRtcWraper);
    }

    private void setVideoRender() {
        mRtcLocalRender = VideoRendererGui.create(0, 0, 100, 100,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
        mRtcRemoteRender = VideoRendererGui.create(0, 0, 100,100,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
    }

    private void setSocketWraper() {
        SocketWraper.shareContext().addListener(this);
    }

    private void setRtcWraper() {
        Log.e("sliver", "RTCActivity setRtcWraper");
        mRtcWrapper = new WebRTCWraper(this, VideoRendererGui.getEGLContext(), mIfNeedAddStream);
        setSocketWraper();
        createOfferOrAck();
    }

    private void createOfferOrAck() {
        if (mCallStatus.equals("send")) {
            mRtcWrapper.createOffer();
        } else if (mCallStatus.equals("recv")) {
            SocketWraper.shareContext().ack(true);
        }
    }

    private void processSignalMsg(String source, String target, String type, String value) {
        Log.e("sliver", "RTCActivity processSignalMsg " + type);
        if (target.equals(SocketWraper.shareContext().getUid())) {
            if (type.equals("offer")) {
                Log.e("sliver", "RTCActivity receive offer " + mRtcWrapper);
                mRtcWrapper.setRemoteSdp(type, value);
                mRtcWrapper.createAnswer();
            }

            if (type.equals("answer")) {
                mRtcWrapper.setRemoteSdp(type, value);
            }

            if (type.equals("exit")) {
                mRtcWrapper.exitSession();
                Intent intent = new Intent(RTCActivity.this, CallActivity.class);
                startActivity(intent);
            }
        } else {
            Log.e("sliver", "RTCActivity get error tag");
        }
    }

    @Override
    public void onUserAgentsUpdate(ArrayList<Agent> agents) {

    }

    @Override
    public void onDisConnect() {
        Log.e("sliver", "RTCActivity onDisConnect");
        runOnUiThread(() -> Toast.makeText(EasyRTCApplication.getContext(), "can't connect to server", Toast.LENGTH_LONG).show());
    }

    @Override
    public void onRemoteEventMsg(String source, String target, String type, String value) {
        processSignalMsg(source, target, type, value);
    }

    @Override
    public void onRemoteCandidate(int label, String mid, String candidate) {
        Log.e("sliver", "RTCActivity onRemoteCandidate");
        mRtcWrapper.setCandidate(label, mid, candidate);
    }

    @Override
    public void onLocalStream(MediaStream mediaStream) {
        Log.e("sliver", "RTCActivity onLocalStream");
        mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(mRtcLocalRender));
        VideoRendererGui.update(mRtcLocalRender, 75,75, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }

    @Override
    public void onAddRemoteStream(MediaStream mediaStream) {
        Log.e("sliver", "RTCActivity onAddRemoteStream");
        mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(mRtcRemoteRender));
        VideoRendererGui.update(mRtcRemoteRender, 0, 0, 75, 75,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
        if (mIfNeedAddStream) {
            VideoRendererGui.update(mRtcLocalRender, 75, 75, 25, 25,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
        }
    }

    @Override
    public void onRemoveRemoteStream(MediaStream mediaStream) {
        Log.e("sliver", "RTCActivity onRemoveRemoteStream");
        VideoRendererGui.update(mRtcLocalRender, 75, 75, 25, 25,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }

    @Override
    public void onCreateOfferOrAnswer(String type, String sdp) {
        Log.e("sliver", "RTCActivity onCreateOfferOrAnswer");
        try {
            SocketWraper.shareContext().emit(type, sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidate(int label, String id, String candidate) {
        Log.e("sliver", "RTCActivity onIceCandidate");
        try {
            JSONObject msg = new JSONObject();
            msg.put("source", SocketWraper.shareContext().getUid());
            msg.put("target", SocketWraper.shareContext().getTarget());
            msg.put("label", label);
            msg.put("mid", id);
            msg.put("candidate", candidate);

            SocketWraper.shareContext().emit("candidate", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
