package fm.jiecao.jiecaovideoplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.squareup.picasso.Picasso;

import java.io.File;

import fm.jiecao.jcvideoplayer_lib.JCUserAction;
import fm.jiecao.jcvideoplayer_lib.JCUserActionStandard;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;

/**
 * Created by Nathen on 16/7/22.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    JCVideoPlayerStandard myJCVideoPlayerStandard;

    Button mTinyWindow, mAutoTinyWindow, mAboutListView, mPlayDirectly, mAboutApi, mAboutWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTinyWindow = (Button) findViewById(R.id.tiny_window);
        mAutoTinyWindow = (Button) findViewById(R.id.auto_tiny_window);
        mPlayDirectly = (Button) findViewById(R.id.play_directly_without_layout);
        mAboutListView = (Button) findViewById(R.id.about_listview);
        mAboutApi = (Button) findViewById(R.id.about_api);
        mAboutWebView = (Button) findViewById(R.id.about_webview);


        mTinyWindow.setOnClickListener(this);
        mAutoTinyWindow.setOnClickListener(this);
        mAboutListView.setOnClickListener(this);
        mPlayDirectly.setOnClickListener(this);
        mAboutApi.setOnClickListener(this);
        mAboutWebView.setOnClickListener(this);

        myJCVideoPlayerStandard = (JCVideoPlayerStandard) findViewById(R.id.jc_video);

        // myJCVideoPlayerStandard.setUp("http://video.jiecao.fm/11/23/xu/%E5%A6%B9%E5%A6%B9.mp4", JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL, "嫂子快长大");

        String video_url = "http://www.tutu3d.cn/video/Airplanelegend.mp4";

        DemoApplication application = (DemoApplication) getApplication();

        HttpProxyCacheServer proxy = application.getProxy(getApplicationContext());
        proxy.registerCacheListener(new CacheListener() {
            @Override
            public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
                Log.e("TAG", "视频缓存进度："+percentsAvailable);
            }
        }, video_url);

        // 返回拿到的是本地地址
        String proxyUrl = proxy.getProxyUrl(video_url);

        // 判断是否缓存过
        if (proxy.isCached(video_url)) {
            Log.e("TAG", "当前视频已被缓存过.....");
        }else{
            Log.e("TAG", "没有被缓存");
        }

        myJCVideoPlayerStandard.setUp(proxyUrl, JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL, "测试视频");

        Picasso.with(this).load("http://img4.jiecaojingxuan.com/2016/11/23/1bb2ebbe-140d-4e2e-abd2-9e7e564f71ac.png@!640_360").into(myJCVideoPlayerStandard.thumbImageView);

        myJCVideoPlayerStandard.setJcUserAction(new MyUserActionStandard());

        myJCVideoPlayerStandard.startVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        JCVideoPlayer.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if (JCVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tiny_window:
//                myJCVideoPlayerStandard.onStatePreparingChangingUrl(1, 0);
                break;
            case R.id.auto_tiny_window:
                startActivity(new Intent(MainActivity.this, AutoTinyActivity.class));
                break;
            case R.id.play_directly_without_layout:
                startActivity(new Intent(MainActivity.this, PlayDirectlyActivity.class));
                break;
            case R.id.about_listview:
                startActivity(new Intent(MainActivity.this, ListViewActivity.class));
                break;
            case R.id.about_api:
                startActivity(new Intent(MainActivity.this, ApiActivity.class));
                break;
            case R.id.about_webview:
                startActivity(new Intent(MainActivity.this, WebViewActivity.class));
                break;
        }
    }

    class MyUserActionStandard implements JCUserActionStandard {
        @Override
        public void onEvent(int type, int screen, Object... objects) {
            switch (type) {
                case JCUserAction.ON_CLICK_START_ICON:
                    Log.e("USER_EVENT", "ON_CLICK_START_ICON" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_CLICK_START_ERROR:
                    Log.e("USER_EVENT", "ON_CLICK_START_ERROR" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_CLICK_START_AUTO_COMPLETE:
                    Log.e("USER_EVENT", "ON_CLICK_START_AUTO_COMPLETE" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_CLICK_PAUSE:
                    Log.e("USER_EVENT", "ON_CLICK_PAUSE" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_CLICK_RESUME:
                    Log.e("USER_EVENT", "ON_CLICK_RESUME" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_SEEK_POSITION:
                    Log.e("USER_EVENT", "ON_SEEK_POSITION" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_AUTO_COMPLETE:
                    Log.e("USER_EVENT", "ON_AUTO_COMPLETE" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_ENTER_FULLSCREEN:
                    Log.e("USER_EVENT", "ON_ENTER_FULLSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_QUIT_FULLSCREEN:
                    Log.e("USER_EVENT", "ON_QUIT_FULLSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_ENTER_TINYSCREEN:
                    Log.e("USER_EVENT", "ON_ENTER_TINYSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_QUIT_TINYSCREEN:
                    Log.e("USER_EVENT", "ON_QUIT_TINYSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_TOUCH_SCREEN_SEEK_VOLUME:
                    Log.e("USER_EVENT", "ON_TOUCH_SCREEN_SEEK_VOLUME" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserAction.ON_TOUCH_SCREEN_SEEK_POSITION:
                    Log.e("USER_EVENT", "ON_TOUCH_SCREEN_SEEK_POSITION" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserActionStandard.ON_CLICK_START_THUMB:
                    Log.e("USER_EVENT", "ON_CLICK_START_THUMB" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                case JCUserActionStandard.ON_CLICK_BLANK:
                    Log.e("USER_EVENT", "ON_CLICK_BLANK" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : "  + " screen is : " + screen);
                    break;
                default:
                    Log.e("USER_EVENT", "unknow");
                    break;
            }
        }
    }

}
