package com.kongzue.dialogx.impl;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.kongzue.dialogx.interfaces.BaseDialog;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.kongzue.dialogx.DialogX.error;

/**
 * @author: Kongzue
 * @github: https://github.com/kongzue/
 * @homepage: http://kongzue.com/
 * @mail: myzcxhh@live.cn
 * @createTime: 2020/9/22 11:31
 */
public class ActivityLifecycleImpl implements Application.ActivityLifecycleCallbacks {
    
    private onActivityResumeCallBack onActivityResumeCallBack;
    
    public ActivityLifecycleImpl(ActivityLifecycleImpl.onActivityResumeCallBack onActivityResumeCallBack) {
        this.onActivityResumeCallBack = onActivityResumeCallBack;
    }
    
    public static void init(Context context, ActivityLifecycleImpl.onActivityResumeCallBack onActivityResumeCallBack) {
        Application application = getApplicationContext(context);
        if (application == null) {
            error("DialogX 未初始化。\n请检查是否在启动对话框前进行初始化操作，使用以下代码进行初始化：\nDialogX.init(context);\n\n另外建议您前往查看 DialogX 的文档进行使用：https://github.com/kongzue/DialogX");
            return;
        }
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleImpl(onActivityResumeCallBack));
    }
    
    public static Application getApplicationContext(Context context) {
        if (context != null) {
            return (Application) context.getApplicationContext();
        }
        try {
            Application application = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            return application;
        } catch (Exception e) {
        }
        try {
            Application application = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null, (Object[]) null);
            return application;
        } catch (Exception e) {
        }
        error("DialogX.init: 初始化异常，请确保init方法内传入的Context是有效的。");
        return null;
    }
    
    public static Activity getTopActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<Object, Object> activities;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activities = (HashMap<Object, Object>) activitiesField.get(activityThread);
            } else {
                activities = (ArrayMap<Object, Object>) activitiesField.get(activityThread);
            }
            if (activities.size() < 1) {
                return null;
            }
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        if (onActivityResumeCallBack != null) {
            onActivityResumeCallBack.getActivity(activity);
        }
    }
    
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    
    }
    
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity.isDestroyed() || activity.isFinishing()) {
            return;
        }
        if (onActivityResumeCallBack != null) {
            onActivityResumeCallBack.getActivity(activity);
        }
    }
    
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }
    
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }
    
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    
    }
    
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (BaseDialog.getContext() == activity) {
            BaseDialog.cleanContext();
        }
    }
    
    @Override
    public void onActivityPreDestroyed(@NonNull final Activity activity) {
        BaseDialog.recycleDialog(activity);
    }
    
    public interface onActivityResumeCallBack {
        void getActivity(Activity activity);
    }
}
