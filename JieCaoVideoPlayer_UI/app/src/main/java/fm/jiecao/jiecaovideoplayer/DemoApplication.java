package fm.jiecao.jiecaovideoplayer;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.danikula.videocache.HttpProxyCacheServer;
import com.squareup.leakcanary.LeakCanary;

import java.io.File;

/**
 * Created by Nathen
 * On 2015/12/01 11:29
 */
public class DemoApplication extends Application {

    private HttpProxyCacheServer proxy;

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);

        //it is public static, you can set this everywhere
        //JCVideoPlayer.TOOL_BAR_EXIST = false;
        //JCVideoPlayer.ACTION_BAR_EXIST = false;
    }

    public HttpProxyCacheServer getProxy(Context context) {
        DemoApplication app = (DemoApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                .cacheDirectory(new File(Environment.getExternalStorageDirectory(), "UI/video_cache"))
                .maxCacheFilesCount(5) //设置数量
                .build();
    }
}
