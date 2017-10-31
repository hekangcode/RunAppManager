package com.can.runappmanager.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.can.runappmanager.R;
import com.can.runappmanager.bean.AppInfo;

import java.util.List;

/**
 * Created by HEKANG on 2017/5/19.
 */

public class AppManagerAdapter extends RecyclerView.Adapter<AppManagerAdapter.MyViewHolder> {

    private Context context;
    private List<AppInfo> mList;
    private LayoutInflater mLayoutInflater;

    private View.OnFocusChangeListener mFocusListener;
    private OnItemFocusChangeListener mOnItemFocusChangeListener;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void setFocusListener(View.OnFocusChangeListener focusListener) {
        this.mFocusListener = focusListener;
    }

    public interface OnItemFocusChangeListener {
        void onItemFocusChange(View view, TextView title, int position);
    }

    public void setOnItemFocusChangeListener(OnItemFocusChangeListener itemFocusChangeListener) {
        this.mOnItemFocusChangeListener = itemFocusChangeListener;
    }

    public AppManagerAdapter(List<AppInfo> list, Context context) {
        this.context = context;
        this.mList = list;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AppManagerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        View view = mLayoutInflater.inflate(R.layout.item_run_app_manager, parent, false);
        view.setOnFocusChangeListener(mFocusListener);
        return new AppManagerAdapter.MyViewHolder(view, mOnItemFocusChangeListener, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(final AppManagerAdapter.MyViewHolder holder, final int position) {
        AppInfo app = mList.get(position);
        if (app.getIcon() != null) {
            holder.ivIcon.setImageDrawable(app.getIcon());
        } else {
            holder.ivIcon.setImageResource(R.mipmap.ic_launcher);
        }
        holder.tvName.setText(app.getName());
        holder.tvMemory.setText(String.format(context.getResources().getString(R.string.app_memory_rate), app
                .getMemory()));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnFocusChangeListener {

        private final OnItemFocusChangeListener mItemFocusChangeListener;
        private final OnItemClickListener mOnItemClickListener;
        private ImageView ivIcon;
        private TextView tvName, tvMemory;
        private RelativeLayout rlItemApp;

        private MyViewHolder(View view, OnItemFocusChangeListener mOnItemFocusChangeListener,
                             OnItemClickListener mOnItemClickListener) {
            super(view);
            this.mItemFocusChangeListener = mOnItemFocusChangeListener;
            this.mOnItemClickListener = mOnItemClickListener;
            ivIcon = (ImageView) view.findViewById(R.id.iv_icon);
            tvName = (TextView) view.findViewById(R.id.tv_name);
            tvMemory = (TextView) view.findViewById(R.id.tv_memory);
            rlItemApp = (RelativeLayout) view.findViewById(R.id.rl_item);
            rlItemApp.setOnFocusChangeListener(this);
            rlItemApp.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int idView = v.getId();
            if (idView == R.id.rl_item) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(getLayoutPosition());
                }
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (mFocusListener != null) {
                mFocusListener.onFocusChange(v, hasFocus);
            }
            if (v.getId() == R.id.rl_item && mItemFocusChangeListener != null) {
                mItemFocusChangeListener.onItemFocusChange(v, tvName, getLayoutPosition());
            }
        }
    }
}
