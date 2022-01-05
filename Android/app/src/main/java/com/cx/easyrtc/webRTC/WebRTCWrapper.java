package com.cx.easyrtc.webRTC;

import static org.webrtc.CameraEnumerationAndroid.getNameOfBackFacingDevice;
import static org.webrtc.CameraEnumerationAndroid.getNameOfFrontFacingDevice;

import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;

import java.util.LinkedList;

/**
 * Created by cx on 2018/12/12.
 */
public class WebRTCWrapper implements SdpObserver, PeerConnection.Observer {

    private static final String TAG = "WebRTCWrapper";

    private PeerConnection mPeer;

    private final PeerConnectionFactory mPeerFactory;

    private MediaStream mLocalMedia;

    private VideoSource mVideoSource;

    private final RtcListener mListener;

    private final LinkedList<PeerConnection.IceServer> mIceServers = new LinkedList<>();

    private final MediaConstraints mMediaConstraints = new MediaConstraints();

    private final boolean mIfNeedAddStream;

    public interface RtcListener {

        void onLocalStream(MediaStream mediaStream);

        void onAddRemoteStream(MediaStream mediaStream);

        void onRemoveRemoteStream(MediaStream mediaStream);

        void onCreateOfferOrAnswer(String type, String sdp);

        void onIceCandidate(int label, String id, String candidate);
    }

    public WebRTCWrapper(RtcListener listener, boolean ifNeedAddStream) {
        mListener = listener;
        mIfNeedAddStream = ifNeedAddStream;
        PeerConnectionFactory.initializeAndroidGlobals(listener, true, true, true);
        mPeerFactory = new PeerConnectionFactory();
        mIceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        mIceServers.add(new PeerConnection.IceServer("turn:129.28.101.171:3478","cx","cx1234"));
        mIceServers.add(new PeerConnection.IceServer("turn:39.105.125.160:3478", "helloword", "helloword"));
        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        createLocalMedia();
        createPeerConnection();
    }

    public void createOffer() {
        Log.e(TAG, "WebRTCWrapper createOffer");
        mPeer.createOffer(this, mMediaConstraints);
    }

    public void createAnswer() {
        Log.e(TAG, "WebRTCWrapper createAnswer");
        mPeer.createAnswer(this, mMediaConstraints);
    }

    public void setRemoteSdp(String type, String sdp) {
        Log.e(TAG, "WebRTCWrapper setRemoteSdp");
        SessionDescription sessionDescription =
                new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp);
        mPeer.setRemoteDescription(this, sessionDescription);
    }

    public void setCandidate(int label, String mid, String candidate) {
        Log.e(TAG, "WebRTCWrapper setRemoteCandidate");
        if (mPeer.getRemoteDescription() != null) {
            IceCandidate iceCandidate = new IceCandidate(mid, label, candidate);
            mPeer.addIceCandidate(iceCandidate);
        } else {
            Log.e(TAG, "WebRTCWrapper remote sdp is null when set candidate");
        }
    }

    public void exitSession() {
        mPeer.close();
        mPeer.dispose();
        if (mVideoSource != null) {
            mVideoSource.dispose();
        }
        mPeerFactory.dispose();
    }

    private void createPeerConnection() {
        mPeer = mPeerFactory.createPeerConnection(mIceServers, mMediaConstraints, this);
        if (mIfNeedAddStream) {
            mPeer.addStream(mLocalMedia);
        }
    }

    private void createLocalMedia() {
        mLocalMedia = mPeerFactory.createLocalMediaStream("ARDAMS");
        MediaConstraints videoConstraints = new MediaConstraints();

        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1280)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(720)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(60)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)));

        mVideoSource = mPeerFactory.createVideoSource(getVideoCapture(true), videoConstraints);
        mLocalMedia.addTrack(mPeerFactory.createVideoTrack("ARDAMSv0", mVideoSource));

        AudioSource audioSource = mPeerFactory.createAudioSource(new MediaConstraints());
        mLocalMedia.addTrack(mPeerFactory.createAudioTrack("ARDAMSa0", audioSource));

        if (mIfNeedAddStream) {
            mListener.onLocalStream(mLocalMedia);
        }
    }
    
    private VideoCapturer getVideoCapture(boolean useFrontCamera) {
        String frontCameraDeviceName;
        if (useFrontCamera)
            frontCameraDeviceName = getNameOfFrontFacingDevice();
        else
            frontCameraDeviceName = getNameOfBackFacingDevice();

        return VideoCapturerAndroid.create(frontCameraDeviceName);
    }

    //Successfully created local sdp
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.e(TAG, "WebRTCWrapper onCreateSuccess type:" + sessionDescription.type.canonicalForm());
        Log.e(TAG, "this is sdp offer find a=mid:audio " + sessionDescription.description.contains("a=mid:video"));
        Log.e(TAG, "this is sdp offer = "+sessionDescription.description);
        mPeer.setLocalDescription(this, sessionDescription);
        mListener.onCreateOfferOrAnswer(sessionDescription.type.canonicalForm(), sessionDescription.description);
    }

    //Set up remote sdp successfully
    @Override
    public void onSetSuccess() {
        Log.e(TAG, "WebRTCWrapper onSetSuccess");
    }

    //Successfully created local sdp
    @Override
    public void onCreateFailure(String s) {
        Log.e(TAG, "WebRTCWrapper onCreateFailure error : " + s);
    }

    //Failed to set remote sdp
    @Override
    public void onSetFailure(String s) {
        Log.e(TAG, "WebRTCWrapper onSetFailure error : " + s);
    }

    //Signaling status change
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.e(TAG, "WebRTCWrapper onSignalingChange state : " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.e(TAG, "WebRTCWrapper onIceConnectionChange state : " + iceConnectionState);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.e(TAG, "WebRTCWrapper onIceGatheringChange state : " + iceGatheringState);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.e(TAG, "WebRTCWrapper onIceCandidate");
        mListener.onIceCandidate(iceCandidate.sdpMLineIndex, iceCandidate.sdpMid, iceCandidate.sdp);
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.e(TAG, "WebRTCWrapper onAddStream");
        mListener.onAddRemoteStream(mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.e(TAG, "WebRTCWrapper onRemoveStream");
        mListener.onRemoveRemoteStream(mediaStream);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.e(TAG, "WebRTCWrapper onDataChannel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.e(TAG, "WebRTCWrapper onRenegotiationNeeded");
    }
}
