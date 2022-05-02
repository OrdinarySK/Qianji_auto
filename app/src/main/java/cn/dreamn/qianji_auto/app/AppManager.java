package cn.dreamn.qianji_auto.app;

import android.content.Context;
import android.os.Bundle;

import com.hjq.toast.ToastUtils;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;

import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.core.broadcast.AppBroadcast;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.Tool;

public class AppManager {
    /**
     * 获取自动记账支持的所有APP
     *
     * @return
     */
    public static Bundle[] getAllApps() {
        try {
            List<Bundle> mList = new ArrayList<>();
            for (IApp iApp : AppList.getInstance().getList()) {
                Bundle bundle = new Bundle();
                bundle.putString("appName", iApp.getAppName());
                bundle.putString("appPackage", iApp.getPackPageName());
                bundle.putInt("appIcon", iApp.getAppIcon());
                mList.add(bundle);
            }
            return mList.toArray(new Bundle[0]);
        } catch (Exception ignored) {

        }

        return null;
    }


    /**
     * 发送数据给支持的app
     *
     * @param billInfo
     */
    public static void sendToApp(Context context, BillInfo billInfo) {
        String app = getApp();
        try {
            for (IApp iApp : AppList.getInstance().getList()) {
                if (iApp.getPackPageName().equals(app)) {
                    iApp.sendToApp(context, billInfo);
                    break;
                }
            }
        } catch (Exception e) {
            Tool.goToCoolMarket(context, app);
            ToastUtils.show("请下载对应的App");
        }
    }

    /*
     * 进行数据同步
     */
    public static void Async(Context context, int type) {
        MMKV mmkv = MMKV.defaultMMKV();
        mmkv.encode("AutoSignal", type);//设置为同步
        String app = getApp();
        //   Log.i("选择的App",app);
        try {
            for (IApp iApp : AppList.getInstance().getList()) {
                // Log.i("遍历的App",iApp.getPackPageName());
                if (iApp.getPackPageName().equals(app)) {
                    iApp.asyncDataBefore(context, type);
                    break;
                }
            }
        } catch (Exception e) {
            Tool.goToCoolMarket(context, app);
            ToastUtils.show("请下载对应的App");
        }

    }

    /*
     * 进行数据同步
     */
    public static void AsyncEnd(Context context, Bundle bundle, int type) {
        MMKV mmkv = MMKV.defaultMMKV();
        mmkv.encode("AutoSignal", AppBroadcast.BROADCAST_NOTHING);//设置为未同步
        String app = getApp();
        for (IApp iApp : AppList.getInstance().getList()) {
            if (iApp.getPackPageName().equals(app)) {
                Log.d("收到广播的同步消息");
                iApp.asyncDataAfter(context, bundle, type);
                break;
            }
        }

    }

    public static void setApp(String appPackage) {
        MMKV mmkv = MMKV.defaultMMKV();
        mmkv.encode("bookApp", appPackage);
    }

    public static String getApp() {
        MMKV mmkv = MMKV.defaultMMKV();
        return mmkv.getString("bookApp", "com.mutangtech.qianji");
    }


    public static Bundle getAppInfo(Context context) {
        String app = getApp();
        for (IApp iApp : AppList.getInstance().getList()) {
            if (iApp.getPackPageName().equals(app)) {
                Bundle bundle = new Bundle();
                bundle.putString("appName", iApp.getAppName());
                bundle.putString("appPackage", iApp.getPackPageName());
                bundle.putInt("appIcon", iApp.getAppIcon());
                bundle.putString("appAsync", iApp.getAsyncDesc(context));
                return bundle;
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString("appName", "钱迹");
        bundle.putString("appPackage", "com.mutangtech.qianji");
        bundle.putInt("appIcon", 0);
        bundle.putString("appAsync", "");
//        Bundle bundle = new Bundle();
//        bundle.putString("appName", "Ordinary°");
//        bundle.putString("appPackage", "com.sk.ordinary");
//        bundle.putInt("appIcon", 0);
//        bundle.putString("appAsync", "");
        return bundle;
    }

    public static void rei(Context context) {

    }


}
