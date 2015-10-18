package net.ibaixin.chat.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.CommonAdapter;
import net.ibaixin.chat.model.emoji.Emojicon;
import net.ibaixin.chat.model.emoji.EmojiconRecents;
import net.ibaixin.chat.model.emoji.EmojiconRecentsManager;
import net.ibaixin.chat.model.emoji.Nature;
import net.ibaixin.chat.model.emoji.Objects;
import net.ibaixin.chat.model.emoji.People;
import net.ibaixin.chat.model.emoji.Places;
import net.ibaixin.chat.model.emoji.Symbols;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.CirclePageIndicator;

import org.jivesoftware.smack.ReconnectionManager;

import android.R.menu;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

/**
 * 表情界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月27日 下午8:16:15
 */
public class EmojiFragment extends BaseFragment implements EmojiconRecents {
	public static final String ARG_EMOJI_TYPE = "arg_emoji_type";
	public static final String ARG_EMOJIS = "arg_emojis";
	public static final String ARGS_USE_SYSTEM_DEFAULT_KEY = "useSystemDefaults";
	
	private ViewPager mViewPager;
	private CirclePageIndicator mIndicator;
	
	private EmojiPagerAdapter mEmojiPageAdapter;
	/**
	 * 表情分页的集合
	 */
	Map<Integer, ArrayList<Emojicon>> mEmojiMap;
	
	private int mEmojiType;
	
	private TextView mTvPrompt;
	
	/**
	 * 表情数组
	 */
	private Emojicon[] mData;
	
	/**
	 * 消息编辑输入框，在activty中
	 */
	private EditText mEtContent;
	private boolean mUseSystemDefault;
	
//	EmojiconRecentsManager recents;
	
	private EmojiconRecents recents;
	
	public static EmojiFragment newInstance(int emojiType) {
		return newInstance(emojiType, false);
	}
	
	public static EmojiFragment newInstance(int emojiType, boolean useSystemDefault) {
		EmojiFragment fragment = new EmojiFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_EMOJI_TYPE, emojiType);
		args.putBoolean(ARGS_USE_SYSTEM_DEFAULT_KEY, useSystemDefault);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if (activity instanceof EmojiconRecents) {
			recents = (EmojiconRecents) activity;
		} else if (getParentFragment() instanceof EmojiconRecents) {
			recents = (EmojiconRecents) getParentFragment();
		} else {
			throw new IllegalArgumentException(activity + " must implement interface " + EmojiconRecents.class.getSimpleName());
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_emoji, container, false);
		
		mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
		mIndicator = (CirclePageIndicator) view.findViewById(R.id.indicator);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);
		//查询最近使用过的表情
		EmojiconRecentsManager recentsManager = EmojiconRecentsManager
	            .getInstance(view.getContext());
		
		mEtContent = (EditText) getActivity().findViewById(R.id.et_content);
		Bundle args = getArguments();
		if (args == null) {
			mData = People.DATA;
			mUseSystemDefault = false;
		} else {
			mEmojiType = args.getInt(EmojiFragment.ARG_EMOJI_TYPE, People.EMOJI_TYPE);
			mData = (Emojicon[]) args.getParcelableArray(ARG_EMOJIS);
			if (mData == null) {
				switch (mEmojiType) {
				case 0:	//最近使用过的表情
					if (!SystemUtil.isEmpty(recentsManager)) {
						mData = recentsManager.toArray(new Emojicon[recentsManager.size()]);
					} else {
						mData = new Emojicon[0];
					}
					break;
				case People.EMOJI_TYPE:
					mData = People.DATA;
					break;
				case Nature.EMOJI_TYPE:
					mData = Nature.DATA;
					break;
				case Objects.EMOJI_TYPE:
					mData = Objects.DATA;
					break;
				case Places.EMOJI_TYPE:
					mData = Places.DATA;
					break;
				case Symbols.EMOJI_TYPE:
					mData = Symbols.DATA;
					break;
				default:
					mData = new Emojicon[0];
					break;
				}
			}
			mUseSystemDefault = args.getBoolean(ARGS_USE_SYSTEM_DEFAULT_KEY, false);
		}
		
		initEmojiPageData();
		
		mEmojiPageAdapter = new EmojiPagerAdapter(getChildFragmentManager(), mEmojiMap);
		mViewPager.setAdapter(mEmojiPageAdapter);
		
		mIndicator.setViewPager(mViewPager);
	}
	
	public ViewPager getViewPager() {
		return mViewPager;
	}

	public EmojiPagerAdapter getEmojiPageAdapter() {
		return mEmojiPageAdapter;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		
		outState.putParcelableArray(ARG_EMOJIS, mData);
	}
	
	/**
	 * 初始化表情的个页面
	 * @update 2014年10月27日 下午2:53:04
	 */
	private void initEmojiPageData() {
		mEmojiMap = new HashMap<>();
		int pageCount = SystemUtil.getEmojiPageCount(mData.length);
		int i = 0;
		do {
			ArrayList<Emojicon> data = SystemUtil.getCurrentPageEmojis(mData, i);
			mEmojiMap.put(i, data);
			i ++;
		} while (i < pageCount);
	}
	
	/**
	 * 表情分页的适配器
	 * @author huanghui1
	 * @update 2014年10月27日 下午8:32:37
	 */
	class EmojiPagerAdapter extends FragmentStatePagerAdapter {
		
		private Map<Integer, ArrayList<Emojicon>> emojiMap;

		public EmojiPagerAdapter(FragmentManager fm, Map<Integer, ArrayList<Emojicon>> emojiMap) {
			super(fm);
			this.emojiMap = emojiMap;
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			if (mEmojiType == 0) {
				fragment = EmojiRecentGridFragment.newInstance(mEmojiType, emojiMap.get(position));
			} else {
				fragment = EmojiGridFragment.newInstance(mEmojiType, emojiMap.get(position), recents, false);
			}
			return fragment;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// TODO Auto-generated method stub
			Fragment fragment = (Fragment) super.instantiateItem(container, position);
			if (mEmojiType == 0) {
				((EmojiRecentGridFragment) fragment).setRecents(null);
				EmojiconRecentsManager recentsManager = EmojiconRecentsManager.getInstance(mContext);
				Bundle args = fragment.getArguments();
				if (args != null) {
					args.putParcelableArrayList(EmojiGridFragment.ARG_EMOJI_DATA, recentsManager);
				}
			} else {
				((EmojiGridFragment) fragment).setRecents(recents);
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return emojiMap.size();
		}
	}
	
	/**
	 * 表情网格点击的回调
	 * @author huanghui1
	 * @update 2015年1月26日 下午9:57:23
	 */
	public interface OnEmojiconClickedListener {
        void onEmojiconClicked(Emojicon emojicon);
    }

	@Override
	public void addRecentEmoji(Context context, Emojicon emojicon) {
		/*final ViewPager emojisPager = (ViewPager) getView().findViewById(R.id.view_pager);
		EmojiRecentGridFragment fragment = (EmojiRecentGridFragment) mEmojiPageAdapter.instantiateItem(emojisPager, 0);
        fragment.addRecentEmoji(context, emojicon);*/
		if (recents != null) {
			recents.addRecentEmoji(context, emojicon);
		}
	}

	public void putRecentEmoji(EmojiFragment emojiFragment, Context context, Emojicon emojicon) {
		// TODO Auto-generated method stub
		ViewPager viewPager = emojiFragment.getViewPager();
		FragmentStatePagerAdapter pagerAdapter = emojiFragment.getEmojiPageAdapter();
		if (viewPager != null && pagerAdapter != null) {
			Fragment fragment = (Fragment) pagerAdapter.instantiateItem(viewPager, 0);
			if (fragment instanceof EmojiRecentGridFragment) {
				((EmojiRecentGridFragment) fragment).addRecentEmoji(context, emojicon);
			}
		}
	}
}
