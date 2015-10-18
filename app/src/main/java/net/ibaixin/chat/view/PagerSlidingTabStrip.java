/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ibaixin.chat.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import java.util.Locale;

import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.EmojiTypeFragment.OnEmojiconBackspaceClickedListener;
import net.ibaixin.chat.model.EmojiType;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.EmojiPageIndicator.OnTabReselectedListener;
import net.ibaixin.chat.view.EmojiPageIndicator.RepeatListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PagerSlidingTabStrip extends HorizontalScrollView implements PageIndicator {
	
	/** Title text used when no title is provided by the adapter. */
    private static final CharSequence EMPTY_TITLE = "";
//
//	public interface IconTabProvider {
//		public int getPageIconResId(int position);
//	}

	// @formatter:off
	/*private static final int[] ATTRS = new int[] {
		android.R.attr.textSize,
		android.R.attr.textColor
    };*/
	// @formatter:on

	private LinearLayout.LayoutParams defaultTabLayoutParams;
	private LinearLayout.LayoutParams expandedTabLayoutParams;

	private OnPageChangeListener pageListener;
//	public OnPageChangeListener delegatePageListener;

	private final IcsLinearLayout mTabLayout;
	private ViewPager mViewPager;

	private int tabCount;

	private int currentPosition = 0;
	private int selectedPosition = 0;
	private float currentPositionOffset = 0f;

	private Paint rectPaint;
	private Paint dividerPaint;

	private int indicatorColor = 0xFF666666;
	private int underlineColor = 0x1A000000;
	private int dividerColor = 0x1A000000;

	private boolean shouldExpand = true;
	private boolean textAllCaps = true;

	private int scrollOffset = 52;
	private int indicatorHeight = 8;
	private int underlineHeight = 2;
	private int dividerPadding = 12;
	private int tabPadding = 0;
	private int dividerWidth = 1;

	private int tabTextSize = 12;
	private int tabTextColor = 0xFF666666;
	private int selectedTabTextColor = 0xFF666666;
	private Typeface tabTypeface = null;
	private int tabTypefaceStyle = Typeface.NORMAL;

	private int lastScrollX = 0;

	private int tabBackgroundResId = R.drawable.pager_tab_pressed_background;

	private Locale locale;
	private int mMaxTabWidth;
	
	private OnTabReselectedListener mTabReselectedListener;
	
	private Runnable mTabSelector;
	
	private SparseArray<EmojiType> typeMap = new SparseArray<>();
	
	/**
	 * 删除键的监听器
	 */
	private OnEmojiconBackspaceClickedListener mOnEmojiconBackspaceClickedListener;

	public PagerSlidingTabStrip(Context context) {
		this(context, null);
	}

	public PagerSlidingTabStrip(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PagerSlidingTabStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setFillViewport(true);
		setWillNotDraw(false);
		
		setHorizontalScrollBarEnabled(false);
		
		mTabLayout = new IcsLinearLayout(context, R.attr.vpiTabPageIndicatorStyle);
		mTabLayout.setOrientation(LinearLayout.HORIZONTAL);
		mTabLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		mTabLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.emojiType_bg));
		
		addView(mTabLayout);

		DisplayMetrics dm = getResources().getDisplayMetrics();

		scrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
		indicatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
		underlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
		dividerPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
		tabPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
		dividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
		tabTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);

		// get custom attrs

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip);

		indicatorColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsIndicatorColor, indicatorColor);
		underlineColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsUnderlineColor, underlineColor);
		dividerColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor, dividerColor);
		indicatorHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight, indicatorHeight);
		underlineHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight, underlineHeight);
		dividerPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerPadding, dividerPadding);
		tabPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight, tabPadding);
		tabBackgroundResId = a.getResourceId(R.styleable.PagerSlidingTabStrip_pstsTabBackground, tabBackgroundResId);
		shouldExpand = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand, shouldExpand);
		scrollOffset = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsScrollOffset, scrollOffset);
		textAllCaps = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, textAllCaps);
		selectedTabTextColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsSelectedTabTextColor, selectedTabTextColor);
		tabTextColor = a.getColor(R.styleable.PagerSlidingTabStrip_android_textColor, tabTextColor);
		tabTextSize = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_android_textSize, tabTextSize);

		a.recycle();

		rectPaint = new Paint();
		rectPaint.setAntiAlias(true);
		rectPaint.setStyle(Style.FILL);

		dividerPaint = new Paint();
		dividerPaint.setAntiAlias(true);
		dividerPaint.setStrokeWidth(dividerWidth);

		defaultTabLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		expandedTabLayoutParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);

		if (locale == null) {
			locale = getResources().getConfiguration().locale;
		}
	}
	
    /**
     * 根据索引位置判断指示器类型是否是表情
     * @update 2015年1月26日 下午3:10:47
     * @param position
     * @return
     */
    public boolean isEmoji(int position) {
    	if (typeMap.indexOfKey(position) >= 0) {
    		EmojiType emojiType = typeMap.get(position);
    		if (emojiType != null) {
    			return emojiType.getOptType() == EmojiType.OPT_EMOJI;
    		}
    	}
    	return false;
    }
    
    /**
     * 根据索引位置获得指示器类型 
     * @update 2015年1月26日 下午3:17:19
     * @param position
     * @return
     */
    public EmojiType getEmojiType(int position) {
    	if (typeMap.indexOfKey(position) >= 0) {
    		EmojiType emojiType = typeMap.get(position);
    		if (emojiType != null) {
    			return emojiType;
    		}
    	}
    	return null;
    }

	public void setViewPager(ViewPager viewPager) {
		if (mViewPager == viewPager) {
            return;
        }
        if (mViewPager != null) {
            mViewPager.setOnPageChangeListener(null);
        }
        final PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        
        this.mViewPager = viewPager;

//        viewPager.setOnPageChangeListener(pageListener);
        viewPager.setOnPageChangeListener(this);

		notifyDataSetChanged();
	}
	
	public void setOnTabReselectedListener(OnTabReselectedListener listener) {
        mTabReselectedListener = listener;
    }
	
	private final OnClickListener mTabClickListener = new OnClickListener() {
        public void onClick(View view) {
            TabView tabView = (TabView)view;
            final int oldSelected = mViewPager.getCurrentItem();
            final int newSelected = tabView.getIndex();
            EmojiType emojiType = typeMap.valueAt(newSelected);
            if (emojiType != null) {
            	switch (emojiType.getOptType()) {
				case EmojiType.OPT_EMOJI:	//表情类型
					mViewPager.setCurrentItem(newSelected);
					if (oldSelected == newSelected && mTabReselectedListener != null) {
						mTabReselectedListener.onTabReselected(newSelected);
					}
					break;
				case EmojiType.OPT_DEL:	//删除表情
					if (mOnEmojiconBackspaceClickedListener != null) {
						mOnEmojiconBackspaceClickedListener.onEmojiconBackspaceClicked(view);
					}
					break;
				default:
					break;
				}
            }
        }
    };
    
    public void setOnEmojiconBackspaceClickedListener(
			OnEmojiconBackspaceClickedListener onEmojiconBackspaceClickedListener) {
		this.mOnEmojiconBackspaceClickedListener = onEmojiconBackspaceClickedListener;
	}

	public void setOnPageChangeListener(OnPageChangeListener listener) {
		this.pageListener = listener;
	}

	public void notifyDataSetChanged() {

		mTabLayout.removeAllViews();

		PagerAdapter adapter = mViewPager.getAdapter();
		PagerAdapterProvider pagerAdapter = null;
		
		tabCount = adapter.getCount();
		int totalCount = tabCount;
		if (adapter instanceof PagerAdapterProvider) {
			pagerAdapter = (PagerAdapterProvider) adapter;
			final int extraCount = pagerAdapter.getExtraCount();
	        totalCount = tabCount + extraCount;
		}

		for (int i = 0; i < totalCount; i++) {

			if (adapter instanceof EmojiIconPagerAdapterProvider) {
				addEmojiIconTab(i, ((EmojiIconPagerAdapterProvider) adapter).getEmojiType(i));
			} else if (adapter instanceof IconPagerAdapterProvider) {
				addIconTab(i, ((IconPagerAdapterProvider) adapter).getIconResId(i));
			} else {
				CharSequence title = adapter.getPageTitle(i);
				if (title == null) {
					title = EMPTY_TITLE;
				}
				addTextTab(i, title);
			}

		}
		
		updateTabStyles();
		
		getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@SuppressWarnings("deprecation")
			@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
			@Override
			public void onGlobalLayout() {
				if(SystemUtil.hasSDK16()) {
					getViewTreeObserver().removeOnGlobalLayoutListener(this);
				} else {
					getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				currentPosition = mViewPager.getCurrentItem();
				scrollToChild(currentPosition, 0);
			}
		});

	}

	/**
	 * 添加表情的tab
	 * @update 2015年1月28日 下午9:56:31
	 * @param position
	 * @param emojiType
	 */
	private void addEmojiIconTab(final int position, EmojiType emojiType) {
		typeMap.put(position, emojiType);
        final TabView tabView = new TabView(getContext());
        tabView.mIndex = position;
        tabView.setFocusable(true);
        tabView.setImageResource(emojiType.getResId());
        if (emojiType != null && EmojiType.OPT_DEL == emojiType.getOptType()) {	//删除
        	tabView.setOnTouchListener(new RepeatListener(1000, 50, mTabClickListener));
        } else {
        	tabView.setOnClickListener(mTabClickListener);
        }
        if (!TextUtils.isEmpty(emojiType.getDescription())) {
        	tabView.setContentDescription(emojiType.getDescription());
        }
        mTabLayout.addView(tabView, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
	}

	/**
	 * 添加文本的tab
	 * @update 2015年1月28日 下午9:58:54
	 * @param position
	 * @param title
	 */
	private void addTextTab(final int position, CharSequence title) {
		//最外层的layout容器
		RelativeLayout tabLayout = new RelativeLayout(getContext());
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		tabLayout.setLayoutParams(layoutParams);
		
		RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		textParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		//tab的文字控件
		TextView tab = new TextView(getContext());
		tab.setId(100 + position);
		tab.setText(title);
		tab.setGravity(Gravity.CENTER);
		tab.setSingleLine();
		tabLayout.addView(tab, textParams);
		
		//用于未读信息提醒的控件
		RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		viewParams.addRule(RelativeLayout.RIGHT_OF, tab.getId());
		viewParams.addRule(RelativeLayout.CENTER_VERTICAL);
		View view  = new View(getContext());
		ViewGroup.LayoutParams vParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(vParams);
		tabLayout.addView(view, viewParams);
		
		addTab(position, tabLayout);
		
	}

	private void addIconTab(final int position, int resId) {

		//最外层的layout容器
		RelativeLayout tabLayout = new RelativeLayout(getContext());
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		tabLayout.setLayoutParams(layoutParams);
		tabLayout.setGravity(Gravity.CENTER);
		
		//tab的图片控件
		RelativeLayout.LayoutParams imgParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		imgParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		ImageButton tab = new ImageButton(getContext());
		tab.setId(100 + position);
		tab.setImageResource(resId);
		tabLayout.addView(tab, imgParams);
		
		//用于未读信息提醒的控件
		RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		viewParams.addRule(RelativeLayout.RIGHT_OF, tab.getId());
		View view  = new View(getContext());
		ViewGroup.LayoutParams vParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(vParams);
		tabLayout.addView(view, viewParams);
		
		addTab(position, tabLayout);
		
		/*BadgeView badgeView = new BadgeView(getContext(), view);
        badgeView.setText("");
        badgeView.setTextSize(10);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setBackgroundResource(R.drawable.main_tab_new_message_notify);
        badgeView.setBadgePosition(BadgeView.POSITION_VERTICAL_LEFT);
        badgeView.show();*/
	}

	private void addTab(final int position, View tab) {
		tab.setFocusable(true);
		tab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mViewPager.setCurrentItem(position);
			}
		});

		tab.setPadding(tabPadding, 0, tabPadding, 0);
		mTabLayout.addView(tab, position, shouldExpand ? expandedTabLayoutParams : defaultTabLayoutParams);
	}
	
	private class TabView extends ImageView {
        private int mIndex;

        public TabView(Context context) {
            this(context, null);
        }

		public TabView(Context context, AttributeSet attrs) {
			super(context, attrs, R.attr.vpiTabPageIndicatorStyle);
		}

		@Override
        public boolean performClick() {
        	// TODO Auto-generated method stub
        	return super.performClick();
        }
        
        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Re-measure if we went beyond our maximum size.
            if (mMaxTabWidth > 0 && getMeasuredWidth() > mMaxTabWidth) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(mMaxTabWidth, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            }
        }

        public int getIndex() {
            return mIndex;
        }
    }
	
	/**
	 * 根据索引获得当前的tabView
	 * @param position 当前页面的索引，从0开始
	 * @return 指示器的view
	 */
	public View getCurrentTab(int position) {
		return mTabLayout.getChildAt(position);
	}
	
	/**
	 * 根据索引获得当前的指示器的未读信息提醒的view
	 * @param position 当前页面的索引，从0开始
	 * @return 指示器的未读信息提醒的view
	 */
	public View getCurrentTargetView(int position) {
		ViewGroup viewGroup = (ViewGroup) mTabLayout.getChildAt(position);
		View view = viewGroup.getChildAt(1);
		return view;
	}

	private void updateTabStyles() {
		
		for (int i = 0; i < mTabLayout.getChildCount(); i++) {
			View child = mTabLayout.getChildAt(i);
			if (tabBackgroundResId != 0) {
				child.setBackgroundResource(tabBackgroundResId);
			}
            final boolean isSelected = (i == selectedPosition);
            child.setSelected(isSelected);
			if (child instanceof TextView) {

				TextView tab = (TextView) child;
				tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
				tab.setTypeface(tabTypeface, tabTypefaceStyle);
				tab.setTextColor(tabTextColor);

				// setAllCaps() is only available from API 14, so the upper case is made manually if we are on a
				// pre-ICS-build
				if (textAllCaps) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						tab.setAllCaps(true);
					} else {
						tab.setText(tab.getText().toString().toUpperCase(locale));
					}
				}
				if (i == selectedPosition) {
					tab.setTextColor(selectedTabTextColor);
				}
			}
		}

	}

	private void scrollToChild(int position, int offset) {

		if (tabCount == 0) {
			return;
		}

		int newScrollX = mTabLayout.getChildAt(position).getLeft() + offset;

		if (position > 0 || offset > 0) {
			newScrollX -= scrollOffset;
		}

		if (newScrollX != lastScrollX) {
			lastScrollX = newScrollX;
			scrollTo(newScrollX, 0);
		}

	}
	
	private void animateToTab(final int position) {
        final View tabView = mTabLayout.getChildAt(position);
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
        }
        mTabSelector = new Runnable() {
            public void run() {
                final int scrollPos = tabView.getLeft() - (getWidth() - tabView.getWidth()) / 2;
                smoothScrollTo(scrollPos, 0);
                mTabSelector = null;
            }
        };
        post(mTabSelector);
    }

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (isInEditMode() || tabCount == 0) {
			return;
		}

		final int height = getHeight();
		
		int defaultColor = rectPaint.getColor();
		
		// draw underline
		rectPaint.setColor(underlineColor);
		canvas.drawRect(0, height - underlineHeight, mTabLayout.getWidth(), height, rectPaint);

		// draw indicator line
		rectPaint.setColor(indicatorColor);

		// default: line below current tab
		View currentTab = mTabLayout.getChildAt(currentPosition);
		float lineLeft = currentTab.getLeft();
		float lineRight = currentTab.getRight();

		// if there is an offset, start interpolating left and right coordinates between current and next tab
		if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {

			View nextTab = mTabLayout.getChildAt(currentPosition + 1);
			final float nextTabLeft = nextTab.getLeft();
			final float nextTabRight = nextTab.getRight();

			lineLeft = (currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft);
			lineRight = (currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight);
		}

		canvas.drawRect(lineLeft, height - indicatorHeight, lineRight, height, rectPaint);

		rectPaint.setColor(defaultColor);		
		
		// draw divider

		dividerPaint.setColor(dividerColor);
		for (int i = 0; i < tabCount - 1; i++) {
			View tab = mTabLayout.getChildAt(i);
			canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint);
		}
	}

	/*private class PageListener implements OnPageChangeListener {

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			currentPosition = position;
			currentPositionOffset = positionOffset;

			scrollToChild(position, (int) (positionOffset * mTabLayout.getChildAt(position).getWidth()));

			invalidate();

			if (delegatePageListener != null) {
				delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			if (state == ViewPager.SCROLL_STATE_IDLE) {
				scrollToChild(mViewPager.getCurrentItem(), 0);
			}

			if (delegatePageListener != null) {
				delegatePageListener.onPageScrollStateChanged(state);
			}
		}

		@Override
		public void onPageSelected(int position) {
			selectedPosition = position;
			updateTabStyles();
			if (delegatePageListener != null) {
				delegatePageListener.onPageSelected(position);
			}
		}

	}*/

	public void setIndicatorColor(int indicatorColor) {
		this.indicatorColor = indicatorColor;
		invalidate();
	}

	public void setIndicatorColorResource(int resId) {
		this.indicatorColor = getResources().getColor(resId);
		invalidate();
	}

	public int getIndicatorColor() {
		return this.indicatorColor;
	}

	public void setIndicatorHeight(int indicatorLineHeightPx) {
		this.indicatorHeight = indicatorLineHeightPx;
		invalidate();
	}

	public int getIndicatorHeight() {
		return indicatorHeight;
	}

	public void setUnderlineColor(int underlineColor) {
		this.underlineColor = underlineColor;
		invalidate();
	}

	public void setUnderlineColorResource(int resId) {
		this.underlineColor = getResources().getColor(resId);
		invalidate();
	}

	public int getUnderlineColor() {
		return underlineColor;
	}

	public void setDividerColor(int dividerColor) {
		this.dividerColor = dividerColor;
		invalidate();
	}

	public void setDividerColorResource(int resId) {
		this.dividerColor = getResources().getColor(resId);
		invalidate();
	}

	public int getDividerColor() {
		return dividerColor;
	}

	public void setUnderlineHeight(int underlineHeightPx) {
		this.underlineHeight = underlineHeightPx;
		invalidate();
	}

	public int getUnderlineHeight() {
		return underlineHeight;
	}

	public void setDividerPadding(int dividerPaddingPx) {
		this.dividerPadding = dividerPaddingPx;
		invalidate();
	}

	public int getDividerPadding() {
		return dividerPadding;
	}

	public void setScrollOffset(int scrollOffsetPx) {
		this.scrollOffset = scrollOffsetPx;
		invalidate();
	}

	public int getScrollOffset() {
		return scrollOffset;
	}

	public void setShouldExpand(boolean shouldExpand) {
		this.shouldExpand = shouldExpand;
		notifyDataSetChanged();
	}

	public boolean getShouldExpand() {
		return shouldExpand;
	}

	public boolean isTextAllCaps() {
		return textAllCaps;
	}

	public void setAllCaps(boolean textAllCaps) {
		this.textAllCaps = textAllCaps;
	}

	public void setTextSize(int textSizePx) {
		this.tabTextSize = textSizePx;
		updateTabStyles();
	}
	
	public int getTextSize() {
		return tabTextSize;
	}

	public void setTextColor(int textColor) {
		this.tabTextColor = textColor;
		updateTabStyles();
	}
	
	public void setTextColorResource(int resId) {
		this.tabTextColor = getResources().getColor(resId);
		updateTabStyles();
	}

	public int getTextColor() {
		return tabTextColor;
	}
	
	public void setSelectedTextColor(int textColor) {
		this.selectedTabTextColor = textColor;
		updateTabStyles();
	}
	
	public void setSelectedTextColorResource(int resId) {
		this.selectedTabTextColor = getResources().getColor(resId);
		updateTabStyles();
	}

	public int getSelectedTextColor() {
		return selectedTabTextColor;
	}

	public void setTypeface(Typeface typeface, int style) {
		this.tabTypeface = typeface;
		this.tabTypefaceStyle = style;
		updateTabStyles();
	}

	public void setTabBackground(int resId) {
		this.tabBackgroundResId = resId;
		updateTabStyles();
	}

	public int getTabBackground() {
		return tabBackgroundResId;
	}

	public void setTabPaddingLeftRight(int paddingPx) {
		this.tabPadding = paddingPx;
		updateTabStyles();
	}

	public int getTabPaddingLeftRight() {
		return tabPadding;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		currentPosition = savedState.currentPosition;
		requestLayout();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.currentPosition = currentPosition;
		return savedState;
	}

	static class SavedState extends BaseSavedState {
		int currentPosition;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			currentPosition = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(currentPosition);
		}

		public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
		currentPosition = position;
		currentPositionOffset = positionOffset;

		scrollToChild(position, (int) (positionOffset * mTabLayout.getChildAt(position).getWidth()));

		invalidate();

		if (pageListener != null) {
			pageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
		}
	}

	@Override
	public void onPageSelected(int position) {
		selectedPosition = position;
		updateTabStyles();
		if (pageListener != null) {
			pageListener.onPageSelected(position);
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		if (state == ViewPager.SCROLL_STATE_IDLE) {
			scrollToChild(mViewPager.getCurrentItem(), 0);
		}

		if (pageListener != null) {
			pageListener.onPageScrollStateChanged(state);
		}
	}

	@Override
	public void setViewPager(ViewPager view, int initialPosition) {
		setViewPager(view);
        setCurrentItem(initialPosition);
	}

	@Override
	public void setCurrentItem(int item) {
		if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        selectedPosition = item;
        mViewPager.setCurrentItem(item);

        for (int i = 0; i < tabCount; i++) {
            final View child = mTabLayout.getChildAt(i);
            final boolean isSelected = (i == item);
            child.setSelected(isSelected);
            if (isSelected) {
                animateToTab(item);
//            	scrollToChild(item, offset);
            }
        }
	}

}
