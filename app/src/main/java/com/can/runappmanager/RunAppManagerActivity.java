package com.can.runappmanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.can.runappmanager.adapter.AppManagerAdapter;
import com.can.runappmanager.bean.AppInfo;
import com.can.runappmanager.bean.PackagesInfo;
import com.can.runappmanager.utils.TextFormat;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.can.tvlib.ui.ToastUtils;
import cn.can.tvlib.ui.focus.FocusMoveUtil;

public class RunAppManagerActivity extends Activity implements View.OnFocusChangeListener {

    private static final int MSG_GET_RUNNING_PROCESS_COMPLETE = 0;
    private static final int MSA_REMOVE_ITEM = 1;
    private Dialog mLoadingDialog;
    private FocusMoveUtil mFocusMoveUtil;

    private Handler mHandler = new MyHandler(this);
    private TextView mTvRunAppCount;
    private Button mBtnClear;
    private TextView mTvNoRunApp;
    private ActivityManager mActivityManager;
    private Runnable mFocusMoveRunnable;
    private View mFocusedView;
    private List<AppInfo> mList;
    private AppManagerAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ImageView mIvBottomShadow;
    private boolean isRemoving;
    private Rect mFocusRegionRec;
    private Rect mFocusRegion;
    private CustomItemAnimator mItemAnimator;
    private ScrollSpeedLinearLayoutManger mScrollSpeedLinearLayoutManger;

    private static class MyHandler extends Handler {
        private final WeakReference<RunAppManagerActivity> mActivity;

        private MyHandler(RunAppManagerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity.get() == null) {
                return;
            }
            RunAppManagerActivity activity = mActivity.get();
            switch (msg.what) {
                case MSG_GET_RUNNING_PROCESS_COMPLETE:
                    if (activity.mList != null && activity.mList.size() > 0) {
                        activity.hideLoadingDialog();
                        activity.mTvRunAppCount.setVisibility(View.VISIBLE);
                        activity.mTvRunAppCount.setText(String.format(activity.getResources().getString(R.string
                                .run_app_count), activity.mList.size()));
                        activity.mBtnClear.setVisibility(View.VISIBLE);
                        activity.initAdapter();
                        activity.initRecyclerView();
                    } else {
                        activity.mTvNoRunApp.setVisibility(View.VISIBLE);
                    }
                    break;
                case MSA_REMOVE_ITEM:
                    if (activity.mList.size() > 0) {
                        activity.setProcessesStop(activity.mList.get(0).getPackageName());
                        activity.mList.remove(0);
                        activity.mAdapter.notifyItemRemoved(0);
                        activity.refreshRunAppCount(activity.mList.size());
                        activity.mHandler.removeMessages(MSA_REMOVE_ITEM);
                        if (activity.mList.size() > 0) {
                            activity.mHandler.sendEmptyMessageDelayed(MSA_REMOVE_ITEM, 400);
                        } else {
                            activity.mHandler.sendEmptyMessage(MSA_REMOVE_ITEM);
                        }
                    } else {
                        activity.mFocusMoveUtil.hideFocus();
                        activity.mTvRunAppCount.setVisibility(View.INVISIBLE);
                        activity.mBtnClear.setVisibility(View.INVISIBLE);
                        activity.mRecyclerView.setVisibility(View.INVISIBLE);
                        activity.mTvNoRunApp.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_app_manager);
        initView();
        initData();
    }

    private void initView() {
        showLoadingDialog();
        mTvRunAppCount = (TextView) findViewById(R.id.tv_run_count);
        mBtnClear = (Button) findViewById(R.id.btn_clear_all);
        mBtnClear.setOnFocusChangeListener(this);
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRemoving && mList != null && mList.size() > 0) {
                    mRecyclerView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    mRecyclerView.setOnFocusChangeListener(null);
                    isRemoving = true;
                    setProcessesStop(mList.get(0).getPackageName());
                    mList.remove(0);
                    mAdapter.notifyItemRemoved(0);
                    refreshRunAppCount(mList.size());
                    mHandler.removeMessages(MSA_REMOVE_ITEM);
                    mHandler.sendEmptyMessageDelayed(MSA_REMOVE_ITEM, 400);
                }
            }
        });
        mIvBottomShadow = (ImageView) findViewById(R.id.iv_bottom_shadow);
        mTvNoRunApp = (TextView) findViewById(R.id.tv_no_run_app);
        mFocusMoveUtil = new FocusMoveUtil(this, getWindow().getDecorView(), R.mipmap.btn_focus);
    }

    private void initData() {
        mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        initFocusRunnable();
        getRunningProcessData();
    }

    private void initFocusRunnable() {
        mFocusMoveRunnable = new Runnable() {
            @Override
            public void run() {
                View focusedView = RunAppManagerActivity.this.mFocusedView;
                if (focusedView == null || !focusedView.isFocused()) {
                    return;
                }
                mFocusMoveUtil.startMoveFocus(focusedView);
            }
        };
    }

    private void getRunningProcessData() {
        new Thread() {
            @Override
            public void run() {
                PackagesInfo pi = new PackagesInfo(RunAppManagerActivity.this);
                List<ActivityManager.RunningAppProcessInfo> runList = mActivityManager.getRunningAppProcesses();
                PackageManager pm = RunAppManagerActivity.this.getPackageManager();
                mList = new ArrayList<>();
                for (ActivityManager.RunningAppProcessInfo ra : runList) {
                    ApplicationInfo applicationInfo = pi.getInfo(ra.processName);
                    if (applicationInfo == null || ra.processName.equals("system") || ra.processName.equals
                            (getPackageName())) {
                        continue;
                    }
                    AppInfo appInfo = new AppInfo();
                    Debug.MemoryInfo[] memoryInfo = mActivityManager.getProcessMemoryInfo(new int[]{ra.pid});
                    appInfo.setPackageName(applicationInfo.packageName);
                    appInfo.setIcon(applicationInfo.loadIcon(pm));
                    appInfo.setName(applicationInfo.loadLabel(pm).toString());
                    appInfo.setMemory(TextFormat.formatByte(memoryInfo[0].dalvikPrivateDirty));
                    mList.add(appInfo);
                }
                mHandler.sendEmptyMessage(MSG_GET_RUNNING_PROCESS_COMPLETE);
            }
        }.start();
    }

    private void initAdapter() {
        mAdapter = new AppManagerAdapter(mList, this);
        mAdapter.setFocusListener(this);
        mAdapter.setOnItemFocusChangeListener(new AppManagerAdapter.OnItemFocusChangeListener() {
            @Override
            public void onItemFocusChange(View view, TextView title, int position) {

            }
        });
        mAdapter.setOnItemClickListener(new AppManagerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mFocusMoveUtil.hideFocusForShowDelay(1000);
                ToastUtils.showMessage(RunAppManagerActivity.this, "停止" + mList.get(position).getName() + "进程");
                mList.remove(position);
                mAdapter.notifyItemRemoved(position);
                refreshRunAppCount(mList.size());
            }
        });
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setVisibility(View.VISIBLE);
        mIvBottomShadow.setVisibility(View.VISIBLE);
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.MarginLayoutParams lpRecyclerView = (ViewGroup.MarginLayoutParams) mRecyclerView
                        .getLayoutParams();
                ViewGroup.MarginLayoutParams lpBtnClear = (ViewGroup.MarginLayoutParams) mBtnClear.getLayoutParams();
                mRecyclerView.getChildAt(0).requestFocus();
                mFocusRegion = new Rect();
                mFocusRegionRec = new Rect();
                int[] location = new int[2];
                mRecyclerView.getLocationInWindow(location);
                mFocusRegion.set(location[0], lpBtnClear.topMargin, getWindowManager().getDefaultDisplay().getWidth()
                        - lpRecyclerView.rightMargin, mRecyclerView.getHeight() + location[1] - mRecyclerView
                        .getPaddingBottom());
                mFocusRegionRec.set(location[0], location[1], mFocusRegion.right, mFocusRegion.bottom);
                setFocusActiveRegion(mFocusRegion);
                mFocusMoveUtil.hideFocusForShowDelay(500);
            }
        });
        mScrollSpeedLinearLayoutManger = new ScrollSpeedLinearLayoutManger(this);
        mScrollSpeedLinearLayoutManger.setSpeedRatio(2.0);
        mRecyclerView.setLayoutManager(mScrollSpeedLinearLayoutManger);
        mItemAnimator = new CustomItemAnimator();
        mItemAnimator.setRemoveDuration(100);
        mRecyclerView.setItemAnimator(mItemAnimator);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mHandler.removeCallbacks(mFocusMoveRunnable);
                    mHandler.post(mFocusMoveRunnable);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mHandler.removeCallbacks(mFocusMoveRunnable);
                if (dy == 0) {
                    mHandler.post(mFocusMoveRunnable);
                } else if (dy > 0) {
                    setFocusActiveRegion(mFocusRegionRec);
                    mBtnClear.setOnFocusChangeListener(null);
                } else {
                    if (mScrollSpeedLinearLayoutManger.findFirstVisibleItemPosition() == 0)
                        setFocusActiveRegion(mFocusRegion);
                    mBtnClear.setOnFocusChangeListener(RunAppManagerActivity.this);
                }
            }
        });
    }

    private void setFocusActiveRegion(Rect rect) {
        mFocusMoveUtil.setFocusActiveRegion(rect.left, rect.top, rect.right, rect.bottom);
    }

    private void refreshRunAppCount(int count) {
        mTvRunAppCount.setText(String.format(getResources().getString(R.string.run_app_count), count));
    }

    private void setProcessesStop(String pkg) {
        mActivityManager.killBackgroundProcesses(pkg);
        if (getApplicationInfo().uid == 1000) {
            try {
                Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String
                        .class);
                method.invoke(mActivityManager, pkg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = cn.can.tvlib.ui.LoadingDialog.showLoadingDialog(this, getResources()
                    .getDimensionPixelSize(R.dimen.px136));
        } else if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if (b) {
            mFocusedView = view;
            mFocusMoveRunnable.run();
        }
    }

    @Override
    protected void onDestroy() {
        hideLoadingDialog();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mItemAnimator != null) {
            mItemAnimator = null;
        }
        if (mAdapter != null) {
            mAdapter.setOnItemFocusChangeListener(null);
            mAdapter.setFocusListener(null);
            mAdapter = null;
        }
        if (mList != null) {
            mList.clear();
            mList = null;
        }
        if (mFocusMoveUtil != null) {
            mFocusMoveUtil.release();
            mFocusMoveUtil = null;
        }
        super.onDestroy();
    }
}
