package net.ibaixin.chat.fragment;

import java.util.ArrayList;
import java.util.List;

import net.ibaixin.chat.R;
import net.ibaixin.chat.model.EmojiType;
import net.ibaixin.chat.model.emoji.Emojicon;
import net.ibaixin.chat.model.emoji.EmojiconRecents;
import net.ibaixin.chat.model.emoji.EmojiconRecentsManager;
import net.ibaixin.chat.model.emoji.Nature;
import net.ibaixin.chat.model.emoji.Objects;
import net.ibaixin.chat.model.emoji.People;
import net.ibaixin.chat.model.emoji.Places;
import net.ibaixin.chat.model.emoji.Symbols;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.view.EmojiIconPagerAdapterProvider;
import net.ibaixin.chat.view.PagerSlidingTabStrip;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 表情分类界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年1月26日 下午3:54:56
 */
public class EmojiTypeFragment extends BaseFragment implements EmojiconRecents {
	private OnEmojiconBackspaceClickedListener mOnEmojiconBackspaceClickedListener;
	private ViewPager mViewPager;
//	private EmojiPageIndicator mPageIndicator;
	private PagerSlidingTabStrip mPageIndicator;
	
	static EmojiType[] emojiCategories = null;
	
	static int[] emojiIconRes = {
		R.drawable.ic_emoji_recent_light,
		R.drawable.ic_emoji_people_light,
		R.drawable.ic_emoji_nature_light,
		R.drawable.ic_emoji_objects_light,
		R.drawable.ic_emoji_places_light,
		R.drawable.ic_emoji_symbols_light
	};
	
	static int[] emojiTypes = {
		0,
		People.EMOJI_TYPE,
		Nature.EMOJI_TYPE,
		Objects.EMOJI_TYPE,
		Places.EMOJI_TYPE,
		Symbols.EMOJI_TYPE
	};
	
	static String[] emojiIconDesc = null;
	
	static int[] emojiExtraRes = {
		R.drawable.sym_keyboard_delete_holo
	};
	
	private List<Fragment> fragments = new ArrayList<>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		emojiIconDesc = getResources().getStringArray(R.array.emoji_type_desc);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_emoji_type, container, false);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
//		mPageIndicator = (EmojiPageIndicator) view.findViewById(R.id.page_indicator);
		mPageIndicator = (PagerSlidingTabStrip) view.findViewById(R.id.page_indicator);
		
		initEmojiType();
		
		if (mOnEmojiconBackspaceClickedListener != null) {
			setOnEmojiconBackspaceClickedListener(mOnEmojiconBackspaceClickedListener);
		}
		
		final EmojiTypeAdapter adapter = new EmojiTypeAdapter(getChildFragmentManager(), fragments);
		mViewPager.setAdapter(adapter);
		mPageIndicator.setViewPager(mViewPager);
		/*mPageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				if (position == 0) {	//最近浏览的记录
					EmojiFragment1 fragment = (EmojiFragment1) adapter.getItem(0);
					EmojiconRecentsManager recentsManager = EmojiconRecentsManager.getInstance(getActivity());
					fragment.notifyDataSetChanged(recentsManager);
				}
			}
			
			@Override
			public void onPageScrolled(int position, float positionOffset,
					int positionOffsetPixels) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
				// TODO Auto-generated method stub
				
			}
		});*/
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (getActivity() instanceof OnEmojiconBackspaceClickedListener) {
			mOnEmojiconBackspaceClickedListener = (OnEmojiconBackspaceClickedListener) getActivity();
		} else if (getParentFragment() instanceof OnEmojiconBackspaceClickedListener) {
			mOnEmojiconBackspaceClickedListener = (OnEmojiconBackspaceClickedListener) getParentFragment();
		} else {
			throw new IllegalArgumentException(activity + " must implement interface " + OnEmojiconBackspaceClickedListener.class.getSimpleName());
		}
	}
	
	@Override
	public void onDetach() {
		mOnEmojiconBackspaceClickedListener = null;
		super.onDetach();
	}
	
	public void setOnEmojiconBackspaceClickedListener (
			OnEmojiconBackspaceClickedListener onEmojiconBackspaceClickedListener) {
		mPageIndicator.setOnEmojiconBackspaceClickedListener(onEmojiconBackspaceClickedListener);
	}
	
	public static void input(EditText editText, Emojicon emojicon) {
        if (editText == null || emojicon == null) {
            return;
        }

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0) {
            editText.append(emojicon.getEmoji());
        } else {
            editText.getText().replace(Math.min(start, end), Math.max(start, end), emojicon.getEmoji(), 0, emojicon.getEmoji().length());
        }
    }
	
	/**
	 * 删除字符
	 * @update 2015年1月29日 下午9:04:24
	 * @param editText
	 */
	public static void backspace(EditText editText) {
        KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        editText.dispatchKeyEvent(event);
    }
	
	/**
	 * 初始化表情分类
	 * @update 2015年1月26日 下午4:49:15
	 */
	private void initEmojiType() {
		int totalCount = emojiIconRes.length + emojiExtraRes.length;
		emojiCategories = new EmojiType[totalCount];
		for (int i = 0; i < emojiIconRes.length; i++) {
			int opt = EmojiType.OPT_EMOJI;
			EmojiType emojiType = new EmojiType(emojiIconRes[i], null, emojiIconDesc[i], opt, emojiTypes[i]);
			emojiCategories[i] = emojiType;
			
//			Fragment fragment = EmojiGridFragment.newInstance("表情面板" + i);
			Fragment fragment = EmojiFragment.newInstance(emojiTypes[i]);
//			args.putParcelable(EmojiFragment.ARG_EMOJI_TYPE, emojiType);
			fragments.add(fragment);
		}
		for (int i = 0; i < emojiExtraRes.length; i++) {
			EmojiType emojiType = new EmojiType(emojiExtraRes[i], null, null, EmojiType.OPT_DEL, -1);
			emojiCategories[emojiIconRes.length + i] = emojiType;
		}
	}
	
	/**
	 * 表情分类的适配器
	 * @author huanghui1
	 * @update 2015年1月29日 下午7:58:14
	 */
	class EmojiTypeAdapter extends FragmentStatePagerAdapter implements EmojiIconPagerAdapterProvider {
		private List<Fragment> fragments;

		public EmojiTypeAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public int getIconResId(int index) {
			return emojiCategories[index].getResId();
		}

		@Override
		public EmojiType getEmojiType(int index) {
			return emojiCategories[index];
		}

		@Override
		public int getExtraCount() {
			return emojiExtraRes.length;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return emojiIconRes.length;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			return emojiCategories[position].getDescription();
		}
		
	}
	
	/**
	 * 删除键
	 * @author huanghui1
	 * @update 2015年1月29日 下午8:46:21
	 */
	public interface OnEmojiconBackspaceClickedListener {
        void onEmojiconBackspaceClicked(View v);
    }

	@Override
	public void addRecentEmoji(Context context, Emojicon emojicon) {
		EmojiFragment fragment = (EmojiFragment) fragments.get(0);
		if (fragment != null) {
			fragment.putRecentEmoji(fragment, context, emojicon);
		}
	}

}
