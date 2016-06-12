package com.brucetoo.pinterestview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Bruce Too
 * On 10/2/15.
 * At 11:10
 * TODO get pin view top location...
 */
public class PinterestView extends ViewGroup implements View.OnTouchListener{

    private final static String TAG = "PinterestView";

    private final static int ANIMATION_DURATION = 200;

    private int mChildSize;

    public static final float DEFAULT_BETWTEEN_ANGLE = 30;

    public static final float DEFAULT_FROM_DEGREES = -90.0f;

    public static final float DEFAULT_TO_DEGREES = -90.0f;

    public static final int DEFAULT_CHILD_SIZE = 44;

    public static final int DEFAULT_RECT_RADIUS = 100;

    private float mFromDegrees = DEFAULT_FROM_DEGREES;

    private float mToDegrees = DEFAULT_TO_DEGREES;

    private static final int DEFAULT_RADIUS = 160;//px

    private int mRadius;

    private Context mContext;

    private boolean mExpanded = false;

    private ArrayList<View> mChildViews = new ArrayList<>();

    private float mCenterX;
    private float mCenterY;

    private PinterestView.PinMenuClickListener mPinMenuClickListener;

    private PopupWindow mPopTips;

    final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent e) {
            mCenterX = e.getRawX();
            mCenterY = e.getRawY();
            confirmDegreeRangeByCenter(mCenterX, mCenterY);
            PinterestView.this.setVisibility(View.VISIBLE);
            switchState();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mPinMenuClickListener != null) {
                mPinMenuClickListener.onPreViewClick();
            }
            return true;
        }
    });

    public PinterestView(Context context) {
        super(context);
        this.mContext = context;
    }

    public PinterestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        mRadius = dp2px(DEFAULT_RADIUS / 2);
        createTipsPopWindow(context);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PinterestView, 0, 0);
//            mFromDegrees = a.getFloat(R.styleable.PinterestView_fromDegrees, DEFAULT_FROM_DEGREES);
//            mToDegrees = a.getFloat(R.styleable.PinterestView_toDegrees, DEFAULT_TO_DEGREES);
            mChildSize = a.getDimensionPixelSize(R.styleable.PinterestView_childSize, DEFAULT_CHILD_SIZE);
            a.recycle();
        }

        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (getVisibility() == VISIBLE) {
            handleTouchEvent(event);
            return true;
        }
        return gestureDetector.onTouchEvent(event);
    }


    private static double distSq(double x1, double y1, double x2, double y2) {
        return Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
    }

    /**
     * find the nearest child view
     */
    private static View nearest(float x, float y, List<View> views) {
        double minDistSq = Double.MAX_VALUE;
        View minView = null;

        for (View view : views) {
            double distSq = distSq(x, y, view.getX() + view.getMeasuredWidth() / 2,
                    view.getY() + view.getMeasuredHeight() / 2);

            if (distSq < Math.pow(1.2f * view.getMeasuredWidth(), 2) && distSq < minDistSq) {
                minDistSq = distSq;
                minView = view;
            }
        }

        return minView;
    }

    private void handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                //only listen ACTION_MOVE when PinterestView is visible
                if (PinterestView.this.getVisibility() == VISIBLE) {

                    View nearest = nearest(event.getX(),event.getY(),mChildViews);

                    if(nearest != null){
                        float centerNearestX = nearest.getX() + nearest.getWidth() / 2;
                        float centerNearestY = nearest.getY() + nearest.getHeight() / 2;
                        float distance = (float) Math.sqrt(distSq(centerNearestX, centerNearestY, mCenterX, mCenterY));
                        float scaleRatio = distance / nearest.getMeasuredWidth() < 1 ? 1 : distance / nearest.getMeasuredWidth();
                        scaleRatio = scaleRatio > 1.2f ? 1.2f : scaleRatio;
                        nearest.setScaleX(scaleRatio);
                        nearest.setScaleY(scaleRatio);
                        ((CircleImageView) nearest).setFillColor(mContext.getResources().getColor(R.color.colorPrimary));
                         //TODO let the popWindow not flash
                        if (mPopTips.isShowing()) {
                            mPopTips.dismiss();
                        }else {
                            mPopTips.showAsDropDown(nearest, 0, -mChildSize * 2);
                        }
                        ((TextView) mPopTips.getContentView()).setText((String) nearest.getTag());
                        for (View view : mChildViews) {
                            if(view != nearest) {
                                ((CircleImageView) view).setFillColor(mContext.getResources().getColor(R.color.colorAccent));
                                view.setScaleX(1);
                                view.setScaleY(1);
                            }
                        }
                    }else {
                        mPopTips.dismiss();
                        for (View view : mChildViews) {
                            view.setScaleX(1);
                            view.setScaleY(1);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (PinterestView.this.getVisibility() == VISIBLE) {
                    mPopTips.dismiss();
                    View nearest = nearest(event.getX(),event.getY(),mChildViews);
                    if(nearest != null){
                        mPinMenuClickListener.onMenuItemClick(mChildViews.indexOf(nearest));
                    }
                    switchState();
                }
                break;
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        //Screen width and height
        setMeasuredDimension(mContext.getResources().getDisplayMetrics().widthPixels, mContext.getResources().getDisplayMetrics().heightPixels);

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int location[] = new int[2];
        getLocationOnScreen(location);
        mCenterY = mCenterY + location[1];

        final int childCount = getChildCount();
        //single degrees
        final float perDegrees = (mToDegrees - mFromDegrees)/(childCount - 1);

        float degrees = mFromDegrees;

        mChildViews.clear();
        for (int i = 0; i < getChildCount(); i++) {
            mChildViews.add(getChildAt(i));
        }

        //add centerView
        Rect centerRect = computeChildFrame(mCenterX, mCenterY, 0, perDegrees, mChildSize);
        getChildAt(0).layout(centerRect.left, centerRect.top, centerRect.right, centerRect.bottom);
        degrees += perDegrees;
        //add other view
        for (int i = 1; i < childCount; i++) {
            Rect frame = computeChildFrame(mCenterX, mCenterY, mRadius, degrees, mChildSize);
            if (i == 1) {
                Log.i("computeChildFrame:", frame + "");
            }
            degrees += perDegrees;
            getChildAt(i).layout(frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    private void createTipsPopWindow(Context context) {
        TextView tips = new TextView(context);
        tips.setTypeface(null, Typeface.BOLD);
        tips.setTextSize(15);
        tips.setTextColor(Color.parseColor("#ffffff"));
        tips.setBackgroundResource(R.drawable.shape_child_item);
        mPopTips = new PopupWindow(tips, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private void confirmDegreeRangeByCenter(float centerX, float centerY) {
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();

        //left-top (-60,90)
        Rect leftTopRect = new Rect(0, 0, dp2px(DEFAULT_RECT_RADIUS), dp2px(DEFAULT_RECT_RADIUS));

        //top (-10,140)
        Rect topRect = new Rect(dp2px(DEFAULT_RECT_RADIUS), 0, metrics.widthPixels - dp2px(DEFAULT_RECT_RADIUS), dp2px(DEFAULT_RECT_RADIUS));

        //right-top  50,200
        Rect rightTopRect = new Rect(metrics.widthPixels - dp2px(DEFAULT_RECT_RADIUS), 0, metrics.widthPixels, dp2px(DEFAULT_RECT_RADIUS));

        //left -100,50
        Rect leftRect = new Rect(0, dp2px(DEFAULT_RECT_RADIUS), dp2px(DEFAULT_RECT_RADIUS), metrics.heightPixels - dp2px(DEFAULT_RECT_RADIUS));

        //right 80,230
        Rect rightRect = new Rect(metrics.widthPixels - dp2px(DEFAULT_RECT_RADIUS), dp2px(DEFAULT_RECT_RADIUS), metrics.widthPixels, metrics.heightPixels - dp2px(DEFAULT_RECT_RADIUS));

        //left_bottom -140,10
        Rect leftBottomRect = new Rect(0, metrics.heightPixels - dp2px(DEFAULT_RECT_RADIUS), dp2px(DEFAULT_RECT_RADIUS), metrics.heightPixels);
        //bottom  170,320
        Rect bottomRect = new Rect(dp2px(DEFAULT_RECT_RADIUS), metrics.heightPixels - dp2px(DEFAULT_RECT_RADIUS), metrics.widthPixels - dp2px(DEFAULT_RECT_RADIUS), metrics.heightPixels);
        //right_bottom 150,300 and center
        Rect rightBottomRect = new Rect(metrics.widthPixels - dp2px(DEFAULT_RECT_RADIUS), metrics.heightPixels - dp2px(DEFAULT_RECT_RADIUS), metrics.widthPixels, metrics.heightPixels);
        Rect centerRect = new Rect(dp2px(DEFAULT_RECT_RADIUS), dp2px(DEFAULT_RECT_RADIUS), metrics.widthPixels - dp2px(DEFAULT_RECT_RADIUS), metrics.heightPixels - dp2px(DEFAULT_RECT_RADIUS));

        if (leftTopRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = -60;
            mToDegrees = 90;
        } else if (topRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = -10;
            mToDegrees = 150;
        } else if (rightTopRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = 50;
            mToDegrees = 200;
        } else if (leftRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = -100;
            mToDegrees = 50;
        } else if (rightRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = 80;
            mToDegrees = 230;
        } else if (leftBottomRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = -140;
            mToDegrees = 10;
        } else if (bottomRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = 170;
            mToDegrees = 320;
        } else if (rightBottomRect.contains((int) centerX, (int) centerY) || centerRect.contains((int) centerX, (int) centerY)) {
            mFromDegrees = 150;
            mToDegrees = 300;
        }
        requestLayout();

    }

    private Rect getChildDisPlayBounds(View view) {
        //scale the rect range
        Rect rect = new Rect();
        view.getHitRect(rect);
        return rect;
    }

    private void bindChildAnimation(final View child, final int position) {
        //in case when init in,child.getWidth = 0 cause get wrong rect
        child.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect childRect = getChildDisPlayBounds(child);
                if (mExpanded) {
                    expandAnimation(child, childRect);
                }
            }
        });
        Rect childRect = getChildDisPlayBounds(child);
        if (!mExpanded) {
            collapseAnimation(child, childRect);
        }
    }

    private void collapseAnimation(View child, Rect childRect) {
        AnimatorSet childAnim = new AnimatorSet();
        ObjectAnimator transX = ObjectAnimator.ofFloat(child, "translationX", 0, (mCenterX - childRect.exactCenterX()) / 2);
        ObjectAnimator transY = ObjectAnimator.ofFloat(child, "translationY", 0, (mCenterY - childRect.exactCenterY()) / 2);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(child, "alpha", 1, (int) 0.5);
        childAnim.playTogether(transX, transY, alpha);
        childAnim.setDuration(ANIMATION_DURATION);
        childAnim.setInterpolator(new AccelerateInterpolator());
        childAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
//                PinterestView.this.setVisibility(GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                recoverChildView();
                PinterestView.this.setVisibility(GONE);
            }
        });
        childAnim.start();
    }

    private void expandAnimation(View child, Rect childRect) {
        AnimatorSet childAnim = new AnimatorSet();
        ObjectAnimator transX = ObjectAnimator.ofFloat(child, "translationX", (mCenterX - childRect.exactCenterX()) / 2, 0);
        ObjectAnimator transY = ObjectAnimator.ofFloat(child, "translationY", (mCenterY - childRect.exactCenterY()) / 2, 0);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(child, "alpha", (int) 0.5, 1);
        childAnim.playTogether(transX, transY, alpha);
        childAnim.setDuration(ANIMATION_DURATION);
        childAnim.setInterpolator(new AccelerateInterpolator());
        childAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recoverChildView();
            }
        });
        childAnim.start();
    }


    /**
     * center view animation
     *
     * @param child
     */
    private void bindCenterViewAnimation(View child) {
        AnimatorSet childAnim;
        if (mExpanded) {
            childAnim = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(child, "scaleX", (int) 0.5, 1);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(child, "scaleY", (int) 0.5, 1);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(child, "alpha", (int) 0.5, 1);
            childAnim.playTogether(scaleX, scaleY, alpha);
            childAnim.setDuration(ANIMATION_DURATION);
            childAnim.setInterpolator(new AccelerateInterpolator());
            childAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    recoverChildView();
                }
            });
            childAnim.start();
        } else {
            childAnim = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(child, "scaleX", 1, (int) 0.5);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(child, "scaleY", 1, (int) 0.5);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(child, "alpha", 1, (int) 0.5);
            childAnim.playTogether(scaleX, scaleY, alpha);
            childAnim.setDuration(ANIMATION_DURATION);
            childAnim.setInterpolator(new AccelerateInterpolator());
            childAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    recoverChildView();
                    PinterestView.this.setVisibility(GONE);
                }
            });
            childAnim.start();
        }

    }

    private void recoverChildView() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).animate().setDuration(100).translationX(0).translationY(0).scaleX(1).scaleX(1).start();
        }
    }

    private static Rect computeChildFrame(final float centerX, final float centerY, final int radius, final float degrees,
                                          final int size) {

        final double childCenterX = centerX + radius * Math.cos(Math.toRadians(degrees));
        final double childCenterY = centerY + radius * Math.sin(Math.toRadians(degrees));

        return new Rect((int) (childCenterX - size / 2), (int) (childCenterY - size / 2),
                (int) (childCenterX + size / 2), (int) (childCenterY + size / 2));
    }


    public void switchState() {
        mExpanded = !mExpanded;
        final int childCount = getChildCount();
        //other view
        for (int i = 1; i < childCount; i++) {
            ((CircleImageView) getChildAt(i)).setFillColor(mContext.getResources().getColor(R.color.colorAccent));
            bindChildAnimation(getChildAt(i), i);
        }
        //center view
        bindCenterViewAnimation(getChildAt(0));

    }

    /**
     * addView to PinterestView
     *
     * @param size        view size
     * @param centerView
     * @param normalViews
     */
    public void addShowView(int size, View centerView, View... normalViews) {
        this.setChildSize(size);
        addView(centerView, 0);
        for (int i = 0; i < normalViews.length; i++) {
            addView(normalViews[i]);
        }
    }

    /**
     * size (dp)
     * default all item child size are same
     *
     * @param size //dp
     */
    public void setChildSize(int size) {
        if (mChildSize == size || size < 0) {
            return;
        }
        //convert to px
        mChildSize = dp2px(size);
        requestLayout();
    }


    private int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, getResources().getDisplayMetrics());
    }

    /**
     * set Pinterest click listener
     *
     * @param pinMenuClickListener callback
     */
    public void setPinClickListener(PinMenuClickListener pinMenuClickListener) {
        this.mPinMenuClickListener = pinMenuClickListener;
    }

    interface PinMenuClickListener {
        /**
         * PinterestView item click
         *
         * @param childAt position in PinterestView
         */
        void onMenuItemClick(int childAt);

        /**
         * preview(the view click to show pinterestview) click
         */
        void onPreViewClick();
    }
}