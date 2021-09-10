package cn.dreamn.qianji_auto.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hjq.toast.ToastUtils;
import com.tencent.mmkv.MMKV;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.database.DbManger;
import cn.dreamn.qianji_auto.database.Helper.Caches;
import cn.dreamn.qianji_auto.database.Table.CategoryName;
import cn.dreamn.qianji_auto.setting.AppStatus;
import cn.dreamn.qianji_auto.utils.runUtils.Cmd;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.Task;

public class QianJi implements IApp {
    private static QianJi qianJi;

    public static QianJi getInstance(){
        if(qianJi==null)
            qianJi=new QianJi();
        return qianJi;
    }
    @Override
    public String getPackPageName() {
        return "com.mutangtech.qianji";
    }

    @Override
    public String getAppName() {
        return "钱迹";
    }



    @Override
    public int getAppIcon() {
        return R.drawable.logo_qianji;
    }

    @Override
    public void sendToApp(Context context,BillInfo billInfo) {
        Handler mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    Caches.AddOrUpdate("show_tip", "false");
                    Caches.AddOrUpdate("float_time", String.valueOf(System.currentTimeMillis()));
                    //    Tool.goUrl(context, getQianJi(billInfo));

                    Cmd.exec(new String[]{"am start \"" + getQianJi(billInfo) + "\""});

                    ToastUtils.show("记账成功！金额￥" + billInfo.getMoney() + "。");
                }
            }
        };
        //如果不是xp模式需要延时
        if (!AppStatus.xposedActive(context)) {
            delay(context, mHandler);
        } else {
            mHandler.sendEmptyMessage(0);
        }

    }

    @Override
    public void asyncDataBefore(Context context) {
        if (AppStatus.xposedActive(context)) {
            Cmd.exec(new String[]{"am force-stop com.mutangtech.qianji"});
            //杀死其他应用
            //  Tool.stopApp(context,"com.mutangtech.qianji");
            Intent intent = new Intent();
            intent.setClassName("com.mutangtech.qianji", "com.mutangtech.qianji.ui.main.MainActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("needAsync", "true");
            MMKV mmkv = MMKV.defaultMMKV();
            mmkv.encode("needAsync", true);
            context.startActivity(intent);
        } else {
            ToastUtils.show("钱迹只支持已激活用户同步，无障碍用户需要手动配置。");
        }
    }

    @Override
    public void asyncDataAfter(Context context, Bundle extData) {
        String json = extData.getString("data");
        JSONObject jsonObject = JSONObject.parseObject(json);
        //Toasty.info(context,"收到钱迹数据！正在后台同步中...").show();
        JSONArray asset = jsonObject.getJSONArray("asset");
        JSONArray category = jsonObject.getJSONArray("category");
        JSONArray userBook = jsonObject.getJSONArray("userBook");
        JSONArray billInfo = jsonObject.getJSONArray("billInfo");

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                ToastUtils.show(R.string.async_success);
            }
        };

        if (asset == null || category == null || userBook == null) {
            Log.i("钱迹数据信息无效");
            return;
        }

        Task.onThread(() -> {
            DbManger.db.CategoryNameDao().clean();
            for (int i = 0; i < category.size(); i++) {
                JSONObject jsonObject1 = category.getJSONObject(i);
                String name = jsonObject1.getString("name");
                String icon = jsonObject1.getString("icon");
                String level = jsonObject1.getString("level");
                String type = jsonObject1.getString("type");
                String self_id = jsonObject1.getString("id");
                String parent_id = jsonObject1.getString("parent");
                String book_id = jsonObject1.getString("book_id");
                String sort = jsonObject1.getString("sort");
                if (self_id == null || self_id.equals("")) {
                    self_id = String.valueOf(System.currentTimeMillis());
                }
                if (sort == null || sort.equals("")) {
                    sort = "500";
                }
                String self = self_id;
                String s = sort;
                CategoryName[] categoryNames = DbManger.db.CategoryNameDao().getByName(name, type, book_id);
                if (categoryNames.length != 0) {
                    break;
                }
                DbManger.db.CategoryNameDao().add(name, icon, level, type, self, parent_id, book_id, s);

            }
            Log.i("分类数据处理完毕");


            //资产数据处理
            DbManger.db.Asset2Dao().clean();

            for (int i = 0; i < asset.size(); i++) {
                JSONObject jsonObject1 = asset.getJSONObject(i);
                Log.d(jsonObject1.getString("name") + "->type->" + jsonObject1.getString("type"));
                if (jsonObject1.getString("type").equals("5"))
                    continue;
                DbManger.db.Asset2Dao().add(jsonObject1.getString("name"), jsonObject1.getString("icon"), jsonObject1.getInteger("sort"));

            }
            Log.i("资产数据处理完毕");

            DbManger.db.BookNameDao().clean();
            for (int i = 0; i < userBook.size(); i++) {
                JSONObject jsonObject1 = userBook.getJSONObject(i);

                String bookName = jsonObject1.getString("name");
                String icon = jsonObject1.getString("cover");
                String bid = jsonObject1.getString("id");
                if (bid == null || bid.equals("")) {
                    bid = String.valueOf(System.currentTimeMillis());
                }
                DbManger.db.BookNameDao().add(bookName, icon, bid);

            }

            Log.i("账本数据处理完毕");

            mHandler.sendEmptyMessage(0);
        });

        Cmd.exec(new String[]{"am force-stop com.mutangtech.qianji"});
        //获取账单数据
        // TODO 插入数据库

    }

    private void delay(Context context, Handler mHandler) {

        Task.onThread(() -> {
            Caches.getCacheData("float_time", "", cache -> {
                if (!cache.equals("")) {
                    long time = Long.parseLong(cache);
                    long t = System.currentTimeMillis() - time;
                    t = t / 1000;
                    if (t < 4) {
                        long finalT = t;
                        Caches.getCacheData("show_tip", "false", cache1 -> {
                            if (!cache1.equals("true")) {
                                Looper.prepare();
                                ToastUtils.show("距离上一次发起请求时间为" + finalT + "s,稍后为您自动记账。");
                                Looper.loop();
                                Caches.AddOrUpdate("show_tip", "true");
                            }
                            new Handler().postDelayed(() -> {
                                delay(context, mHandler);
                            }, finalT * 100);
                        });
                        return;

                    }

                }
                mHandler.sendEmptyMessage(0);
            });
        });
    }

    public String getQianJi(BillInfo billInfo) {


        String url = "qianji://publicapi/addbill?&type=" + billInfo.getType(true) + "&money=" + billInfo.getMoney();
        MMKV mmkv = MMKV.defaultMMKV();
        //懒人模式
        if (mmkv.getBoolean("lazy_mode", true)) {
            return url;
        }

        if (billInfo.getTime() != null) {
            url += "&time=" + billInfo.getTime();
        }
        if (billInfo.getRemark() != null) {
            url += "&remark=" + billInfo.getRemark();
        }
        if (billInfo.getCateName() != null) {
            url += "&catename=" + billInfo.getCateName();
        }
        url += "&catechoose=" + billInfo.getCateChoose();

        url += "&catetheme=auto";

        if (billInfo.getBookName()!= null && !billInfo.getBookName().equals("默认账本")) {
            url += "&bookname=" + billInfo.getBookName();
        }

        if (billInfo.getAccountName() != null && !billInfo.getAccountName().equals("")) {
            url += "&accountname=" + billInfo.getAccountName();
        }
        if (billInfo.getAccountName2() != null && !billInfo.getAccountName2().equals("")) {
            url += "&accountname2=" + billInfo.getAccountName2();
        }
        if (billInfo.getFee() != null && !billInfo.getFee().equals("")) {
            url += "&fee=" + billInfo.getFee();
        }
        Log.i("钱迹URL:" + url);
        return url;
    }


    @Override
    public String getAsyncDesc(Context context) {
        if (AppStatus.xposedActive(context)) {
            return context.getResources().getString(R.string.qianji_async_desc);
        }
        return context.getResources().getString(R.string.qianji_async_no_support);
    }
}
