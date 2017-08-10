package fm.jiecao.jcvideoplayer_lib;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.tqit.stereorast.render.TQITVideoRenderer;

/**
 * Created by LiaoHongjie on 2017/8/10.
 */

public class MySurfaceView extends GLSurfaceView {

    protected static final String TAG = "MySurfaceView";

    private TQITVideoRenderer mVideoRenderer;
    public JCMediaManager mJCVideoPlayerManager;
    // x as width, y as height
    protected Point mVideoSize;


    public void setJCMediaManager(JCMediaManager manager) {
        this.mJCVideoPlayerManager = manager;
        JCMediaManager.sSurface = mVideoRenderer.getVideoSurface();
    }

    public MySurfaceView(Context context) {
        super(context);
        init(context);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mVideoRenderer = new TQITVideoRenderer(context);
        setRenderer(mVideoRenderer);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

//        mVideoSize = new Point(0, 0);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        mJCVideoPlayerManager.prepare();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
/*
        boolean hasValidSize = (mVideoSize.x == w && mVideoSize.y == h);
        if (hasValidSize) {

            mJCVideoPlayerManager.
        }*/
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        mJCVideoPlayerManager.releaseMediaPlayer();
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

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//
//        Log.i(TAG, "onMeasure " + " [" + this.hashCode() + "] ");
//
//        int viewRotation = (int) getRotation();
//        int videoWidth = mVideoSize.x;
//        int videoHeight = mVideoSize.y;
//
//        Log.i(TAG, "videoWidth = " + videoWidth + ", " + "videoHeight = " + videoHeight);
//        Log.i(TAG, "viewRotation = " + viewRotation);
//
//        // 如果判断成立，则说明显示的TextureView和本身的位置是有90度的旋转的，所以需要交换宽高参数。
//        if (viewRotation == 90 || viewRotation == 270) {
//            int tempMeasureSpec = widthMeasureSpec;
//            widthMeasureSpec = heightMeasureSpec;
//            heightMeasureSpec = tempMeasureSpec;
//        }
//
//        int width = getDefaultSize(videoWidth, widthMeasureSpec);
//        int height = getDefaultSize(videoHeight, heightMeasureSpec);
//        if (videoWidth > 0 && videoHeight > 0) {
//
//            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
//            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
//            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
//            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
//
//            Log.i(TAG, "widthMeasureSpec  [" + MeasureSpec.toString(widthMeasureSpec) + "]");
//            Log.i(TAG, "heightMeasureSpec [" + MeasureSpec.toString(heightMeasureSpec) + "]");
//
//            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
//                // the size is fixed
//                width = widthSpecSize;
//                height = heightSpecSize;
//                // for compatibility, we adjust size based on aspect ratio
//                if (videoWidth * height < width * videoHeight) {
//                    width = height * videoWidth / videoHeight;
//                } else if (videoWidth * height > width * videoHeight) {
//                    height = width * videoHeight / videoWidth;
//                }
//            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
//                // only the width is fixed, adjust the height to match aspect ratio if possible
//                width = widthSpecSize;
//                height = width * videoHeight / videoWidth;
//                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
//                    // couldn't match aspect ratio within the constraints
//                    height = heightSpecSize;
//                    width = height * videoWidth / videoHeight;
//                }
//            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
//                // only the height is fixed, adjust the width to match aspect ratio if possible
//                height = heightSpecSize;
//                width = height * videoWidth / videoHeight;
//                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
//                    // couldn't match aspect ratio within the constraints
//                    width = widthSpecSize;
//                    height = width * videoHeight / videoWidth;
//                }
//            } else {
//                // neither the width nor the height are fixed, try to use actual video size
//                width = videoWidth;
//                height = videoHeight;
//                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
//                    // too tall, decrease both width and height
//                    height = heightSpecSize;
//                    width = height * videoWidth / videoHeight;
//                }
//                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
//                    // too wide, decrease both width and height
//                    width = widthSpecSize;
//                    height = width * videoHeight / videoWidth;
//                }
//            }
//        } else {
//            // no size yet, just adopt the given spec sizes
//        }
//        setMeasuredDimension(width, height);
//    }
}
