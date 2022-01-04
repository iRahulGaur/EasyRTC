package com.cx.easyrtc.activity

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cx.easyrtc.EasyRTCApplication
import com.cx.easyrtc.R
import com.cx.easyrtc.agent.Agent
import com.cx.easyrtc.agent.AgentListAdapter
import com.cx.easyrtc.socket.SocketWraper
import com.cx.easyrtc.socket.SocketWraper.SocketDelegate
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import java.util.*

class CallActivity : AppCompatActivity(), SocketDelegate {

    private var mAgentsView: ListView? = null
    private var mAgentListAdapter: AgentListAdapter? = null
    private var mAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        Log.e("skruazzz", "CallActivity onCreate")

        askPermissions()
        setUI()
        setAgents()
        setSocket()
        updateRemoteAgent()
    }

    private fun askPermissions() {
        permissionsBuilder(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).build()
            .send { result ->
                if (result.allGranted()) {
                    Toast.makeText(
                        this,
                        "Permissions are working correctly",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val builder = AlertDialog.Builder(this@CallActivity)
                    builder.setTitle("Permission required")
                    builder.setMessage("Camera and mic permission is required to make calls")
                    builder.setPositiveButton("ok") { _: DialogInterface?, _: Int ->
                        Log.e("askPermissions", "show permission again")
                        askPermissions()
                    }
                    builder.setNegativeButton("exit app") { _: DialogInterface?, _: Int ->
                        Log.e("askPermissions", "exit app")
                        finish()
                    }
                    mAlertDialog = builder.create()
                    Toast.makeText(
                        this,
                        "These permissions are necessary to work",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

    }

    override fun onStop() {
        Log.e("skruazzz", "CallActivity onStop")
        SocketWraper.shareContext().removeListener(this)
        super.onStop()
    }

    private fun setSocket() {
        SocketWraper.shareContext().addListener(this)
    }

    private fun updateRemoteAgent() {
        SocketWraper.shareContext().updateRemoteAgent()
    }

    private fun setUI() {
        mAgentsView = findViewById(R.id.AgentsView)
        setAlertDialog()
        setButton()
    }

    private fun setAlertDialog() {
        val builder = AlertDialog.Builder(this@CallActivity)
        builder.setTitle("Received call")
        builder.setPositiveButton("Answer") { _: DialogInterface?, _: Int ->
            Log.e("sliver", "CallActivity accept invite")
            jumpToNextActivity("recv")
        }
        builder.setNegativeButton("hang up") { _: DialogInterface?, _: Int ->
            Log.e("sliver", "CallActivity refuse invite")
            SocketWraper.shareContext().ack(false)
        }
        mAlertDialog = builder.create()
    }

    private fun setButton() {
        val mCallButton = findViewById<ImageButton>(R.id.CallButton)
        mCallButton.setOnClickListener {
            Log.e("sliver", "CallActivity call button clicked")
            val agent = mAgentListAdapter!!.chooseAgent
            if (agent != null) {
                SocketWraper.shareContext().target = agent.id()
                SocketWraper.shareContext().invite()
            }
        }
    }

    private fun setAgents() {
        mAgentListAdapter = AgentListAdapter(this)
        mAgentsView!!.adapter = mAgentListAdapter
    }

    private fun jumpToNextActivity(status: String) {
        val intent = Intent(this@CallActivity, RTCActivity::class.java)
        intent.putExtra("status", status)
        val agent = mAgentListAdapter!!.chooseAgent
        if (agent.type() == "Android_Camera") {
            intent.putExtra("type", "camera")
        } else {
            intent.putExtra("type", "client")
        }
        startActivity(intent)
    }

    private fun processSignal(source: String, target: String, type: String, value: String) {
        if (target == SocketWraper.shareContext().uid) {
            if (type == "invite") {
                SocketWraper.shareContext().target = source
                runOnUiThread { mAlertDialog!!.show() }
            }
            if (type == "ack") {
                if (value == "yes") {
                    jumpToNextActivity("send")
                }
            }
        } else {
            Log.e("sliver", "CallActivity get error target")
        }
    }

    override fun onUserAgentsUpdate(agents: ArrayList<Agent>) {
        Log.e("sliver", "CallActivity onUserAgentsUpdate")
        mAgentListAdapter!!.reset()
        for (i in agents.indices) {
            mAgentListAdapter!!.addAgent(agents[i])
        }
        runOnUiThread { mAgentListAdapter!!.update() }
    }

    override fun onDisConnect() {
        Log.e("sliver", "CallActivity onDisConnect")
        runOnUiThread {
            Toast.makeText(
                EasyRTCApplication.getContext(),
                "can't connect to server",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onRemoteEventMsg(source: String, target: String, type: String, value: String) {
        processSignal(source, target, type, value)
    }

    override fun onRemoteCandidate(label: Int, mid: String, candidate: String) {}
}