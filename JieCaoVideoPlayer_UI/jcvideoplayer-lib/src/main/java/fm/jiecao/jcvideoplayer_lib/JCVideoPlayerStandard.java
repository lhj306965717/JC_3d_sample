package fm.jiecao.jcvideoplayer_lib;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Nathen
 * On 2016/04/18 16:15
 */
public class JCVideoPlayerStandard extends JCVideoPlayer {

    protected static Timer DISMISS_CONTROL_VIEW_TIMER;
    private boolean isLock = false;  // 屏幕是否被锁住
    public ImageView thumbImageView;
    protected DismissControlViewTimerTask mDismissControlViewTimerTask;
    private ImageView mIv_lock;
    private View topContainer;
    private TextView mTitle;
    private ImageView mBack;
    private ImageView mIv_download;


    public JCVideoPlayerStandard(Context context) {
        super(context);
    }

    public JCVideoPlayerStandard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutId() {
        return R.layout.jc_layout_standard;
    }

    @Override
    public void init(Context context) {

        super.init(context);

        // 播放器封面
        thumbImageView = (ImageView) findViewById(R.id.thumb);

        // 总的顶部UI 部分
        topContainer = findViewById(R.id.layout_top);
        // 标题
        mTitle = (TextView) findViewById(R.id.title);
        // 返回
        mBack = (ImageView) findViewById(R.id.back);
        // 下载图标
        mIv_download = (ImageView) findViewById(R.id.iv_download);


        // 全屏锁图标
        mIv_lock = (ImageView) findViewById(R.id.iv_lock);

        // 暂时注销 封面的的点击事件
//      thumbImageView.setOnClickListener(this);
//      thumbImageView.setVisibility(View.INVISIBLE);

    }

    public void setUp(LinkedHashMap urlMap, int defaultUrlMapIndex, int screen, Object... objects) {
        // 注意：先走父类函数
        super.setUp(urlMap, defaultUrlMapIndex, screen, objects);

        if (objects.length == 0)
            return;

        // 设置标题
        mTitle.setText(objects[0].toString());

        // 判断是否是全屏状态
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            mBack.setVisibility(View.VISIBLE);
            // 判断是否是正常状态
        } else if (currentScreen == SCREEN_LAYOUT_NORMAL /*|| currentScreen == SCREEN_LAYOUT_LIST*/) {
            mBack.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.fl_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
//                        onClickUiToggle(); // 更新显示UI

                        Log.e("TAG", "触摸---->状态："+currentState);

                        setUIState(currentState); // 根据状态显示UI
                    }
                    break;
            }
        }
        return super.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int i = v.getId();

        if (i == R.id.thumb) { // 封面图片响应
            if (TextUtils.isEmpty(JCUtils.getCurrentUrlFromMap(urlMap, currentUrlMapIndex))) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentState == CURRENT_STATE_NORMAL) {
                if (!JCUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog(JCUserActionStandard.ON_CLICK_START_THUMB);
                    return;
                }

                startVideo();

            }
        } else if (i == R.id.back) {

            backPress();

        } else if (i == R.id.iv_lock) {


        } else if (i == R.id.iv_download) {


        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        startDismissControlViewTimer();
    }

    @Override
    public void setUIState(int state, int urlMapIndex, int seekToInAdvance) {

        switch (state) {
            case CURRENT_STATE_NORMAL:  // 空闲状态

                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mFl_retry.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                bottomProgressBar.setVisibility(View.INVISIBLE);

                updateStartImage();

                break;
            case CURRENT_STATE_PREPARING:  // 准备状态

                mLoading.setVisibility(View.VISIBLE);

                bottomProgressBar.setVisibility(View.INVISIBLE);
                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mFl_retry.setVisibility(View.INVISIBLE);

                break;
            case CURRENT_STATE_PREPARING_CHANGING_URL: // 重新设置URL地址播放

                mLoading.setVisibility(VISIBLE);

                onStatePreparingChangingUrl(urlMapIndex, seekToInAdvance);

                bottomProgressBar.setVisibility(View.INVISIBLE);
                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mFl_retry.setVisibility(View.INVISIBLE);

                break;
            case CURRENT_STATE_PLAYING:  // 播放状态

                /* onVideoRendingStart */

                if (bottomContainer.getVisibility() == View.VISIBLE) {

                    topContainer.setVisibility(View.INVISIBLE);
                    mIv_lock.setVisibility(View.INVISIBLE);
                    bottomContainer.setVisibility(View.INVISIBLE);
                    mFl_retry.setVisibility(View.INVISIBLE);
                    mLoading.setVisibility(View.INVISIBLE);

                    bottomProgressBar.setVisibility(View.VISIBLE);

                } else {

                    mFl_retry.setVisibility(View.INVISIBLE);
                    mLoading.setVisibility(View.INVISIBLE);

                    bottomContainer.setVisibility(View.VISIBLE);
                    bottomProgressBar.setVisibility(View.VISIBLE);
                    topContainer.setVisibility(View.VISIBLE);

                    switch (currentScreen) {
                        case SCREEN_LAYOUT_NORMAL:

                        {
                            mTitle.setVisibility(View.INVISIBLE);
                            mIv_download.setVisibility(View.INVISIBLE);
                        }

                        mIv_lock.setVisibility(View.INVISIBLE);

                        break;
                        case SCREEN_WINDOW_FULLSCREEN:

                        {
                            mTitle.setVisibility(View.VISIBLE);
                            mIv_download.setVisibility(View.VISIBLE);
                        }

                        mIv_lock.setVisibility(View.INVISIBLE);

                        break;
                    }

                    updateStartImage();

                    startDismissControlViewTimer();
                }
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:  // 缓冲状态

                mLoading.setVisibility(View.VISIBLE);
                bottomProgressBar.setVisibility(View.VISIBLE);

                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mFl_retry.setVisibility(View.INVISIBLE);

                break;
            case CURRENT_STATE_PAUSE:  // 暂停状态

                if (bottomContainer.getVisibility() == View.VISIBLE) {

                    topContainer.setVisibility(View.INVISIBLE);
                    mIv_lock.setVisibility(View.INVISIBLE);
                    bottomContainer.setVisibility(View.INVISIBLE);
                    mFl_retry.setVisibility(View.INVISIBLE);
                    mLoading.setVisibility(View.INVISIBLE);

                    bottomProgressBar.setVisibility(View.VISIBLE);

                } else {

                    mFl_retry.setVisibility(View.INVISIBLE);
                    mLoading.setVisibility(View.INVISIBLE);

                    bottomContainer.setVisibility(View.VISIBLE);
                    bottomProgressBar.setVisibility(View.VISIBLE);
                    topContainer.setVisibility(View.VISIBLE);

                    switch (currentScreen) {
                        case SCREEN_LAYOUT_NORMAL:

                        {
                            mTitle.setVisibility(View.INVISIBLE);
                            mIv_download.setVisibility(View.INVISIBLE);
                        }

                        mIv_lock.setVisibility(View.INVISIBLE);

                        break;
                        case SCREEN_WINDOW_FULLSCREEN:

                        {
                            mTitle.setVisibility(View.VISIBLE);
                            mIv_download.setVisibility(View.VISIBLE);
                        }

                        mIv_lock.setVisibility(View.INVISIBLE);

                        break;
                    }

                    updateStartImage();

                    startDismissControlViewTimer();
                }
                break;
            case CURRENT_STATE_AUTO_COMPLETE:  // 播放完成状态

                mFl_retry.setVisibility(View.VISIBLE);

                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                bottomProgressBar.setVisibility(View.INVISIBLE);

                updateStartImage();

                cancelDismissControlViewTimer();

                break;
            case CURRENT_STATE_ERROR:  // 错误状态

                mFl_retry.setVisibility(View.VISIBLE);

                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                bottomProgressBar.setVisibility(View.INVISIBLE);

                updateStartImage();

                cancelDismissControlViewTimer();

                break;
        }
    }

    /**
     * 点击Dilaog显示情况下，隐藏UI
     */
    public void onCLickUiToggleToClear() {
        if (currentState == CURRENT_STATE_PREPARING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                setUIState(CURRENT_STATE_PREPARING);
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {

                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mFl_retry.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);

                bottomProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {

                topContainer.setVisibility(View.INVISIBLE);
                mIv_lock.setVisibility(View.INVISIBLE);
                bottomContainer.setVisibility(View.INVISIBLE);
                mFl_retry.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);

                bottomProgressBar.setVisibility(View.VISIBLE);

            }
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                setUIState(CURRENT_STATE_AUTO_COMPLETE);
            }
        } else if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                setUIState(CURRENT_STATE_PLAYING_BUFFERING_START);
            }
        }
    }


    /*    +++++++++++++++++       UI 状态        +++++++++++++++          */

    public void updateStartImage() {
        if (currentState == CURRENT_STATE_PLAYING) {

            startButton.setImageResource(R.drawable.icon_start);

        } else if (currentState == CURRENT_STATE_ERROR) {

            Drawable drawable = getContext().getResources().getDrawable(R.drawable.jc_error_normal);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            mRetryText.setCompoundDrawables(null, drawable, null, null);

            // 判断网络是否连接
            if (!JCUtils.isNetworkConnected(getContext())) {
                mRetryText.setText(getContext().getString(R.string.network_error_retry));
            } else {
                mRetryText.setText(getContext().getString(R.string.click_retry));
            }

            // 取消显示
            cancelProgressTimer();
            cancelDismissControlViewTimer();

        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {

            Drawable drawable = getContext().getResources().getDrawable(R.drawable.jc_restart_normal);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            mRetryText.setCompoundDrawables(null, drawable, null, null);

            mRetryText.setText(getContext().getString(R.string.replay));

        } else {

            startButton.setImageResource(R.drawable.icon_bf_stop);
        }
    }


    /***************   这是另外一部分弹窗显示的 UI      *******************/


    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;

    @Override
    public void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.jc_dialog_progress, null);
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mProgressDialog = createDialogWithView(localView);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        mDialogProgressBar.setProgress(totalTimeDuration <= 0 ? 0 : (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.jc_forward_icon);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.jc_backward_icon);
        }
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    protected Dialog mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;
    protected TextView mDialogVolumeTextView;
    protected ImageView mDialogVolumeImageView;

    @Override
    public void showVolumeDialog(float deltaY, int volumePercent) {
        super.showVolumeDialog(deltaY, volumePercent);
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.jc_dialog_volume, null);
            mDialogVolumeImageView = ((ImageView) localView.findViewById(R.id.volume_image_tip));
            mDialogVolumeTextView = ((TextView) localView.findViewById(R.id.tv_volume));
            mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
            mVolumeDialog = createDialogWithView(localView);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (volumePercent <= 0) {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.jc_close_volume);
        } else {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.jc_add_volume);
        }
        if (volumePercent > 100) {
            volumePercent = 100;
        } else if (volumePercent < 0) {
            volumePercent = 0;
        }
        mDialogVolumeTextView.setText(volumePercent + "%");
        mDialogVolumeProgressBar.setProgress(volumePercent);
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissVolumeDialog() {
        super.dismissVolumeDialog();
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    protected Dialog mBrightnessDialog;
    protected ProgressBar mDialogBrightnessProgressBar;
    protected TextView mDialogBrightnessTextView;

    @Override
    public void showBrightnessDialog(int brightnessPercent) {
        super.showBrightnessDialog(brightnessPercent);
        if (mBrightnessDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.jc_dialog_brightness, null);
            mDialogBrightnessTextView = ((TextView) localView.findViewById(R.id.tv_brightness));
            mDialogBrightnessProgressBar = ((ProgressBar) localView.findViewById(R.id.brightness_progressbar));
            mBrightnessDialog = createDialogWithView(localView);
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        if (brightnessPercent > 100) {
            brightnessPercent = 100;
        } else if (brightnessPercent < 0) {
            brightnessPercent = 0;
        }
        mDialogBrightnessTextView.setText(brightnessPercent + "%");
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissBrightnessDialog() {
        super.dismissBrightnessDialog();
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
        }
    }

    public Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(getContext(), R.style.jc_style_dialog_progress);
        dialog.setContentView(localView);
        Window window = dialog.getWindow();
        window.addFlags(Window.FEATURE_ACTION_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        WindowManager.LayoutParams localLayoutParams = window.getAttributes();
        localLayoutParams.gravity = Gravity.CENTER;
        window.setAttributes(localLayoutParams);
        return dialog;
    }

    /**
     * 启动时间显示
     */
    public void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        DISMISS_CONTROL_VIEW_TIMER = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISMISS_CONTROL_VIEW_TIMER.schedule(mDismissControlViewTimerTask, 3000); // 表示执行的时间
    }

    /**
     * 取消时间显示
     */
    public void cancelDismissControlViewTimer() {
        if (DISMISS_CONTROL_VIEW_TIMER != null) {
            DISMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
        }
    }

    public class DismissControlViewTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState != CURRENT_STATE_NORMAL && currentState != CURRENT_STATE_ERROR && currentState != CURRENT_STATE_AUTO_COMPLETE) {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        bottomContainer.setVisibility(View.INVISIBLE);
                        topContainer.setVisibility(View.INVISIBLE);
                        mIv_lock.setVisibility(View.INVISIBLE);

                        bottomProgressBar.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    @Override
    public void onAutoCompletion() {
        super.onAutoCompletion();
        cancelDismissControlViewTimer();
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        cancelDismissControlViewTimer();
    }
}
