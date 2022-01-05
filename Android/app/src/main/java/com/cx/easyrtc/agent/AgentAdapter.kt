package com.cx.easyrtc.agent

import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cx.easyrtc.R
import com.cx.easyrtc.socket.SocketWrapper

// created by Rahul Gaur : 05/01/22
class AgentAdapter : RecyclerView.Adapter<AgentAdapter.ViewHolder>() {

    private val agentList: MutableList<Agent> = ArrayList()

    private var mChooseItemIdx: Int = 0
    private var mChooseView: View? = null

    val chooseAgent: Agent?
        get() = if (agentList.isEmpty()) null else agentList[mChooseItemIdx]

    fun addAgent(agent: Agent) {
        agentList.add(agent)
    }

    fun update() {
        notifyDataSetChanged()
    }

    fun reset() {
        agentList.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.agent_list_cell, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val agent = agentList[position]
        var showText = "name:" + agent.name + " type:" + agent.type
        if (agent.name.contains(Build.MODEL)) {
            showText += " (current)"
        }
        holder.agentListCellTv.text = showText
        if (agent.id == SocketWrapper.shareContext().uid) {
            holder.agentListCellTv.setTextColor(Color.BLUE)
        } else {
            holder.agentListCellTv.setTextColor(Color.RED)
        }
        holder.agentListCellTv.tag = position
        holder.agentListCellTv.setBackgroundColor(Color.WHITE)
        holder.agentListCellTv.setOnClickListener { view1: View ->
            if (mChooseView != null) {
                mChooseView!!.setBackgroundColor(Color.WHITE)
            }
            view1.setBackgroundColor(Color.GRAY)
            mChooseItemIdx = view1.tag as Int
            mChooseView = view1
            Log.e("sliver", "AgentListAdapter choose item idx:$mChooseItemIdx")
        }
    }

    override fun getItemCount(): Int {
        return agentList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val agentListCellTv: TextView = itemView.findViewById(R.id.agent_list_cell_tv)
    }

}