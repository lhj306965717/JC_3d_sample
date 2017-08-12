package fm.jiecao.jcvideoplayer_lib;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tqit.stereorast.render.TQITVideoRenderer;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Nathen on 16/7/30.
 */
public abstract class JCVideoPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener, CompoundButton.OnCheckedChangeListener {

    public static final String TAG = "JieCaoVideoPlayer";

    protected boolean isVideoRendingStart = false;  // 视频渲染是否开始

    // 这两个是 ActionBar与ToolBar标记
    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;

    // 屏幕状态（横屏、竖屏）
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    // 是否保存进度
    public static boolean SAVE_PROGRESS = true;

    public static boolean WIFI_TIP_DIALOG_SHOWED = false;

    public static final int FULLSCREEN_ID = 33797;
    public static final int TINY_ID = 33798;
    public static final int THRESHOLD = 80;
    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    public static final int SCREEN_LAYOUT_NORMAL = 0; // 正常状态
    public static final int SCREEN_WINDOW_FULLSCREEN = 2; // 全屏状态

    public static final int CURRENT_STATE_NORMAL = 0;  // 正常状态(无状态)
    public static final int CURRENT_STATE_PREPARING = 1;  // 准备状态
    public static final int CURRENT_STATE_PREPARING_CHANGING_URL = 2; // URL地址准备状态
    public static final int CURRENT_STATE_PLAYING = 3; // 播放状态
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 4; // 开始缓冲状态
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
    public int currentState = -1;
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
    public ViewGroup surfaceViewContainer;
    public ViewGroup topContainer, bottomContainer;

    protected static JCUserAction JC_USER_EVENT; // 注意：容易造成内存泄漏
    protected static Timer UPDATE_PROGRESS_TIMER;

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
    private Switch mLrswap;

    public JCVideoPlayer(Context context) {
        super(context);
        init(context);
    }

    public JCVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public abstract int getLayoutId();

    public void init(Context context) {

        View.inflate(context, getLayoutId(), this);

        // 真正用于播放显示的
        surfaceViewContainer = (ViewGroup) findViewById(R.id.fl_container);

        // 总的 底部UI 部分
        bottomContainer = (ViewGroup) findViewById(R.id.layout_bottom);
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

        // 总的顶部UI 部分
        topContainer = (ViewGroup) findViewById(R.id.layout_top);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        surfaceViewContainer.setOnClickListener(this);
        surfaceViewContainer.setOnTouchListener(this);

        mLrswap.setChecked(false);
        mLrswap.setOnCheckedChangeListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();

        try {
            if (isCurrentJcvd()) {
                NORMAL_ORIENTATION = ((AppCompatActivity) context).getRequestedOrientation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置初始参数
     *
     * @param url     播放的url地址
     * @param screen  播放器在什么模式下显示(普通模式，窗口模式，列表模式)
     * @param objects 视频的标题
     */
    public void setUp(String url, int screen, Object... objects) {
        LinkedHashMap map = new LinkedHashMap();
        map.put(URL_KEY_DEFAULT, url);
        setUp(map, 0, screen, objects);
    }

    /**
     * @param urlMap
     * @param defaultUrlMapIndex 在HashMap中的位置
     * @param screen
     * @param objects
     */
    public void setUp(LinkedHashMap urlMap, int defaultUrlMapIndex, int screen, Object... objects) {
        if (this.urlMap != null &&
                !TextUtils.isEmpty(JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex)) &&
                TextUtils.equals(JCUtils.getCurrentUrlFromMap(this.urlMap, currentUrlMapIndex), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex))) {
            return;
        }

        this.urlMap = urlMap;
        this.currentUrlMapIndex = defaultUrlMapIndex;
        this.currentScreen = screen;
        this.objects = objects;
        this.headData = null;

        isVideoRendingStart = false; //设置渲染标记

        onStateNormal();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_start) {
            Log.i(TAG, "onClick start [" + this.hashCode() + "] ");
            if (TextUtils.isEmpty(JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex))) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {
                if (!JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex).startsWith("file") &&
                        !JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex).startsWith("/") &&
                        !JCUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {

                    // 启动的时候判断当前网络是否是 wifi

                    showWifiDialog(JCUserActionStandard.ON_CLICK_START_ICON);

                    return;
                }

                startVideo();

                onEvent(currentState != CURRENT_STATE_ERROR ? JCUserAction.ON_CLICK_START_ICON : JCUserAction.ON_CLICK_START_ERROR);

            } else if (currentState == CURRENT_STATE_PLAYING) {
                Log.i(TAG, "pauseVideo [" + this.hashCode() + "] ");
                onEvent(JCUserAction.ON_CLICK_PAUSE);
                JCMediaManager.instance().mediaPlayer.pause();
                onStatePause();
            } else if (currentState == CURRENT_STATE_PAUSE) {
                onEvent(JCUserAction.ON_CLICK_RESUME);
                JCMediaManager.instance().mediaPlayer.start();
                onStatePlaying();
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onEvent(JCUserAction.ON_CLICK_START_AUTO_COMPLETE);
                startVideo();
            }
        } else if (i == R.id.fullscreen) {

            Log.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");

            if (currentState == CURRENT_STATE_AUTO_COMPLETE)
                return;

            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {

                Log.i(TAG, "toFullscreenActivity [" + this.hashCode() + "] ");

                onEvent(JCUserAction.ON_ENTER_FULLSCREEN);

                startLandscape();
                //startWindowFullscreen();
            }
        } else if (i == R.id.fl_container && currentState == CURRENT_STATE_ERROR) {
            Log.i(TAG, "onClick surfaceContainer State=Error [" + this.hashCode() + "] ");
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
                    Log.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;

                    // 这三个控制下面面move中代码执行
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;

                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (currentScreen == SCREEN_WINDOW_FULLSCREEN) { // 不是全屏模式下，上下滑动没有事件响应
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
                                                Log.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            mGestureDownBrightness = lp.screenBrightness * 255;
                                            Log.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                                        }
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
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
//                        mDownY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (mChangePosition) {
                        onEvent(JCUserAction.ON_TOUCH_SCREEN_SEEK_POSITION);
                        JCMediaManager.instance().mediaPlayer.seekTo(mSeekTimePosition);
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        progressBar.setProgress(progress);
                    }
                    if (mChangeVolume) {
                        onEvent(JCUserAction.ON_TOUCH_SCREEN_SEEK_VOLUME);
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    /**
     * 开始播放
     */
    public void startVideo() {
        Log.i(TAG, "startVideo [" + this.hashCode() + "] ");

        JCVideoPlayerManager.completeAll(); //释放当前对象

        initSurfaceView();

        addSurfaceView();

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        // 设置屏幕保持不暗
        JCUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        JCMediaManager.CURRENT_PLAYING_URL = JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex);
        JCMediaManager.CURRENT_PLING_LOOP = loop;
        JCMediaManager.MAP_HEADER_DATA = headData;

        onStatePreparing(); // 设置状态参数

        JCVideoPlayerManager.setFirstFloor(this); // 绑定对象
    }

    /**
     * 视频开始渲染
     */
    public void onVideoRendingStart() {
        Log.i(TAG, "onVideoRendingStart " + " [" + this.hashCode() + "] ");

        isVideoRendingStart = true; // 设置标记

        if (currentState != CURRENT_STATE_PREPARING && currentState != CURRENT_STATE_PREPARING_CHANGING_URL && currentState != CURRENT_STATE_PLAYING_BUFFERING_START)
            return;

        if (seekToInAdvance != 0) {
            JCMediaManager.instance().mediaPlayer.seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            int position = JCUtils.getSavedProgress(getContext(), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex));
            if (position != 0) {
                JCMediaManager.instance().mediaPlayer.seekTo(position);
            }
        }

// TAG: 发现这里代码重复了， 在onStatePlaying中调用了 startProgressTimer
//        startProgressTimer();
        onStatePlaying();
    }

    public void setState(int state) {
        setState(state, 0, 0);
    }

    public void setState(int state, int urlMapIndex, int seekToInAdvance) {
        switch (state) {
            case CURRENT_STATE_NORMAL:
                onStateNormal();
                break;
            case CURRENT_STATE_PREPARING:
                onStatePreparing();
                break;
            case CURRENT_STATE_PREPARING_CHANGING_URL:
                onStatePreparingChangingUrl(urlMapIndex, seekToInAdvance);
                break;
            case CURRENT_STATE_PLAYING:
                onStatePlaying();
                break;
            case CURRENT_STATE_PAUSE:
                onStatePause();
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                onStatePlaybackBufferingStart();
                break;
            case CURRENT_STATE_ERROR:
                onStateError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                onStateAutoComplete();
                break;
        }
    }

    /**
     * 初始化状态
     */
    public void onStateNormal() {
        Log.i(TAG, "onStateNormal " + " [" + this.hashCode() + "] ");

        currentState = CURRENT_STATE_NORMAL; // 设置初始状态

        cancelProgressTimer(); // 取消设置 Progress 进度的 Timer

        // 判断是否是当前对象
        if (isCurrentJcvd()) {//这个if是无法取代的，否则进入全屏的时候会releaseMediaPlayer
            Log.e("TAG", " isCurrentJcvd: " + isCurrentJcvd());
            JCMediaManager.instance().releaseMediaPlayer();
        }
    }

    public void onStatePreparing() {
        Log.i(TAG, "onStatePreparing " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PREPARING;
        resetProgressAndTime();
    }

    public void onStatePreparingChangingUrl(int urlMapIndex, int seekToInAdvance) {
        currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.currentUrlMapIndex = urlMapIndex;
        this.seekToInAdvance = seekToInAdvance;
        JCMediaManager.CURRENT_PLAYING_URL = JCUtils.getCurrentUrlFromMap(urlMap, this.currentUrlMapIndex);
        JCMediaManager.CURRENT_PLING_LOOP = this.loop;
        JCMediaManager.MAP_HEADER_DATA = this.headData;
        JCMediaManager.instance().prepare();
    }

    public void onStatePlaying() {
        Log.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PLAYING;
        startProgressTimer();
    }

    public void onStatePause() {
        Log.i(TAG, "onStatePause " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PAUSE;
        startProgressTimer();
    }

    public void onStatePlaybackBufferingStart() {
        Log.i(TAG, "onStatePlaybackBufferingStart " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PLAYING_BUFFERING_START;
        startProgressTimer();
    }

    public void onStateError() {
        Log.i(TAG, "onStateError " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_ERROR;
        cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        Log.i(TAG, "onStateAutoComplete " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START: // MediaPlayer暂停播放等待缓冲更多数据
                if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) return;
                BACKUP_PLAYING_BUFFERING_STATE = currentState;
                onStatePlaybackBufferingStart();
                Log.e(TAG, "暂停播放等待缓冲更多数据....");
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END: // MediaPlayer在缓冲完后继续播放
                if (BACKUP_PLAYING_BUFFERING_STATE != -1) {
                    if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                        setState(BACKUP_PLAYING_BUFFERING_STATE);
                    }
                    BACKUP_PLAYING_BUFFERING_STATE = -1;
                }
                Log.e(TAG, "缓冲完后继续播放....");
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: //刚刚出了第一帧视频
                onVideoRendingStart();
                Log.e(TAG, "加载出了第一帧");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN: //播放错误，未知错误
                Log.e(TAG, "播放错误，未知错误....");
                Toast.makeText(getContext(), "播放错误，未知错误", Toast.LENGTH_LONG).show();
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK: // 播放错误（一般视频播放比较慢或视频本身有问题会引发）
                Log.e(TAG, "播放错误....");
                Toast.makeText(getContext(), "播放错误", Toast.LENGTH_LONG).show();
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING: //视频过于复杂，无法解码：不能快速解码帧。此时可能只能正常播放音频
                Log.e(TAG, "视频过于复杂，无法解码....");
                Toast.makeText(getContext(), "视频过于复杂，无法解码！", Toast.LENGTH_LONG).show();
                break;
            default:
                Log.e(TAG, "播放器警告信息：-->default: " + what + " extra: " + extra);
                break;
        }
    }

    public void onError(int what, int extra) {
        Log.e(TAG, "播放器错误信息：" + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && what != -38 && extra != -38) {
            onStateError();
            if (isCurrentJcvd()) {
                JCMediaManager.instance().releaseMediaPlayer();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN /*|| currentScreen == SCREEN_WINDOW_TINY*/) {
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

    /**
     * 播放完成回调
     */
    public void onAutoCompletion() {
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        //加上这句，避免循环播放video的时候，内存不断飙升。
        Runtime.getRuntime().gc();
        onEvent(JCUserAction.ON_AUTO_COMPLETE);
        dismissVolumeDialog();
        dismissProgressDialog();
        dismissBrightnessDialog();
        cancelProgressTimer();
        onStateAutoComplete();

        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            backPress();
        }
        JCUtils.saveProgress(getContext(), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex), 0);
    }

    public void onCompletion() {
        Log.i(TAG, "onCompletion " + " [" + this.hashCode() + "] ");
        //save position
        // 如果是播放状态或者暂停状态被执行了清理后者释放，则保存播放进度
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            int position = getCurrentPositionWhenPlaying();
            JCUtils.saveProgress(getContext(), JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex), position);
        }

        cancelProgressTimer();

        onStateNormal();

        // 清理缓存变量
        surfaceViewContainer.removeView(JCMediaManager.sSurfaceView);

        JCMediaManager.instance().currentVideoWidth = 0;
        JCMediaManager.instance().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);

        // 用于防止屏幕熄灭
        JCUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        clearFullscreenLayout();

        // 强制设置屏幕旋转方向为垂直
        JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);

        JCMediaManager.sSurfaceView = null;
        JCMediaManager.sSurface = null;
        isVideoRendingStart = false; // 是否开始渲染标记
    }

    public void release() {
        if (JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex).equals(JCMediaManager.CURRENT_PLAYING_URL) &&
                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            //在非全屏的情况下只能backPress()
            if (JCVideoPlayerManager.getSecondFloor() != null &&
                    JCVideoPlayerManager.getSecondFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//点击全屏
            } else if (JCVideoPlayerManager.getSecondFloor() == null && JCVideoPlayerManager.getFirstFloor() != null &&
                    JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//直接全屏
            } else {
                Log.i(TAG, "release [" + this.hashCode() + "]");
                releaseAllVideos();
            }
        }
    }

    public static void releaseAllVideos() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            Log.i(TAG, "releaseAllVideos");
            JCVideoPlayerManager.completeAll();
            JCMediaManager.instance().releaseMediaPlayer();
        }
    }

    public void initSurfaceView() {
        removeSurfaceView();
        JCMediaManager.sSurfaceView = new MySurfaceView(getContext());
        JCMediaManager.sSurfaceView.setJCMediaManager(JCMediaManager.instance());
    }

    public void addSurfaceView() {
        Log.i(TAG, "addSurfaceView [" + this.hashCode() + "] ");
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);

        surfaceViewContainer.addView(JCMediaManager.sSurfaceView, layoutParams);
    }

    public void removeSurfaceView() {
        JCMediaManager.sSurface = null;
        if (JCMediaManager.sSurfaceView != null && JCMediaManager.sSurfaceView.getParent() != null) {
            ((ViewGroup) JCMediaManager.sSurfaceView.getParent()).removeView(JCMediaManager.sSurfaceView);
        }
    }

    /**
     * 清理全屏布局
     */
    public void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(FULLSCREEN_ID);
        View oldT = vp.findViewById(TINY_ID);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext());
    }

    public void clearFloatScreen() {
        JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        showSupportActionBar(getContext());
        JCVideoPlayer currJcvd = JCVideoPlayerManager.getCurrentJcvd();
        currJcvd.surfaceViewContainer.removeView(JCMediaManager.sSurfaceView);
        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        vp.removeView(currJcvd);
        JCVideoPlayerManager.setSecondFloor(null);
    }

    public void onVideoSizeChanged() {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (JCMediaManager.sSurfaceView != null) {
            JCMediaManager.sSurfaceView.setVideoSize(JCMediaManager.instance().getVideoSize());
        }
    }

    /**
     * 启动播放进度条显示
     */
    public void startProgressTimer() {
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    /**
     * 视频播放模式切换监听
     *
     * @param buttonView
     * @param isChecked
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            JCMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_LR);
        } else {
            JCMediaManager.sSurfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_2D);
        }
    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
//                Log.v(TAG, "onProgressUpdate " + position + "/" + duration + " [" + this.hashCode() + "] ");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int position = getCurrentPositionWhenPlaying();
                        int duration = getDuration();
                        int progress = position * 100 / (duration == 0 ? 1 : duration);
                        setProgressAndText(progress, position, duration);
                    }
                });
            }
        }
    }

    public void setProgressAndText(int progress, int position, int duration) {
        if (!mTouchingProgressBar) {
            if (progress != 0) progressBar.setProgress(progress);
        }
        if (position != 0) currentTimeTextView.setText(JCUtils.stringForTime(position));
        totalTimeTextView.setText(JCUtils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    /**
     * 重置进度条以及播放时间
     */
    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(JCUtils.stringForTime(0));
        totalTimeTextView.setText(JCUtils.stringForTime(0));
    }

    /**
     * 返回seekbar的进度
     *
     * @return
     */
    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (JCMediaManager.instance().mediaPlayer == null)
            return position;//这行代码不应该在这，如果代码和逻辑万无一失的话，心头之恨呐
        if (currentState == CURRENT_STATE_PLAYING ||
                currentState == CURRENT_STATE_PAUSE ||
                currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            try {
                position = JCMediaManager.instance().mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
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
        if (JCMediaManager.instance().mediaPlayer == null) return duration;
        try {
            duration = JCMediaManager.instance().mediaPlayer.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        onEvent(JCUserAction.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING &&
                currentState != CURRENT_STATE_PAUSE) return;
        int time = seekBar.getProgress() * getDuration() / 100;
        JCMediaManager.instance().mediaPlayer.seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // SeekBar 拖动 回调
    }


    public void startLandscape() {
        //设置横屏
        ((Activity) getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 注意：还得重新计算 GLSurfaceView的宽高
//        FrameLayout.LayoutParams params = (LayoutParams) JCMediaManager.sSurfaceView.getLayoutParams();
//        params.width = LayoutParams.MATCH_PARENT;
//        params.height = LayoutParams.MATCH_PARENT;
//
//        JCMediaManager.sSurfaceView.setLayoutParams(params);

        this.currentScreen = JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN;


        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        // 由于设置的 GLSurfaceView 本身就是 MATCH_PARENT，所以这里 设置了 setLayoutParams 会重新计算整个控件
        this.setLayoutParams(layoutParams);
    }


    /**
     * 启动窗口全屏
     */
    public void startWindowFullscreen() {
        Log.i(TAG, "startWindowFullscreen " + " [" + this.hashCode() + "] ");

        hideSupportActionBar(getContext());

        JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        // 重点代码：这里查找根View
        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT); //.getWindow().getDecorView();
        // 如果是全屏显示过，那么肯定有这个id FULLSCREEN_ID
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old); // 就把这个id 删除
        }

        surfaceViewContainer.removeView(JCMediaManager.sSurfaceView);

        try {

            // 反射创建对象
            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());

            // 重点代码：为新创建的这个 Framlayout JCVideoPlayer 对象设置一个id
            jcVideoPlayer.setId(FULLSCREEN_ID);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // 重新创建一个 FrameLayout 添加到根View中
            vp.addView(jcVideoPlayer, lp);

            // 设置参数
            jcVideoPlayer.setUp(urlMap, currentUrlMapIndex, JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, objects);

            // 设置状态
            jcVideoPlayer.setState(currentState);

            // 重新添加 GLSurfaceView
            jcVideoPlayer.addSurfaceView();

            // 添加管理
            JCVideoPlayerManager.setSecondFloor(jcVideoPlayer);

//            final Animation ra = AnimationUtils.loadAnimation(getContext(), R.anim.start_fullscreen);
//            jcVideoPlayer.setAnimation(ra);

            // 设置状态
            onStateNormal();

            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开启小窗口
     */
//    public void startWindowTiny() {
//        Log.i(TAG, "startWindowTiny " + " [" + this.hashCode() + "] ");
//        onEvent(JCUserAction.ON_ENTER_TINYSCREEN);
//        if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) return;
//        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
//                .findViewById(Window.ID_ANDROID_CONTENT);
//        View old = vp.findViewById(TINY_ID);
//        if (old != null) {
//            vp.removeView(old);
//        }
//        surfaceViewContainer.removeView(JCMediaManager.sSurfaceView);
//        try {
//            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
//            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());
//            jcVideoPlayer.setId(TINY_ID);
//            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 400);
//            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
//            vp.addView(jcVideoPlayer, lp);
//            jcVideoPlayer.setUp(urlMap, currentUrlMapIndex, JCVideoPlayerStandard.SCREEN_WINDOW_TINY, objects);
//            jcVideoPlayer.setState(currentState);
//            jcVideoPlayer.addSurfaceView();
//            JCVideoPlayerManager.setSecondFloor(jcVideoPlayer);
//            onStateNormal();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 开启全屏
     *
     * @param context
     * @param _class
     * @param url
     * @param objects
     */
    public static void startFullscreen(Context context, Class _class, String url, Object... objects) {
        LinkedHashMap map = new LinkedHashMap();
        map.put(URL_KEY_DEFAULT, url);
        startFullscreen(context, _class, map, 0, objects);
    }

    public static void startFullscreen(Context context, Class _class, LinkedHashMap urlMap, int defaultUrlMapIndex, Object... objects) {
        hideSupportActionBar(context);
        JCUtils.getAppCompActivity(context).setRequestedOrientation(FULLSCREEN_ORIENTATION);
        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(context))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(JCVideoPlayer.FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
        try {
            Constructor<JCVideoPlayer> constructor = _class.getConstructor(Context.class);
            final JCVideoPlayer jcVideoPlayer = constructor.newInstance(context);
            jcVideoPlayer.setId(JCVideoPlayer.FULLSCREEN_ID);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jcVideoPlayer, lp);
//            final Animation ra = AnimationUtils.loadAnimation(context, R.anim.start_fullscreen);
//            jcVideoPlayer.setAnimation(ra);
            jcVideoPlayer.setUp(urlMap, defaultUrlMapIndex, JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, objects);
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();

            // 模拟手指主动执行点击事件
            jcVideoPlayer.startButton.performClick();

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //isCurrentJcvd and isCurrenPlayUrl should be two logic methods,isCurrentJcvd is for different jcvd with same
    //url when fullscreen or tiny screen. isCurrenPlayUrl is to find where is myself when back from tiny screen.
    //Sometimes they are overlap.
    // 判断是否是当前的JCVideoPlayer
    public boolean isCurrentJcvd() {//虽然看这个函数很不爽，但是干不掉
        return JCVideoPlayerManager.getCurrentJcvd() != null
                && JCVideoPlayerManager.getCurrentJcvd() == this;
    }


    //退出全屏和小窗的方法
    public void playOnThisJcvd() {

        Log.i(TAG, "playOnThisJcvd " + " [" + this.hashCode() + "] ");

        //1.清空全屏和小窗的jcvd
        currentState = JCVideoPlayerManager.getSecondFloor().currentState;
        currentUrlMapIndex = JCVideoPlayerManager.getSecondFloor().currentUrlMapIndex;

        clearFloatScreen(); // 清除漂浮的小窗口

        //2.在本jcvd上播放，这是接着播放的
        setState(currentState);

        addSurfaceView();
    }

    /**
     * onBackPressed 后退逻辑
     *
     * @return
     */
    public static boolean backPress() {

        Log.e(TAG, "backPress");

        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;

        // 这里判断有几层显示器
        if (JCVideoPlayerManager.getFirstFloor() != null && (JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN)) {//以前我总想把这两个判断写到一起，这分明是两个独立是逻辑
            // 如果是 全屏状态 状态
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            //直接退出全屏和小窗
            JCVideoPlayerManager.getCurrentJcvd().currentState = CURRENT_STATE_NORMAL;
            JCVideoPlayerManager.getFirstFloor().clearFloatScreen();
            JCMediaManager.instance().releaseMediaPlayer();
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
    public static void showSupportActionBar(Context context) {
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
    public static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = JCUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            JCUtils.getAppCompActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    //重力感应的时候调用的函数，
    public void autoFullscreen(float x) {
        if (isCurrentJcvd()
                && currentState == CURRENT_STATE_PLAYING
                && currentScreen != SCREEN_WINDOW_FULLSCREEN) {
            if (x > 0) {
                JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                JCUtils.getAppCompActivity(getContext()).setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            onEvent(JCUserAction.ON_ENTER_FULLSCREEN);
            startWindowFullscreen();
        }
    }

    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && isCurrentJcvd()
                && currentState == CURRENT_STATE_PLAYING
                && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    public static class JCAutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
            if (((x > -15 && x < -10) || (x < 15 && x > 10)) && Math.abs(y) < 1.5) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                    if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                        JCVideoPlayerManager.getCurrentJcvd().autoFullscreen(x);
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
    public static void setJcUserAction(JCUserAction jcUserEvent) {
        JC_USER_EVENT = jcUserEvent;
    }

    /**
     * 事件传递
     *
     * @param type
     */
    public void onEvent(int type) {
        if (JC_USER_EVENT != null && isCurrentJcvd() && urlMap != null) {
            JC_USER_EVENT.onEvent(type, JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex), currentScreen, objects);
        }
    }

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Log.i(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        if (JCMediaManager.instance().mediaPlayer != null &&
                                JCMediaManager.instance().mediaPlayer.isPlaying()) {
                            JCMediaManager.instance().mediaPlayer.pause();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    /**
     * 如有需要就在子类中重写
     */
    public void onSeekComplete() {

    }

    public void showWifiDialog(int event) {
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
