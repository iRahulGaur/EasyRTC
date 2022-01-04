package com.cx.easyrtc.agent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.cx.easyrtc.R;
import com.cx.easyrtc.socket.SocketWraper;

import java.util.ArrayList;

/**
 * Created by cx on 2018/12/14.
 */

public class AgentListAdapter extends BaseAdapter{

//    private final String TAG = AgentListAdapter.class.getName();

    private final ArrayList<Agent> mAgents;

    private final LayoutInflater mLayoutInflater;

    private int mChooseItemIdx;

    private View mChooseView;

    public AgentListAdapter(Context context) {
        super();
        mLayoutInflater = LayoutInflater.from(context);
        mAgents = new ArrayList<>();
        mChooseItemIdx = 0;
    }

    public void addAgent(Agent agent) {
        mAgents.add(agent);
    }

/*
    public void removeAgent(Agent agent) {
        mAgents.remove(agent);
    }
*/

    public void update() {
        this.notifyDataSetChanged();
    }

    public void reset() {
        mAgents.clear();
    }

    public Agent getChooseAgent() {
        if (mAgents.isEmpty())
            return null;

        return mAgents.get(mChooseItemIdx);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null)
            view = mLayoutInflater.inflate(R.layout.agent_list_cell, null);

        TextView textView = view.findViewById(R.id.agent_list_cell_tv);
        String showText = "name:" + mAgents.get(i).name() + " type:" + mAgents.get(i).type();
        textView.setText(showText);
        if (mAgents.get(i).id().equals(SocketWraper.shareContext().getUid())) {
            textView.setTextColor(Color.BLUE);
        } else {
            textView.setTextColor(Color.RED);
        }
        textView.setTag(i);
        textView.setBackgroundColor(Color.WHITE);

        textView.setOnClickListener(view1 -> {
            if (mChooseView != null) {
                mChooseView.setBackgroundColor(Color.WHITE);
            }
            view1.setBackgroundColor(Color.GRAY);
            mChooseItemIdx = (Integer) view1.getTag();
            mChooseView = view1;
            Log.e("sliver", "AgentListAdapter choose item idx:" + mChooseItemIdx);
        });

        return view;
    }

    @Override
    public int getCount() {
        return mAgents.size();
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Nullable
    @Override
    public CharSequence[] getAutofillOptions() {
        return new CharSequence[0];
    }

    @Override
    public Object getItem(int i) {
        return null;
    }
}
