package fm.jiecao.jiecaovideoplayer.CustomView;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import fm.jiecao.jiecaovideoplayer.R;

//import com.facebook.drawee.view.SimpleDraweeView;

/**
 * Just replace thumb from ImageView to SimpleDraweeView
 * Created by Nathen
 * On 2016/05/01 22:59
 */
public class JCVideoPlayerStandardFresco extends JCVideoPlayerStandard {
    //    public SimpleDraweeView thumbImageView;

    public JCVideoPlayerStandardFresco(Context context) {
        super(context);
    }

    public JCVideoPlayerStandardFresco(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        bottomProgressBar = (ProgressBar) findViewById(R.id.bottom_progress);
//        mTitle = (TextView) findViewById(R.id.title);
//        mBack = (ImageView) findViewById(R.id.back);
//        mLoading = (ProgressBar) findViewById(R.id.loading);
//        mBack.setOnClickListener(this);
    }

    @Override
    public void setUp(String url, int screen, Object... objects) {
        super.setUp(url, screen, objects);
        if (objects.length == 0) return;
//        mTitle.setText(objects[0].toString());
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_standard_fresco;
    }


}
