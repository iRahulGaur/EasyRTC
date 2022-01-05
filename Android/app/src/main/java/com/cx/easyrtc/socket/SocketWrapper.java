package com.cx.easyrtc.socket;

import android.os.Build;
import android.util.Log;

import com.cx.easyrtc.agent.Agent;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by cx on 2018/12/12.
 */
public class SocketWrapper {

    private static final String TAG = "SocketWrapper";

    private Socket mSignaling;

    private MessageProcessor mMsgProcessor;

    private final ArrayList<SocketDelegate> mListeners;

    private String uid;

    private String mTarget;

    private static SocketWrapper mShareContext;

    public interface SocketDelegate {
        void onUserAgentsUpdate(ArrayList<Agent> agents);

        void onDisConnect();

        void onRemoteEventMsg(String source, String target, String type, String value);

        void onRemoteCandidate(int label, String mid, String candidate);
    }

    public static synchronized SocketWrapper shareContext() {
        if (mShareContext == null) {
            mShareContext = new SocketWrapper();
        }
        return mShareContext;
    }

    public SocketWrapper() {
        mListeners = new ArrayList<>();
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }

    public void connectToURL(String host) {
        mMsgProcessor = new MessageProcessor();
        try {
            mSignaling = IO.socket(host);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        setEvent();
        mSignaling.connect();
        register();
    }

    public synchronized void addListener(SocketDelegate delegate) {
        mListeners.add(delegate);
    }

    public synchronized void removeListener(SocketDelegate delegate) {
        mListeners.remove(delegate);
    }

    private void destroy() {
        mSignaling.disconnect();
        mSignaling.close();
    }

    public String getUid() {
        return uid;
    }

    public void setTarget(String target) {
        mTarget = target;
    }

    public String getTarget() {
        return mTarget;
    }

    public void emit(String type, String value) throws JSONException{
        JSONObject msg = new JSONObject();
        msg.put("source", uid);
        msg.put("target", mTarget);
        msg.put("type", type);
        msg.put("value", value);
        mSignaling.emit("event", msg);
    }

    public void emit(String event, JSONObject object) {
        mSignaling.emit(event, object);
    }

    public void keepAlive() {
        if (uid != null) {
            try {
                JSONObject msg = new JSONObject();
                msg.put("source", uid);
                msg.put("target", "EastRTC Server");
                msg.put("type", "heart");
                msg.put("value", "yes");
                mSignaling.emit("heart", msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateRemoteAgent() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("source", uid);
            msg.put("target", mTarget);
            msg.put("type", "request_user_list");
            msg.put("value", "yes");
            mSignaling.emit("request_user_list", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("name", Build.MODEL);
            msg.put("type", "Android_client");
            mSignaling.emit("get_id", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void invite() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("source", uid);
            msg.put("target", mTarget);
            msg.put("type", "invite");
            msg.put("value", "yes");

            mSignaling.emit("event", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void ack(boolean accept) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("source", uid);
            msg.put("target", mTarget);
            msg.put("type", "ack");
            if (accept) {
                msg.put("value", "yes");
            } else {
                msg.put("value", "no");
            }

            mSignaling.emit("event", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setEvent() {
        mSignaling.on("connect", mMsgProcessor.onConnect);
        mSignaling.on("event", mMsgProcessor.onEvent);
        mSignaling.on("candidate", mMsgProcessor.onCandidate);
        mSignaling.on("set_id", mMsgProcessor.onGet);
        mSignaling.on("user_list", mMsgProcessor.onResponse);
        mSignaling.on("disconnect", mMsgProcessor.onDisconnect);
    }

    private void verify(String type) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("source", uid);
            msg.put("target", "EasyRTC Server");
            msg.put("type", type);
            mSignaling.emit("ack", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class MessageProcessor {

        private final Emitter.Listener onConnect = args -> Log.e(TAG, "SocketWrapper SocketWrapper onConnect");

        private final Emitter.Listener onEvent = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject)args[0];
                try {
                    String type   = data.getString("type");
                    String source = data.getString("source");
                    String target = data.getString("target");
                    String value  = data.getString("value");

                    synchronized (SocketWrapper.shareContext()) {
                        Log.e(TAG, "SocketWrapper receive event type " + type + " size " + mListeners.size());
                        for (SocketDelegate delegate : SocketWrapper.shareContext().mListeners) {
                            delegate.onRemoteEventMsg(source, target, type, value);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                verify("event");
            }
        };

        private final Emitter.Listener onCandidate = args -> {
            Log.e(TAG, "SocketWrapper receive remote candidate");
            JSONObject data = (JSONObject)args[0];
            try {
                int label = data.getInt("label");
                String mid = data.getString("mid");
                String candidate = data.getString("candidate");

                synchronized (SocketWrapper.shareContext()) {
                    for (SocketDelegate delegate : SocketWrapper.shareContext().mListeners) {
                        delegate.onRemoteCandidate(label, mid, candidate);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            verify("candidate");
        };

        private final Emitter.Listener onGet = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject)args[0];
                try {
                    uid = data.getString("value");
                    Log.e(TAG, "SocketWrapper get id : " + uid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                verify("set_id");
            }
        };

        private final Emitter.Listener onResponse = args -> {
            JSONObject data = (JSONObject)args[0];
            try {
                String cntString  = data.getString("cnt");
                int cnt = Integer.parseInt(cntString);

                Log.e(TAG, "SocketWrapper cnt : " + cnt);

                ArrayList<Agent> list = new ArrayList<>();

                for (int i = 0; i < cnt; i++) {
                    JSONObject object = (JSONObject) data.get(String.valueOf(i));
                    String id = object.getString("id");
                    String name = object.getString("name");
                    String remoteType = object.getString("type");

                    list.add(new Agent(id, name, remoteType));
                }
                synchronized (SocketWrapper.shareContext()) {
                    for (SocketDelegate delegate : SocketWrapper.shareContext().mListeners) {
                        delegate.onUserAgentsUpdate(list);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            verify("user_list");
        };

        private final Emitter.Listener onDisconnect = args -> {
            Log.e(TAG, "onDisconnect");
            for (SocketDelegate delegate : SocketWrapper.shareContext().mListeners) {
                delegate.onDisConnect();
            }
        };
    }
}
