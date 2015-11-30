package net.ibaixin.chat.app;

import android.animation.Animator;
import android.app.Activity;
import android.os.Bundle;
import android.view.animation.Animation;

import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;

/**
 * 创建人：huanghui1
 * 创建时间： 2015/11/30 11:29
 * 修改人：huanghui1
 * 修改时间：2015/11/30 11:29
 * 修改备注：
 *
 * @version: 0.0.1
 */
public class TransitionCompat {

    // 判断动画是否在进行中
    private static boolean mIsPlaying = false;

    // 判断当前是否是activity的进入状态
    public static boolean mIsEnter;

    private static TransitionListener mTransitionListener;

    /** @hide 内部的listener */
    private static MyTransitionListener mListener;

    private static Bundle mBundle;

    private static int mAnimationType;

    private static boolean mIsVerticalScreen;
    private static boolean mIsInTheScreen;

    private static TransitionAnims mTransitionAnims;

    private static int mWidth;
    private static int mHeight;
    private static int mStartX;
    private static int mStartY;

    /**
     * @param activity
     * @param layoutResId
     */
    public static void startTransition(final Activity activity, final int layoutResId) {
        if (mIsPlaying) {
            return;
        }
        Bundle bundle = activity.getIntent().getExtras();
        if (bundle == null) {
            Log.w("ActivityOptions's Bundle is null");
            return;
        }
        mIsEnter = true;
        mListener = new TransitionCompat().new MyTransitionListener();
        mBundle = bundle;
        mAnimationType = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_TYPE, ActivityOptionsCompatICS.ANIM_NONE);
        mIsVerticalScreen = bundle.getBoolean(ActivityOptionsCompatICS.KEY_IS_VERTICAL_SCREEN);
        /**
         * 根据type的不同开始执行不同的动画效果
         */
        switch (mAnimationType) {

            case ActivityOptionsCompatICS.ANIM_SCALE_UP:
                mIsInTheScreen = bundle.getBoolean(ActivityOptionsCompatICS.KEY_IS_IN_THE_SCREEN);
                mWidth = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_WIDTH);
                mHeight = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_HEIGHT);
                mStartX = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_START_X);
                mStartY = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_START_Y);
                //开始执行动画，这里的tue表示是开始状态
                scaleUpAnimation(activity, true);
                break;

            /*case ActivityOptionsCompatICS.ANIM_THUMBNAIL_SCALE_UP:
                mIsStartFullScreen = bundle.getBoolean(ActivityOptionsCompatICS.KEY_IS_START_FULL_SCREEN);
                mThumbnail = (Bitmap) bundle.getParcelable(ActivityOptionsCompatICS.KEY_ANIM_THUMBNAIL);
                mIsInTheScreen = bundle.getBoolean(ActivityOptionsCompatICS.KEY_IS_IN_THE_SCREEN);
                mWidth = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_WIDTH);
                mHeight = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_HEIGHT);
                mStartX = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_START_X);
                mStartY = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_START_Y);
                //开始执行动画，这里的tue表示是开始状态
                thumbnailScaleUpAnimation(activity, true);
                break;

            case ActivityOptionsCompatICS.ANIM_SCENE_TRANSITION:
                mIsStartFullScreen = bundle.getBoolean(ActivityOptionsCompatICS.KEY_IS_START_FULL_SCREEN);
                mIsInTheScreenArr = bundle.getBooleanArray(ActivityOptionsCompatICS.kEY_IS_IN_THE_SCREEN_ARR);
                mSharedElementIds = bundle.getIntegerArrayList(ActivityOptionsCompatICS.kEY_SHARED_ELEMENT_ID_LIST);
                mSharedElementBounds = bundle.getParcelableArrayList(ActivityOptionsCompatICS.kEY_SHARED_ELEMENT_BOUNDS_LIST);
                mLayoutResId = layoutResId;
                //开始执行动画，这里的tue表示是开始状态
                sceneTransitionAnimation(activity, layoutResId, true);
                break;*/
            case ActivityOptionsCompatICS.ANIM_NONE:
                break;
            default:
                break;
        }
        //执行场景进入的动画
        if (mTransitionAnims != null) {
//            mTransitionAnims.setAnimsInterpolator(mInterpolator);
//            mTransitionAnims.setAnimsStartDelay(mStartDelay);
//            mTransitionAnims.setAnimsDuration(mAnimTime);
            mTransitionAnims.addListener(mListener);
            mTransitionAnims.playScreenEnterAnims();
        }
        mTransitionAnims = null;
        //起始完成后就用不到bundle了，为了避免干扰，这里置空
        mBundle = null;
    }

    /**
     *
     * @param activity：当前的activity，可以是actionbarActivity
     * @param isEnter：是否是开始动画，如果是fale则执行结束的动画
     *
     * 执行ScaleUpAnimation动画，包括开始和结束的动画效果。
     * 【开始】
     * 前提判断开始的activity和当前的activity是不是处于同一个横竖模式，开始的activity的view是不是处于屏幕上
     * 这个动画涉及到了渐变，拉伸，移动的效果，三个动画效果需要同时执行。
     * 这里的动画起始坐标是相对于屏幕的坐标，渐变是从无到有。
     * alpha 0f->1f
     * x,y (startX , startY)->(0,0)屏幕左上角是(0,0)，注意下要通过setPivotX，setPivotY设定位移的开始坐标
     * scaleX，scaleY都是通过计算得出的，这个计算也比较容易就是算开始的宽高和屏幕宽高的比值。
     * 计算好后就可以执行动画了。这里的动画仅仅是做平面的动画，动画的view是当前屏幕的根视图。
     * 我为了动画执行方便，所以根视图我设定的是透明，所以用到动画效果的时候，可以自己给activity的主背景进行设置颜色。
     * 【结束】
     * 结束也是一样，开始判断横竖屏模式，还有起始时view在不在屏幕上
     * 接着就开始执行动画了，这部分比较简单，就是简单的基础动画。难点在于获得activity的根view，还有设定动画透明度
     */
    private static void scaleUpAnimation(final Activity activity, final boolean isEnter) {
        //如果开始的view不在屏幕上，那么就不执行动画
        if (!mIsInTheScreen) {
            return;
        }
        //如果当前手机的横竖模式和前一个activity的横竖模式不同，就不执行动画
        if (mIsVerticalScreen != SystemUtil.isVerticalScreen(activity)) {
            return;
        }
        //执行屏幕的动画
        if (mTransitionAnims == null) {
            final SceneScaleUp anim = new SceneScaleUp(activity,
                    mStartX, mStartY, mWidth, mHeight);
//            anim.setAnimsInterpolator(anim.getAnimsInterpolator());
//            anim.setAnimsStartDelay(mStartDelay);
//            anim.setAnimsDuration(mAnimTime);
            anim.addListener(mListener);
            activity.getWindow().getDecorView().post(new Runnable() {

                @Override
                public void run() {
                    // TODO 自动生成的方法存根
                    anim.playScreenAnims(isEnter);
                }
            });

        }
    }

    /**
     * 在Activity.onBackPressed()中执行的方法，请不要执行onBackPressed()的super方法体
     * 这时候已经可以确保要执行动画的view显示完成了，所以可以安全的执行这个方法
     *
     * @param activity
     */
    public static void finishAfterTransition(Activity activity, int resultCode) {
        if (mIsPlaying) {
            return;
        }
        mIsEnter = false;
        activity.setResult(resultCode);
        mListener = new TransitionCompat().new MyTransitionListener();
        //开始执行动画，这里的false表示执行结束的动画
        switch (mAnimationType) {

            case ActivityOptionsCompatICS.ANIM_SCALE_UP:
                scaleUpAnimation(activity, false);
                break;
//            case ActivityOptionsCompatICS.ANIM_THUMBNAIL_SCALE_UP:
//                thumbnailScaleUpAnimation(activity, false);
//                break;
//            case ActivityOptionsCompatICS.ANIM_SCENE_TRANSITION:
//                sceneTransitionAnimation(activity, mLayoutResId, false);
//                break;
            case ActivityOptionsCompatICS.ANIM_NONE:
                activity.finish();
                return;
            default:
                activity.finish();
                return;
        }
        //执行场景退出的动画
        if (mTransitionAnims != null) {

//            mTransitionAnims.setAnimsInterpolator(mInterpolator);
//            mTransitionAnims.setAnimsStartDelay(mStartDelay);
//            mTransitionAnims.setAnimsDuration(mAnimTime);
            mTransitionAnims.addListener(mListener);
            mTransitionAnims.playScreenExitAnims();
        }
        mTransitionAnims = null;
    }

    /**
     * 在Activity.onBackPressed()中执行的方法，请不要执行onBackPressed()的super方法体
     * 这时候已经可以确保要执行动画的view显示完成了，所以可以安全的执行这个方法
     *
     * @param activity
     */
    public static void finishAfterTransition(Activity activity) {
        finishAfterTransition(activity, Activity.RESULT_CANCELED);
    }

    /**
     * @author:Jack Tony
     * @tips  :设置屏幕动画的监听器
     * @date  :2014-11-22
     */
    public interface TransitionListener {
        public void onTransitionStart(Animator animator, Animation animation, boolean isEnter);
        public void onTransitionEnd(Animator animator, Animation animation, boolean isEnter);
        public void onTransitionCancel(Animator animator, Animation animation, boolean isEnter);
    }

    /**
     * @author:Jack Tony
     * @tips  :用来判断动画执行的监听器，如果动画在执行中，那么就不会再启动动画了。只有当动画执行完成才能再次启动动画
     * 这里传入的enter是通过这个类得到的，和监听器无关。因为通过这个类才能准确的得到当前是进入状态还是退出状态
     * @date  :2014-11-26
     * @hide
     */
    private class MyTransitionListener implements TransitionListener {

        @Override
        public void onTransitionStart(Animator animator, Animation animation, boolean e) {
            // TODO 自动生成的方法存根
            if (mTransitionListener != null) {
                mTransitionListener.onTransitionStart(animator, animation, mIsEnter);
            }
            mIsPlaying = true;
        }

        @Override
        public void onTransitionEnd(Animator animator, Animation animation, boolean e) {
            // TODO 自动生成的方法存根
            if (mTransitionListener != null) {
                mTransitionListener.onTransitionEnd(animator, animation, mIsEnter);
            }
            mIsPlaying = false;
        }

        @Override
        public void onTransitionCancel(Animator animator, Animation animation, boolean e) {
            // TODO 自动生成的方法存根
            if (mTransitionListener != null) {
                mTransitionListener.onTransitionCancel(animator, animation, mIsEnter);
            }
        }

    }
}
