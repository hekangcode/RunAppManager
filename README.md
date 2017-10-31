#RunAppManager

###一款Android TV 端的运行应用管理，在获取系统权限后，可以停止该进程

###停止进程方法，采用反射

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