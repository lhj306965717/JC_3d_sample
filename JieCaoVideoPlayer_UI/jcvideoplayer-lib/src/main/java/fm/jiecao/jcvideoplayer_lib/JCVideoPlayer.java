package fm.jiecao.jcvideoplayer_lib;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tqit.stereorast.render.TQITVideoRenderer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static fm.jiecao.jcvideoplayer_lib.R.id.retry_text;

/**
 * Created by Nathen on 16/7/30.
 */
public abstract class JCVideoPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener, CompoundButton.OnCheckedChangeListener {

    public static final String TAG = "JieCaoVideoPlayer";

    // 这两个是 ActionBar与ToolBar标记
    public static boolean ACTION_BAR_EXIST = false;
    public static boolean TOOL_BAR_EXIST = true;

    // 屏幕状态（横屏、竖屏）
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    // 是否保存进度
    public static boolean SAVE_PROGRESS = true;

    public static boolean WIFI_TIP_DIALOG_SHOWED = false;

    public static final int THRESHOLD = 80;
    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    protected boolean isVideoRendingStart = false;  // 视频渲染是否开始

    public static final int SCREEN_LAYOUT_NORMAL = 0; // 正常状态
    public static final int SCREEN_WINDOW_FULLSCREEN = 2; // 全屏状态

    public static final int CURRENT_STATE_NORMAL = 0;  // 正常状态(无状态)
    public static final int CURRENT_STATE_PREPARING = 1;  // 准备状态
    public static final int CURRENT_STATE_PREPARING_CHANGING_URL = 2; // URL地址准备状态
    public static final int CURRENT_STATE_PLAYING = 3; // 播放状态
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 4; // 缓冲状态
    public static final int CURRENT_STATE_PAUSE = 5;  // 暂停状态
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6; // 播放完成状态
    public static final int CURRENT_STATE_ERROR = 7;  // 错误状态

    public static int BACKUP_PLAYING_BUFFERING_STATE = -1;

    public static long lastAutoFullscreenTime = 0;

    public static final String URL_KEY_DEFAULT = "URL_KEY_DEFAULT";

    /*******************  上面的很多状态都是使用静态是因为全屏窗口与普通窗口之间数据共享，达到无缝切换全屏   *******************/

    LinkedHashMap urlMap;
    int currentUrlMapIndex = 0;

    public int currentScreen = -1;  // 初始化 设置的 播放器显示模式
    public int currentState = -1;  // 播放器状态

    public boolean loop = false;
    public Map<String, String> headData;

    public Object[] objects = null;
    public int seekToInAdvance = 0;

    public int widthRatio = 0;
    public int heightRatio = 0;

    public ImageView startButton;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public FrameLayout surfaceViewContainer;
    public View bottomContainer;

    protected JCUserAction JC_USER_EVENT; // 注意：容易造成内存泄漏
    protected Timer UPDATE_PROGRESS_TIMER;

    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected Handler mHandler;
    protected ProgressTimerTask mProgressTimerTask;

    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected int mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected int mSeekTimePosition;
    public Switch mLrswap;
    protected ProgressBar bottomProgressBar;
    protected ProgressBar mLoading;
    protected View mFl_retry;
    protected TextView mRetryText;
    protected boolean isSeekTo; // 是否手动执行SeekTo
    protected boolean isFullscreen; //是否是本地播放全屏状态
    private boolean isReleaseVideo; // 是否释放播放器
    private boolean isBackstage; // 是否在后台运行
    private JCMediaManager mMediaManager;

    public JCVideoPlayer(Context context) {
        super(context);
        init(context);
    }

    public JCVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {

        View.inflate(context, getLayoutId(), this);

        // 真正用于播放显示的
        surfaceViewContainer = (FrameLayout) findViewById(R.id.fl_container);

        // 总的 底部UI 部分
        bottomContainer = findViewById(R.id.layout_bottom);

        // 播放按钮
        startButton = (ImageView) findViewById(R.id.iv_start);
        // 这个是时间显示
        currentTimeTextView = (TextView) findViewById(R.id.current);
        // 播放进度条
        progressBar = (SeekBar) findViewById(R.id.bottom_seek_progress);
        // 当前视频的总播放时间
        totalTimeTextView = (TextView) findViewById(R.id.total);
        // 模式切换
        mLrswap = (Switch) findViewById(R.id.lrswap);
        // 全屏显示按钮
        fullscreenButton = (ImageView) findViewById(R.id.fullscreen);

        // loading
        mLoading = (ProgressBar) findViewById(R.id.loading);

        // 播放错误或者重试
        mFl_retry = findViewById(R.id.fl_retry);
        mRetryText = (TextView) findViewById(R.id.retry_text);

        // 最底部的进度条
        bottomProgressBar = (ProgressBar) findViewById(R.id.bottom_progress);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        mRetryText.setOnClickListener(this);

        progressBar.setOnSeekBarChangeListener(this);

        surfaceViewContainer.setOnClickListener(this);
        surfaceViewContainer.setOnTouchListener(this);

        mLrswap.setChecked(false);
        mLrswap.setOnCheckedChangeListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        mHandler = new Handler(Looper.myLooper());
    }

    /**
     * 设置初始参数
     *
     * @param url     播放的url地址
     * @param screen  播放器在什么模式下显示(普通模式，全屏)
     * @param objects 视频的标题
     */
    public void setUp(String url, int screen, Object... objects) {
        LinkedHashMap map = new LinkedHashMap();
        map.put(URL_KEY_DEFAULT, url);
        setUp(map, 0, screen, objects);
    }

    public void setUp(LinkedHashMap urlMap, int defaultUrlMapIndex, int screen, Object... objects) {
        if (this.urlMap != null && !TextUtils.isEmpty(JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex))) {
            return;
        }

        this.urlMap = urlMap;
        this.currentUrlMapIndex = defaultUrlMapIndex;
        this.currentScreen = screen;
        this.objects = objects;
        this.headData = null;

        isVideoRendingStart = false; //设置渲染标记

        onStateNormal();

        setUIState(CURRENT_STATE_NORMAL);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_start) {

            if (TextUtils.isEmpty(JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex))) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }

            // 一般默认状态下就会调用走第一个判断
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {

                if (!JCUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    // 启动的时候判断当前网络是否是 wifi
                    // 这里最好用 onEvent 把回调传递出去
                    showWifiDialog(JCUserActionStandard.ON_CLICK_START_ICON);

                    return;
                }

                startVideo();

            } else if (currentState == CURRENT_STATE_PLAYING) {
                JCMediaManager.instance().pause();
                onStatePause();
                setUIState(CURRENT_STATE_PAUSE);
            } else if (currentState == CURRENT_STATE_PAUSE) {
                JCMediaManager.instance().start();
                onStatePlaying();
                setUIState(CURRENT_STATE_PLAYING);
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                startVideo();
            }
        } else if (i == R.id.fullscreen) {  // 全屏
            if (isFullscreen)
                return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                startProtrait();
            } else {
                startLandscape();
            }
        } else if (i == retry_text && currentState == CURRENT_STATE_ERROR) {
            startVideo();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.fl_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;

                    // 这三个控制下面面move中代码执行
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;

                    break;
                case MotionEvent.ACTION_MOVE:

                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);

                    // 不是全屏模式下，上下滑动没有事件响应
                    if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {

                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {

                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {

                                cancelProgressTimer();

                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (currentState != CURRENT_STATE_ERROR) {
                                        mChangePosition = true;
                                        mGestureDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (mDownX < mScreenWidth * 0.5f) {//左侧改变亮度
                                        mChangeBrightness = true;
                                        WindowManager.LayoutParams lp = JCUtils.getAppCompActivity(getContext()).getWindow().getAttributes();
                                        if (lp.screenBrightness < 0) {
                                            try {
                                                mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            mGestureDownBrightness = lp.screenBrightness * 255;
                                        }
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
                                }
                            }
                        }

                        if (mChangePosition) {
                            int totalTimeDuration = getDuration();
                            mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                            if (mSeekTimePosition > totalTimeDuration)
                                mSeekTimePosition = totalTimeDuration;
                            String seekTime = JCUtils.stringForTime(mSeekTimePosition);
                            String totalTime = JCUtils.stringForTime(totalTimeDuration);

                            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                        }
                        if (mChangeVolume) {
                            deltaY = -deltaY;
                            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                            //dialog中显示百分比
                            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
                            showVolumeDialog(-deltaY, volumePercent);
                        }
                        if (mChangeBrightness) {
                            deltaY = -deltaY;
                            int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                            WindowManager.LayoutParams params = JCUtils.getAppCompActivity(getContext()).getWindow().getAttributes();
                            if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                                params.screenBrightness = 1;
                            } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                                params.screenBrightness = 0.01f;
                            } else {
                                params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                            }
                            JCUtils.getAppCompActivity(getContext()).getWindow().setAttributes(params);
                            //dialog中显示百分比
                            int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
                            showBrightnessDialog(brightnessPercent);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:

                    mTouchingProgressBar = false;

                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();

                    if (mChangePosition) {
                        mMediaManager.seekTo(mSeekTimePosition);
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        progressBar.setProgress(progress);
                        bottomProgressBar.setProgress(progress);
                    }

                    // 这行代码有待确定，正常是不需要的
//                    startProgressTimer();

                    break;
            }
        }
        return false;
    }

    /**
     * 开始播放
     */
    public void startVideo() {

        Log.e("TAG", "+++++++ startVideo +++++");

        mMediaManager = JCMediaManager.instance();

        JCVideoPlayerManager.completeAll(); //释放当前对象

        initSurfaceView();

        addSurfaceView();

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        // 设置屏幕保持不暗
        JCUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        JCMediaManager.instance().current_playing_url = JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex);
        JCMediaManager.instance().current_pling_loop = loop;
        JCMediaManager.instance().map_header_data = headData;

        onStatePreparing(); // 设置状态参数
        setUIState(CURRENT_STATE_PREPARING); // 更新UI 状态

        JCVideoPlayerManager.setFirstFloor(this); // 绑定对象
    }

    /**
     * 非常重要方法，设置各种状态下应该显示的UI
     */
    public void setUIState(int state) {
        setUIState(state, 0, 0);
    }

    /**
     * 初始化状态
     */
    public void onStateNormal() {

        Log.e("TAG", "+++++  onStateNormal  +++++");

        isSeekTo = false;

        currentState = CURRENT_STATE_NORMAL; // 设置初始状态

        cancelProgressTimer(); // 取消设置 Progress 进度的 Timer

        // 释放
        JCMediaManager.instance().releaseMediaPlayer();
    }

    public void onStatePreparing() {
        currentState = CURRENT_STATE_PREPARING;
        resetProgressAndTime();
        isReleaseVideo = false;
    }

    public void onStatePreparingChangingUrl(int urlMapIndex, int seekToInAdvance) {

        Log.e("TAG", "++++++++  重新设置URL地址播放  ++++++");

        currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.currentUrlMapIndex = urlMapIndex;
        this.seekToInAdvance = seekToInAdvance;
        JCMediaManager.instance().current_playing_url = JCUtils.getCurrentUrlFromMap(urlMap, this.currentUrlMapIndex);
        JCMediaManager.instance().current_pling_loop = this.loop;
        JCMediaManager.instance().map_header_data = this.headData;
        JCMediaManager.instance().prepare();
    }

    public void onVideoRendingStart() {

        Log.e(TAG, "++++++   onVideoRendingStart----->状态：" + currentState);

        isVideoRendingStart = true; // 设置标记

        // 状态判断有点问题，可有可无，这个状态维持一定不能错
        if (currentState != CURRENT_STATE_PREPARING && currentState != CURRENT_STATE_PREPARING_CHANGING_URL && currentState != CURRENT_STATE_PLAYING_BUFFERING_START)
            return;

        // 如果保存有上一次的播放记录，则继续续播
        if (seekToInAdvance != 0) {
            mMediaManager.seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            int position = JCUtils.getSavedProgress(getContext(), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex));
            if (position != 0) {
                mMediaManager.seekTo(position);
            }
        }

        onStatePlaying(); // 更新为播放状态

        setUIState(CURRENT_STATE_PLAYING); // 设置UI状态
    }

    public void onStatePlaying() {
        currentState = CURRENT_STATE_PLAYING;
        startProgressTimer();
    }

    public void onStatePause() {
        currentState = CURRENT_STATE_PAUSE;
        startProgressTimer();
    }

    public void onStatePlaybackBufferingStart() {
        currentState = CURRENT_STATE_PLAYING_BUFFERING_START;
        startProgressTimer();
    }

    public void onStateError() {
        currentState = CURRENT_STATE_ERROR;
        cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        currentState = CURRENT_STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
        bottomProgressBar.setProgress(100);
    }

    public void onInfo(int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START: // MediaPlayer暂停播放等待缓冲更多数据
                Log.e(TAG, "暂停播放等待缓冲更多数据----->状态：" + currentState);
                if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START)
                    return;
                BACKUP_PLAYING_BUFFERING_STATE = currentState; // 赋值状态
                onStatePlaybackBufferingStart();
                setUIState(CURRENT_STATE_PLAYING_BUFFERING_START);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END: // MediaPlayer在缓冲完后继续播放
                Log.e(TAG, "缓冲完成继续播放---->状态：" + currentState);
                if (BACKUP_PLAYING_BUFFERING_STATE != -1) {
                    if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                        if (!isSeekTo) {
                            onStatePlaying();
                            Log.e("TAG", " BACKUP_PLAYING_BUFFERING_STATE: " + BACKUP_PLAYING_BUFFERING_STATE);
                            setUIState(BACKUP_PLAYING_BUFFERING_STATE);
                        }
                    }
                    BACKUP_PLAYING_BUFFERING_STATE = -1;
                }
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: //刚刚出了第一帧视频
                onVideoRendingStart();
                Log.e(TAG, "加载出了第一帧");
                break;
            default:
                Log.e(TAG, "警告信息：-->default: " + what + " extra: " + extra);
                break;
        }
    }

    public void onError(int what, int extra) {
        Log.e(TAG, "播放器错误信息：" + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && what != -38 && extra != -38) {
            onStateError();
            setUIState(CURRENT_STATE_ERROR);
            JCMediaManager.instance().releaseMediaPlayer();
        }
    }

    /**
     * SeekBar 拖动事件
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        cancelProgressTimer();

        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        startProgressTimer();

        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }

        if (currentState != CURRENT_STATE_PLAYING && currentState != CURRENT_STATE_PAUSE)
            return;

        int time = seekBar.getProgress() * getDuration() / 100;

        isSeekTo = true;

        JCMediaManager.instance().seekTo(time);

        // 避免卡住不显示Loading
//        onStatePlaybackBufferingStart(); // 这里状态值不能在这里赋值，还是需要info中自动回调
//        setUIState(CURRENT_STATE_PLAYING_BUFFERING_START);// 状态值可以不用，但是 UI一定要有
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // SeekBar 拖动 回调
    }

    /**
     * 如有需要就在子类中重写
     */
    public void onSeekComplete() {
        isSeekTo = false;

        Log.e("TAG", "++++++   onSeekComplete   ++++++");

        onStatePlaying();
        setUIState(CURRENT_STATE_PLAYING);
    }

    /**
     * 播放完成回调
     */
    public void onAutoCompletion() {

        Log.e("TAG", "++++++   onAutoCompletion   +++++++");

        cancelProgressTimer();

        onStateAutoComplete(); // 更新播放完成状态

        setUIState(CURRENT_STATE_AUTO_COMPLETE);

        dismissVolumeDialog();
        dismissProgressDialog();
        dismissBrightnessDialog();

        // 让进度从零开始
        JCUtils.saveProgress(getContext(), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex), 0);

        //加上这句，避免循环播放video的时候，内存不断飙升。
        Runtime.getRuntime().gc();
    }

    /**
     * 将进度单独抽取出来
     */
    public void saveProgress() {
        // 如果是播放状态或者暂停状态被执行了清理后者释放，则保存播放进度
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            int position = getCurrentPositionWhenPlaying();
            JCUtils.saveProgress(getContext(), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex), position);
        }
    }

    /**
     * 释放播放器
     * 只在 JCVideoPlayerManager 类中调用
     */
    public void onCompletion() {

        Log.e("TAG", "执行清理操作--->onCompletion");

        onStateNormal();
        setUIState(CURRENT_STATE_NORMAL);

        // 清理缓存变量
        // 非常重要，因为 JCMediaManager.sSurfaceView 是被静态引用，会导致无法释放
        // 最要是初始化时，重新添加了，所以这里需要先删除。否则会报错
        surfaceViewContainer.removeView(mMediaManager.sSurfaceView);

        JCMediaManager.instance().currentVideoWidth = 0;
        JCMediaManager.instance().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);

        // 清除播放中屏幕不熄灭
        JCUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 强制设置屏幕旋转方向为垂直
        JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);

        JCMediaManager.instance().sSurfaceView = null;
        JCMediaManager.instance().sSurface = null;
        isVideoRendingStart = false;
    }

    /**
     * 释放播放器
     */
    public void releaseAllVideos() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            isReleaseVideo = true;
            JCVideoPlayerManager.completeAll();
        }
    }

    public void initSurfaceView() {
        removeSurfaceView();
        mMediaManager.sSurfaceView = new MySurfaceView(getContext());
        mMediaManager.sSurfaceView.setJCMediaManager(mMediaManager);
        mMediaManager.sSurfaceView.setJCVideoPlayer(this);
    }

    public void addSurfaceView() {
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);

        surfaceViewContainer.addView(mMediaManager.sSurfaceView, layoutParams);
    }

    public void removeSurfaceView() {
        mMediaManager.sSurface = null;
        if (mMediaManager.sSurfaceView != null && mMediaManager.sSurfaceView.getParent() != null) {
            ((ViewGroup) mMediaManager.sSurfaceView.getParent()).removeView(mMediaManager.sSurfaceView);
        }
    }

    public void onVideoSizeChanged() {
        if (mMediaManager.sSurfaceView != null) {
            mMediaManager.sSurfaceView.setVideoSize(mMediaManager.getVideoSize());
        }
    }

    /**
     * switch 视频播放模式切换监听
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_LR);
        } else {
            mMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_2D);
        }
    }

    public void setProgressAndText(int progress, int position, int duration) {
        if (!mTouchingProgressBar) {
            if (progress != 0) progressBar.setProgress(progress);
        }
        if (position != 0) currentTimeTextView.setText(JCUtils.stringForTime(position));

        totalTimeTextView.setText(JCUtils.stringForTime(duration));

        if (progress != 0) bottomProgressBar.setProgress(progress);
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) {
            progressBar.setSecondaryProgress(bufferProgress);
            bottomProgressBar.setSecondaryProgress(bufferProgress);
        }
    }

    /**
     * 启动播放进度条显示
     */
    public void startProgressTimer() {

        cancelProgressTimer();

        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();

        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300); // 表示每0.3秒执行一次run
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (isReleaseVideo)
                            return;

                        int position = getCurrentPositionWhenPlaying();
                        int duration = getDuration();
                        int progress = position * 100 / (duration == 0 ? 1 : duration);
                        setProgressAndText(progress, position, duration);
                    }
                });
            }
        }
    }

    /**
     * 重置进度条以及播放时间
     */
    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(JCUtils.stringForTime(0));
        totalTimeTextView.setText(JCUtils.stringForTime(0));
        bottomProgressBar.setProgress(0);
        bottomProgressBar.setSecondaryProgress(0);
    }

    /**
     * 返回当前的播放进度
     *
     * @return
     */
    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            position = mMediaManager.getCurrentPosition();
        }
        return position;
    }

    /**
     * 返回播放持续时间
     *
     * @return
     */
    public int getDuration() {
        int duration = 0;
        try {
            duration = mMediaManager.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    /**
     * 启动窗口全屏
     * 暂时不删除代码
     */
    public void startLandscape() {

        // 隐藏状态栏
        hideSupportActionBar(getContext());
        // 设置横屏
        JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        /********    下面是彻底的 沉浸式 包括虚拟按键也会被隐藏      *******/

//        int curApiVersion = android.os.Build.VERSION.SDK_INT;
//        int flags;
//        if(curApiVersion >= Build.VERSION_CODES.KITKAT){
//            flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
//        }else{
//            flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//        }

//        final View decorView = activity.getWindow().getDecorView();

//        decorView.setSystemUiVisibility(flags);

        // 监听navigate bar
//        decorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener(){
//            @Override
//            public void onSystemUiVisibilityChange(int visibility) {
//                if(visibility == View.VISIBLE) {
//                    int curApiVersion = android.os.Build.VERSION.SDK_INT;
//                    int flags;
//                    if (curApiVersion >= Build.VERSION_CODES.KITKAT) {
//                        flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
//                    } else {
//                        flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//                    }
//                    decorView.setSystemUiVisibility(flags);
//                }
//            }
//        });

//        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
//        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//        activity.getWindow().setAttributes(params);
//        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);


        // 注意：还得重新计算 GLSurfaceView的宽高
//        FrameLayout.LayoutParams params = (LayoutParams) JCMediaManager.sSurfaceView.getLayoutParams();
//        params.width = LayoutParams.MATCH_PARENT;
//        params.height = LayoutParams.MATCH_PARENT;
//        JCMediaManager.sSurfaceView.setLayoutParams(params);

        // 设置横屏状态
        this.currentScreen = JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN;


        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        // 由于设置的 GLSurfaceView 本身就是 MATCH_PARENT，所以这里 设置了 setLayoutParams 会重新计算整个控件
        this.setLayoutParams(layoutParams);

        CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
    }

    /**
     * 设置竖屏
     */
    public void startProtrait() {

        // 设置横屏状态
        this.currentScreen = JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL;

        showSupportActionBar(getContext());

        // 设置横屏
        JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.getLayoutParams();
        layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
        layoutParams.height = 0;
        layoutParams.weight = 1.4f;

        // 由于设置的 GLSurfaceView 本身就是 MATCH_PARENT，所以这里 设置了 setLayoutParams 会重新计算整个控件
        this.setLayoutParams(layoutParams);
    }

    /**
     * 注意：是静态方法
     * onBackPressed 后退逻辑
     */
    public static boolean backPress() {

        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;

        // 这里判断有几层显示器
        if (JCVideoPlayerManager.getFirstFloor() != null && JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//以前我总想把这两个判断写到一起，这分明是两个独立是逻辑
            // 如果是 全屏状态 状态
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            //直接退出全屏和小窗
            JCVideoPlayerManager.getFirstFloor().currentState = CURRENT_STATE_NORMAL;

            JCVideoPlayerManager.setFirstFloor(null);

            return true;
        }
        return false;
    }

    /**
     * 显示 ActionBar或者ToolBar
     *
     * @param context
     */
    public void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = JCUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            JCUtils.getAppCompActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 隐藏 ActionBar或者ToolBar
     *
     * @param context
     */
    public void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = JCUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            JCUtils.getAppCompActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 添加重力感应
     */
    //重力感应的时候调用的函数，
    public void autoFullscreen(float x) {
        if (currentState == CURRENT_STATE_PLAYING && currentScreen != SCREEN_WINDOW_FULLSCREEN) {
            if (x > 0) {
                JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }

            startLandscape();
        }
    }

    /**
     * 退出重力感应
     */
    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && currentState == CURRENT_STATE_PLAYING
                && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    /**
     * 重力感应监听
     */
    public static class JCAutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
            if (((x > -15 && x < -10) || (x < 15 && x > 10)) && Math.abs(y) < 1.5) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                    if (JCVideoPlayerManager.getFirstFloor() != null) {
                        JCVideoPlayerManager.getFirstFloor().autoFullscreen(x);
                    }
                    lastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /**
     * 清除掉本地保存的播放进度
     *
     * @param context
     * @param url
     */
    public static void clearSavedProgress(Context context, String url) {
        JCUtils.clearSavedProgress(context, url);
    }

    /**
     * 绑定各种事件动作回调对象
     *
     * @param jcUserEvent
     */
    public void setJcUserAction(JCUserAction jcUserEvent) {
        JC_USER_EVENT = jcUserEvent;
    }

    /**
     * 事件传递
     *
     * @param type
     */
    public void onEvent(int type) {
        if (JC_USER_EVENT != null && urlMap != null) {
            JC_USER_EVENT.onEvent(type, currentScreen, objects);
        }
    }

    public AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
//                    releaseAllVideos();
                    Log.e("TAG", "长久的失去焦点");
                    try {
                        if (mMediaManager.isMediaplayerNull() && (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PLAYING_BUFFERING_START)) {
                            mMediaManager.pause();
                            onStatePause();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        if (mMediaManager.isMediaplayerNull() && (currentScreen == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PLAYING_BUFFERING_START)) {
                            mMediaManager.pause();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    Log.e("TAG", "短暂的失去焦点");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    public abstract int getLayoutId();

    public abstract void setUIState(int state, int urlMapIndex, int seekToInAdvance);

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    public void showWifiDialog(int event) {
        onEvent(event); // 将回调转到外面
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime,
                                   int seekTimePosition,
                                   String totalTime,
                                   int totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }
}
