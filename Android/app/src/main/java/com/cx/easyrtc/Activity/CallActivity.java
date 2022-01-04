package com.cx.easyrtc.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cx.easyrtc.Agent.Agent;
import com.cx.easyrtc.Agent.AgentListAdapter;
import com.cx.easyrtc.EasyRTCApplication;
import com.cx.easyrtc.R;
import com.cx.easyrtc.Socket.SocketWraper;

import java.util.ArrayList;

public class CallActivity extends AppCompatActivity implements SocketWraper.SocketDelegate{

    private final String TAG = CallActivity.class.getName();

    private ListView mAgentsView;

    private AgentListAdapter mAgentListAdapter;

    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        Log.e("skruazzz", "CallActivity onCreate");

        setUI();
        setAgents();
        setSocket();
        updateRemoteAgent();
    }

    @Override
    protected void onStop() {
        Log.e("skruazzz", "CallActivity onStop");
        SocketWraper.shareContext().removeListener(this);

        super.onStop();
    }

    private void setSocket() {
        SocketWraper.shareContext().addListener(this);
    }

    private void updateRemoteAgent() {
        SocketWraper.shareContext().updateRemoteAgent();
    }

    private void setUI() {
        mAgentsView = findViewById(R.id.AgentsView);
        setAlertDialog();
        setButton();
    }

    private void setAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CallActivity.this);
        builder.setTitle("Received call");
        builder.setPositiveButton("Answer", (dialogInterface, i) -> {
            Log.e("sliver", "CallActivity accept invite");
            jumpToNextActivity("recv");
        });
        builder.setNegativeButton("hang up", (dialogInterface, i) -> {
            Log.e("sliver", "CallActivity refuse invite");
            SocketWraper.shareContext().ack(false);
        });
        mAlertDialog = builder.create();
    }

    private void setButton() {
        ImageButton mCallButton = findViewById(R.id.CallButton);
        mCallButton.setOnClickListener(view -> {
            Log.e("sliver", "CallActivity call button clicked");
            Agent agent = mAgentListAdapter.getChooseAgent();
            if (agent != null) {
                SocketWraper.shareContext().setTarget(agent.id());
                SocketWraper.shareContext().invite();
            }
        });
    }

    private void setAgents() {
        mAgentListAdapter = new AgentListAdapter(this);
        mAgentsView.setAdapter(mAgentListAdapter);
    }

    private void jumpToNextActivity(String status) {
        Intent intent = new Intent(CallActivity.this, RTCActivity.class);
        intent.putExtra("status", status);

        Agent agent = mAgentListAdapter.getChooseAgent();
        if (agent.type().equals("Android_Camera")) {
            intent.putExtra("type", "camera");
        } else {
            intent.putExtra("type", "client");
        }

        startActivity(intent);
    }

    private void processSignal(String source, String target, String type, String value) {
        if (target.equals(SocketWraper.shareContext().getUid())) {
            if (type.equals("invite")) {
                SocketWraper.shareContext().setTarget(source);
                runOnUiThread(() -> mAlertDialog.show());
            }
            if (type.equals("ack")) {
                if(value.equals("yes")) {
                    jumpToNextActivity("send");
                }
            }
        } else {
            Log.e("sliver", "CallActivity get error target");
        }
    }

    @Override
    public void onUserAgentsUpdate(ArrayList<Agent> agents) {
        Log.e("sliver", "CallActivity onUserAgentsUpdate");
        mAgentListAdapter.reset();
        for (int i = 0; i < agents.size(); i++) {
            mAgentListAdapter.addAgent(agents.get(i));
        }
        runOnUiThread(() -> mAgentListAdapter.update());
    }

    @Override
    public void onDisConnect() {
        Log.e("sliver", "CallActivity onDisConnect");
        runOnUiThread(() -> Toast.makeText(EasyRTCApplication.getContext(), "can't connect to server", Toast.LENGTH_LONG).show());
    }

    @Override
    public void onRemoteEventMsg(String source, String target, String type, String value) {
        processSignal(source, target, type, value);
    }

    @Override
    public void onRemoteCandidate(int label, String mid, String candidate) {

    }
}