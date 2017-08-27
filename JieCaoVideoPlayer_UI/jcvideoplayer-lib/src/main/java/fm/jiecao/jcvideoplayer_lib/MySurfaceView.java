package fm.jiecao.jcvideoplayer_lib;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.tqit.stereorast.render.TQITVideoRenderer;

/**
 * Created by LiaoHongjie on 2017/8/10.
 */

public class MySurfaceView extends GLSurfaceView {

    protected static final String TAG = "MySurfaceView";

    public static final int ZOOM_FULL_SCREEN_VIDEO_RATIO = 0;
    public static final int ZOOM_FULL_SCREEN_SCREEN_RATIO = 1;
    public static final int ZOOM_ORIGIN_SIZE = 2;
    public static final int ZOOM_4R3 = 3;
    public static final int ZOOM_16R9 = 4;

    private TQITVideoRenderer mVideoRenderer;
    public JCMediaManager mJCVideoPlayerManager;
    // x as width, y as height
    protected Point mVideoSize;
    private int mZoomMode = 4; // 16:9
    private JCVideoPlayer mJcVideoPlayer;

    public MySurfaceView(Context context) {
        super(context);
        init(context);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setJCMediaManager(JCMediaManager manager) {
        this.mJCVideoPlayerManager = manager;
        JCMediaManager.sSurface = mVideoRenderer.getVideoSurface();
    }

    public void setJCVideoPlayer(JCVideoPlayer player) {
        this.mJcVideoPlayer = player;
    }

    private void init(Context context) {
        setEGLContextClientVersion(2); // 设置使用的 OpenGL ES 2.0
        mVideoRenderer = new TQITVideoRenderer(context);

        setRenderer(mVideoRenderer);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        mVideoSize = new Point(0, 0);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        Log.e("TAG", "++++++++++++++   surfaceCreated   ++++++++++++++");
        // 界面可见的时候调用 surfaceCreated
        // 当处于装备状态时才能调用prepare
        if (mJcVideoPlayer.currentState == JCVideoPlayer.CURRENT_STATE_PREPARING) {
            mJCVideoPlayerManager.prepare();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
        Log.e("TAG", "++++++++++++++   surfaceChanged   ++++++++++++++");
        // 再这里重新设置
        if (mJcVideoPlayer.mLrswap.isChecked()) {
            JCMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_2D); // 必须要重新设置为默认的模式后，再设置为左右模式显示才不会模糊
            JCMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_LR);
        } else {
            JCMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_2D);
        }
        // 当 SurfaceView 的宽高发生变化时调用，如横屏与竖屏的切换
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        Log.e("TAG", "+++++++++   surfaceDestroyed   ++++++++++");
        Log.e("TAG", "surfaceDestroyed--->currentState状态: " + mJcVideoPlayer.currentState);

        // 必须要，否则会报错
        // 如果是 播放状态、暂停状态、缓冲状态
        // 不管什么状态，如果 SurfaceView被销毁了，那么保存进度
       //  if (mJcVideoPlayer.currentState == JCVideoPlayer.CURRENT_STATE_PLAYING || mJcVideoPlayer.currentState == CURRENT_STATE_PAUSE || mJcVideoPlayer.currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
//        mJcVideoPlayer.savePlayProgress(); // 保存进度
//        mJCVideoPlayerManager.mediaPlayer.pause();
//        JCMediaManager.instance().releaseMediaPlayer(); // 重置
//        mJcVideoPlayer.currentState = CURRENT_STATE_DESTROYED;
    //     }
        // 界面 被 隐藏 gaon 的时候
    }

    public void setVideoSize(Point videoSize) {
        if (videoSize != null && !mVideoSize.equals(videoSize)) {
            this.mVideoSize = videoSize;
            requestLayout();
        }
    }

    @Override
    public void setRotation(float rotation) {
        if (rotation != getRotation()) {
            super.setRotation(rotation);
            requestLayout();
        }
    }

//		TQITVideoRenderer.TQIT_VIDEO_MODE_LR
//     左右屏幕显示立体视频的格式
//		TQITVideoRenderer.TQIT_VIDEO_MODE_RL
//     左边屏幕显示立体视频的格式
//		TQITVideoRenderer.TQIT_VIDEO_MODE_2D
//		显示非立体视频的格式----> 普通模式

    /**
     * 用左、右的方式或2d模式的视频渲染。
     *
     * @param mode
     * @return
     */
    public void setMode(int mode) {
        if (mVideoRenderer != null)
            mVideoRenderer.setParam(mode);
    }

    public int getZoomMode() {
        return mZoomMode;
    }

    /**
     * 设置缩放模式
     *
     * @param mode
     */
    public void setZoomMode(int mode) {
        if (mode == mZoomMode) {
            return;
        }
        mZoomMode = mode;
        if (mVideoSize.x > 0 && mVideoSize.y > 0) {
            getHolder().setFixedSize(mVideoSize.x, mVideoSize.y);
            requestLayout();
        }
    }
}
