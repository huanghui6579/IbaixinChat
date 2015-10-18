package net.ibaixin.chat.fragment;

import java.util.ArrayList;
import java.util.List;

import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.CommonAdapter;
import net.ibaixin.chat.fragment.EmojiFragment.OnEmojiconClickedListener;
import net.ibaixin.chat.model.emoji.Emojicon;
import net.ibaixin.chat.model.emoji.EmojiconRecents;
import net.ibaixin.chat.model.emoji.EmojiconRecentsManager;
import net.ibaixin.chat.model.emoji.People;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.EmojiconTextView;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

/**
 * 显示一页表情 
 * @author huanghui1
 * @update 2015年1月29日 下午9:34:26
 */
public class EmojiGridFragment extends BaseFragment implements AdapterView.OnItemClickListener {
	public static final String ARG_EMOJI_DATA = "arg_emoji_data";
	
	
	protected OnEmojiconClickedListener mOnEmojiconClickedListener;
	
	protected EmojiconRecents recents;
	protected int mEmojiType = 0;
	protected boolean mUseSystemDefault;
	
	protected EmojiAdapter mAdapter;
	
	protected List<Emojicon> data = new ArrayList<>();
	
	protected static EmojiGridFragment newInstance(int emojiType, ArrayList<Emojicon> list, EmojiconRecents recents, boolean useSystemDefault) {
		EmojiGridFragment fragment = new EmojiGridFragment();
		Bundle args = new Bundle();
		args.putInt(EmojiFragment.ARG_EMOJI_TYPE, emojiType);
		args.putParcelableArrayList(ARG_EMOJI_DATA, list);
		args.putBoolean(EmojiFragment.ARGS_USE_SYSTEM_DEFAULT_KEY, useSystemDefault);
		if (recents != null) {
			fragment.setRecents(recents);
		}
		fragment.setArguments(args);
		return fragment;
	}
	
	protected static EmojiGridFragment newInstance(int emojiType, ArrayList<Emojicon> list) {
		return newInstance(emojiType, list, null, false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		if (args != null) {
			mEmojiType = args.getInt(EmojiFragment.ARG_EMOJI_TYPE, People.EMOJI_TYPE); 
			mUseSystemDefault = args.getBoolean(EmojiFragment.ARGS_USE_SYSTEM_DEFAULT_KEY, false);
			List<Emojicon> temp = args.getParcelableArrayList(ARG_EMOJI_DATA);
			if (temp != null && temp.size() > 0) {
				data.addAll(temp);
			}
		}
	}
	
	public void setRecents(EmojiconRecents recents) {
		this.recents = recents;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_emoji_grid, null);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		
		GridView gridView = (GridView) view.findViewById(R.id.gv_emoji);
		TextView emptyView = (TextView) view.findViewById(R.id.empty_view);
//			List<Emoji> list = ChatApplication.getCurrentPageEmojis(i);
		mAdapter = new EmojiAdapter(data, mContext);
		mAdapter.notifyDataSetChanged();
		gridView.setAdapter(mAdapter);
		
		Drawable drawable= getResources().getDrawable(R.drawable.ic_no_emoji_history_dark);
		/// 这一步必须要做,否则不会显示.
		drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
		emptyView.setCompoundDrawables(null, drawable, null, null);
		gridView.setEmptyView(emptyView);
		
		gridView.setOnItemClickListener(this);
	}
	
	/**
	 * 刷新数据
	 * @update 2015年1月30日 下午8:25:56
	 * @param list
	 */
	protected void notifyDataSetChanged(List<Emojicon> list) {
		if (!SystemUtil.isEmpty(list)) {
			data.clear();
			data.addAll(list);
			mAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if (activity instanceof OnEmojiconClickedListener) {
            mOnEmojiconClickedListener = (OnEmojiconClickedListener) activity;
        } else if (getParentFragment() instanceof OnEmojiconClickedListener) {
            mOnEmojiconClickedListener = (OnEmojiconClickedListener) getParentFragment();
        } else {
            throw new IllegalArgumentException(activity + " must implement interface " + OnEmojiconClickedListener.class.getSimpleName());
        }
		
	}
	
	@Override
	public void onDetach() {
		mOnEmojiconClickedListener = null;
		recents = null;
		super.onDetach();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Emojicon emojicon = (Emojicon) parent.getItemAtPosition(position);
		if (mOnEmojiconClickedListener != null) {
            mOnEmojiconClickedListener.onEmojiconClicked(emojicon);
        }
		
		if (mEmojiType != 0) {	//不在最近的表情面板，则添加到最近的表情集合里去
			if (recents != null) {
				recents.addRecentEmoji(getActivity(), (Emojicon) parent.getItemAtPosition(position));
			}
		}
	}
	
	/**
	 * 表情的网格适配器
	 * @author huanghui1
	 * @update 2014年10月27日 下午2:48:31
	 */
	class EmojiAdapter extends CommonAdapter<Emojicon> {

		public EmojiAdapter(List<Emojicon> list, Context context) {
			super(list, context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			EmojiViewHolder holder = null;
			if (convertView == null) {
				holder = new EmojiViewHolder();
				
				convertView = inflater.inflate(R.layout.item_emoji1, parent, false);
				
				holder.icon = (EmojiconTextView) convertView.findViewById(R.id.iv_emoji);
				holder.icon.setUseSystemDefault(mUseSystemDefault);
				
				convertView.setTag(holder);
			} else {
				holder = (EmojiViewHolder) convertView.getTag();
			}
			
			final Emojicon emoji = list.get(position);
			holder.icon.setText(emoji.getEmoji());
			return convertView;
		}
		
	}

	final class EmojiViewHolder {
		EmojiconTextView icon;
	}
	
}