package fm.jiecao.jcvideoplayer_lib;

import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * <p>统一管理MediaPlayer的地方,只有一个mediaPlayer实例，那么不会有多个视频同时播放，也节省资源。</p>
 * <p>Unified management MediaPlayer place, there is only one MediaPlayer instance, then there will be no more video broadcast at the same time, also save resources.</p>
 * Created by Nathen
 * On 2015/11/30 15:39
 */
public class JCMediaManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {
    public static String TAG = "JieCaoVideoPlayer";

    private static JCMediaManager JCMediaManager;

    public MySurfaceView sSurfaceView;
    public Surface sSurface;

    public String current_playing_url; // 播放地址
    public Map<String, String> map_header_data;
    public boolean current_pling_loop;
    private MediaPlayer mediaPlayer;
    public int currentVideoWidth = 0;
    public int currentVideoHeight = 0;

    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_RELEASE = 2;
    public boolean isNetworkResource = true; //是否是网络资源

    HandlerThread mMediaHandlerThread;
    MediaHandler mMediaHandler;
    Handler mainThreadHandler;


    public static JCMediaManager instance() {
        if (JCMediaManager == null) {
            JCMediaManager = new JCMediaManager();
        }
        return JCMediaManager;
    }

    public JCMediaManager() {
        mMediaHandlerThread = new HandlerThread(TAG);  // 这里用的好
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler(mMediaHandlerThread.getLooper()); // 所有message消息都是在子线程中执行
        mainThreadHandler = new Handler(Looper.myLooper());
    }

    public Point getVideoSize() {
        if (currentVideoWidth != 0 && currentVideoHeight != 0) {
            return new Point(currentVideoWidth, currentVideoHeight);
        } else {
            return null;
        }
    }

    public class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    try {
                        currentVideoWidth = 0;
                        currentVideoHeight = 0;
                        if (mediaPlayer != null) {
                            mediaPlayer.release();
                        }
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setSurface(sSurface);
                        // 是否循环播放
                        mediaPlayer.setLooping(current_pling_loop);
                        // 是否使用SurfaceHolder来显示
                        mediaPlayer.setScreenOnWhilePlaying(false);
                        // 视频资源加载回调监听
                        mediaPlayer.setOnPreparedListener(JCMediaManager.this);
                        // 播放完成监听
                        mediaPlayer.setOnCompletionListener(JCMediaManager.this);
                        // 缓冲更新监听
                        mediaPlayer.setOnBufferingUpdateListener(JCMediaManager.this);
                        // seekto定位监听
                        mediaPlayer.setOnSeekCompleteListener(JCMediaManager.this);
                        // 播放错误监听
                        mediaPlayer.setOnErrorListener(JCMediaManager.this);
                        // 指示信息和警告信息监听
                        mediaPlayer.setOnInfoListener(JCMediaManager.this);
                        // 视频大小变化监听
                        mediaPlayer.setOnVideoSizeChangedListener(JCMediaManager.this);

                        if (isNetworkResource) {
                            Class<MediaPlayer> clazz = MediaPlayer.class;
                            Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
                            method.invoke(mediaPlayer, current_playing_url, map_header_data);
                        } else {
                            mediaPlayer.setDataSource(current_playing_url); // 播放本地资源
                        }

                        mediaPlayer.prepareAsync();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case HANDLER_RELEASE:
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    break;
            }
        }
    }

    /**
     * 创建 MediaPlayer ，并异步准备
     */
    public void prepare() {
        releaseMediaPlayer();
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        mMediaHandler.sendMessage(msg);
    }

    /**
     * 释放资源
     */
    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        mMediaHandler.sendMessage(msg);
    }

    /**
     * 多媒体资源加载准备完成回调
     *
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
    }

    /**
     * 播放完成回调
     *
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getFirstFloor() != null) {
                    JCVideoPlayerManager.getFirstFloor().onAutoCompletion();
                }
            }
        });
    }

    public void pause() {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.pause();
            }
        });
    }

    public void start() {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
            }
        });
    }

    public void seekTo(final int time) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.seekTo(time);
            }
        });
    }

    /**
     * 多媒体资源缓冲更新回调
     *
     * @param mp
     * @param percent
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getFirstFloor() != null) {
                    JCVideoPlayerManager.getFirstFloor().setBufferProgress(percent);
                }
            }
        });
    }

    /**
     * Seekto定位回调
     * <p>
     * 注意：
     * seekTo()是定位方法，可以让播放器从指定的位置开始播放，
     * 需要注意的是该方法是个异步方法，也就是说该方法返回时并不意味着定位完成，
     * 尤其是播放的网络文件，真正定位完成时会触发OnSeekComplete.onSeekComplete()，
     * 如果需要是可以调用setOnSeekCompleteListener(OnSeekCompleteListener)设置监听器来处理的
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getFirstFloor() != null) {
                    JCVideoPlayerManager.getFirstFloor().onSeekComplete();
                }
            }
        });
    }

    public int getCurrentPosition() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return 0;
        }
        return 0;
    }

    public int getDuration(){
        try {
            if(mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return 0;
        }
        return 0;
    }

    public boolean isMediaplayerNull(){
        if (mediaPlayer != null)
            return true;
        else
            return false;
    }

    /**
     * 播放器错误回调
     *
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getFirstFloor() != null) {
                    JCVideoPlayerManager.getFirstFloor().onError(what, extra);
                }
            }
        });
        return true;
    }

    /**
     * 播放器警告信息回调
     *
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onInfo(MediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getFirstFloor() != null) {
                    JCVideoPlayerManager.getFirstFloor().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    /**
     * 加载到视频资源大小回调
     *
     * @param mp
     * @param width
     * @param height
     */
    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        currentVideoWidth = width;
        currentVideoHeight = height;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getFirstFloor() != null) {
                    JCVideoPlayerManager.getFirstFloor().onVideoSizeChanged();
                }
            }
        });
    }
}
