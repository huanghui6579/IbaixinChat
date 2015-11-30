package net.ibaixin.chat.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import net.ibaixin.chat.util.SystemUtil;

/**
 * 创建人：huanghui1
 * 创建时间： 2015/11/30 11:21
 * 修改人：huanghui1
 * 修改时间：2015/11/30 11:21
 * 修改备注：
 *
 * @version: 0.0.1
 */
public class ActivityOptionsCompatICS {

    /**
     * 传递的结果码，用来判断
     */
    public final static int RESULT_CODE = 1314;

    /**
     * 结果码的KEY
     */
    public static String KEY_RESULT_CODE = "kale:resultCode";

    /**
     * Type of animation that arguments specify.
     * 设置动画的类型，在启动activity时用到
     * @hide
     */
    public static final String KEY_ANIM_TYPE = "kale:animType";

    /**
     * Custom enter animation resource ID.
     * 进入动画的id
     */
    public static final String KEY_ANIM_ENTER_RES_ID = "kale:animEnterRes";

    /**
     * Custom exit animation resource ID.
     * 退出动画的id
     */
    public static final String KEY_ANIM_EXIT_RES_ID = "kale:animExitRes";

    /**
     * 判断当前屏幕是否是竖屏
     */
    public static final String KEY_IS_VERTICAL_SCREEN = "kale:isVerticalScreen";

    /**
     * 判断现在view是否在屏幕上显示
     */
    public static final String KEY_IS_IN_THE_SCREEN = "kale:isInTheScreen";

    /**
     * Initial width of the animation.
     * 设置view开始的的宽度，用户绘制控件的原始宽度
     */
    public static final String KEY_ANIM_WIDTH = "kale:animWidth";

    /**
     * Initial height of the animation.
     * 设置view开始的高度，用于绘制控件开始的高度
     */
    public static final String KEY_ANIM_HEIGHT = "kale:animHeight";

    /**
     * Start X position of thumbnail animation.
     * 设置view开始的X坐标，用于绘制控件的原始位置
     */
    public static final String KEY_ANIM_START_X = "kale:animStartX";

    /**
     * Start Y position of thumbnail animation.
     * 设置view开始的Y坐标，用于绘制控件的原始位置
     */
    public static final String KEY_ANIM_START_Y = "kale:animStartY";
    
    /**
     * 没有动画 
     */
    public static final int ANIM_NONE = 0;

    /**
     * 自定义动画：makeCustomAnimation	
     */
    public static final int ANIM_CUSTOM = 1;

    /**
     * 拉伸动画：makeScaleUpAnimation	
     */
    public static final int ANIM_SCALE_UP = 2;

//    /**
//     * Bitmap的动画效果，从bitmap渐变到activity
//     */
//    public static final int ANIM_THUMBNAIL_SCALE_UP = 3;

    private int mAnimationType = ANIM_NONE;

    // 当前是否是竖屏
    private boolean mIsVerticalScreen;
    // 当前是否是全屏
    private boolean mIsStartFullScreen;
    // 当前view是否在屏幕上
    private boolean mIsInTheScreen;

    private int mStartX;
    private int mStartY;
    private int mWidth;
    private int mHeight;

    private int mCustomEnterResId;
    private int mCustomExitResId;

    /**
     * 默认的动画效果，等于没进行设置
     */
    public static final int ANIM_DEFAULT = 6;

    /**
     * @param source：新activity开始动画的view，通过startX，startY来定义区域
     * @param startX：动画开始的X坐标，相对于source左上角的X坐标
     * @param startY：动画开始的Y坐标，相对于source左上角的Y坐标
     * @param width：新activity的起始宽度
     * @param height：新activity的起始高度
     *
     * Create an ActivityOptions specifying an animation where the new activity is
     * scaled from a small originating area of the screen to its final full
     * representation.
     * <p/>
     * If the Intent this is being used with has not set its
     * {@link android.content.Intent#setSourceBounds(android.graphics.Rect)},
     * those bounds will be filled in for you based on the initial bounds passed
     * in here.
     *
     * @param source The View that the new activity is animating from. This
     * defines the coordinate space for startX and startY.
     * @param startX The x starting location of the new activity, relative to
     * source.
     * @param startY The y starting location of the activity, relative to source.
     * @param width The initial width of the new activity.
     * @param height The initial height of the new activity.
     * @return Returns a new ActivityOptions object that you can use to supply
     * these options as the options Bundle when starting an activity.
     *
     */
    public static ActivityOptionsCompatICS makeScaleUpAnimation(View source,
                                                                int startX, int startY, int width, int height) {
        Activity activity = (Activity) source.getContext();
        ActivityOptionsCompatICS opts = new ActivityOptionsCompatICS();
        // 设置动画类型
        opts.mAnimationType = ANIM_SCALE_UP;
        // 判断当前是否是竖屏
        opts.mIsVerticalScreen = SystemUtil.isVerticalScreen(activity);
        // 判断view是否在屏幕上，如果在就执行动画，否则不执行动画
        opts.mIsInTheScreen = SystemUtil.isInScreen(activity, source);

        int[] pts = new int[2];//ps = position，目的得到当前view相对于屏幕的坐标
        source.getLocationOnScreen(pts);
        // 设置起始坐标和起始宽高
        opts.mStartX = pts[0] + startX;
        opts.mStartY = pts[1] + startY;
        opts.mWidth = width;
        opts.mHeight = height;

        return opts;
    }

    /**
     * 将各种坐标和参数放入bundle中传递
     *
     * Returns the created options as a Bundle, which can be passed to
     * {@link android.content.Context#startActivity(android.content.Intent, android.os.Bundle)
     * Context.startActivity(Intent, Bundle)} and related methods.
     * Note that the returned Bundle is still owned by the ActivityOptions
     * object; you must not modify it, but can supply it to the startActivity
     * methods that take an options Bundle.
     */
    public Bundle toBundle() {
        if (mAnimationType == ANIM_DEFAULT) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ANIM_TYPE, mAnimationType);
        switch (mAnimationType) {

            case ANIM_CUSTOM:
                bundle.putInt(KEY_ANIM_ENTER_RES_ID, mCustomEnterResId);
                bundle.putInt(KEY_ANIM_EXIT_RES_ID, mCustomExitResId);
                break;

            case ANIM_SCALE_UP:
                bundle.putBoolean(KEY_IS_VERTICAL_SCREEN, mIsVerticalScreen);
                bundle.putBoolean(KEY_IS_IN_THE_SCREEN, mIsInTheScreen);

                bundle.putInt(KEY_ANIM_WIDTH, mWidth);
                bundle.putInt(KEY_ANIM_HEIGHT, mHeight);
                bundle.putInt(KEY_ANIM_START_X, mStartX);
                bundle.putInt(KEY_ANIM_START_Y, mStartY);
                break;

//            case ANIM_THUMBNAIL_SCALE_UP:
//                bundle.putBoolean(KEY_IS_START_FULL_SCREEN, mIsStartFullScreen);
//                bundle.putBoolean(KEY_IS_VERTICAL_SCREEN, mIsVerticalScreen);
//                bundle.putBoolean(KEY_IS_IN_THE_SCREEN, mIsInTheScreen);
//                bundle.putParcelable(KEY_ANIM_THUMBNAIL, mThumbnail);
//                bundle.putInt(KEY_ANIM_START_X, mStartX);
//                bundle.putInt(KEY_ANIM_START_Y, mStartY);
//                bundle.putInt(KEY_ANIM_WIDTH, mWidth);
//                bundle.putInt(KEY_ANIM_HEIGHT, mHeight);
//                break;
//
//            case ANIM_SCENE_TRANSITION:
//                bundle.putBoolean(KEY_IS_VERTICAL_SCREEN, mIsVerticalScreen);
//                bundle.putBoolean(KEY_IS_START_FULL_SCREEN, mIsStartFullScreen);
//
//                bundle.putBooleanArray(kEY_IS_IN_THE_SCREEN_ARR, mIsInTheScreenArr);
//                bundle.putIntegerArrayList(kEY_SHARED_ELEMENT_ID_LIST, mSharedElementIds);
//                bundle.putParcelableArrayList(kEY_SHARED_ELEMENT_BOUNDS_LIST, mSharedElementBounds);
//                break;
        }

        return bundle;
    }
}
