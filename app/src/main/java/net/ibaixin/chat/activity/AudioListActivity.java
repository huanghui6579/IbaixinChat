package net.ibaixin.chat.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.ibaixin.chat.R;
import net.ibaixin.chat.loader.AudioLoader;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.AudioItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressWheel;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * 音频文件列表
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月22日 下午3:21:05
 */
public class AudioListActivity extends BaseActivity implements LoaderCallbacks<List<AudioItem>> {
	private MsgManager msgManager = MsgManager.getInstance();
	
	private ListView lvData;
	private View emptyView;
	private ProgressWheel pbLoading;
	
	private List<AudioItem> mAudioItems = new ArrayList<>();
	
	private AudioAdapter mAudioAdapter;
	
	private MsgInfo msgInfo;

	@Override
	protected int getContentView() {
		return R.layout.layout_common_list;
	}

	@Override
	protected void initView() {
		lvData = (ListView) findViewById(R.id.lv_data);
		emptyView = findViewById(R.id.empty_view);
		pbLoading = (ProgressWheel) findViewById(R.id.pb_loading);
	}

	@Override
	protected void initData() {
		msgInfo = getIntent().getParcelableExtra(ChatActivity.ARG_MSG_INFO);
		
		mAudioAdapter = new AudioAdapter(mAudioItems, mContext);
		lvData.setAdapter(mAudioAdapter);
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void addListener() {
		lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				msgInfo = msgManager.getMsgInfoByAudio(msgInfo, mAudioItems.get(position));
				if (msgInfo != null) {
					Intent data = new Intent();
					data.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
					setResult(RESULT_OK, data);
					finish();
				} else {
					SystemUtil.makeShortToast(R.string.album_photo_chose_error);
					setResult(RESULT_CANCELED);
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.audio_select, menu);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setQueryHint(getString(R.string.audio_list_search_hint));
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				mAudioAdapter.getFilter().filter(query);
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				mAudioAdapter.getFilter().filter(newText);
				return true;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void beforeBack() {
		if (SystemUtil.isSoftInputActive()) {
			SystemUtil.hideSoftInput(this);
		}
	}
	
	@Override
	public Loader<List<AudioItem>> onCreateLoader(int id, Bundle args) {
		if (!SystemUtil.isViewVisible(pbLoading)) {
			pbLoading.setVisibility(View.VISIBLE);
		}
		return new AudioLoader(mContext);
	}

	@Override
	public void onLoadFinished(Loader<List<AudioItem>> loader,
			List<AudioItem> data) {
		mAudioItems.clear();
		if (!SystemUtil.isEmpty(data)) {
			mAudioItems.addAll(data);
		}
		if (lvData.getEmptyView() == null) {
			lvData.setEmptyView(emptyView);
		}
		
		mAudioAdapter.notifyDataSetChanged();
		
		if (SystemUtil.isViewVisible(pbLoading)) {
			pbLoading.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<AudioItem>> loader) {
		mAudioItems.clear();
		mAudioAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 音频文件的适配器
	 * @author huanghui1
	 * @update 2014年11月22日 下午3:32:20
	 */
	class AudioAdapter extends CommonAdapter<AudioItem> implements Filterable {

		public AudioAdapter(List<AudioItem> list, Context context) {
			super(list, context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AudioViewHolder holder = null;
			if (convertView == null) {
				holder = new AudioViewHolder();
				convertView = inflater.inflate(R.layout.item_audio, parent, false);
				
				holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
				holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
				holder.tvSize = (TextView) convertView.findViewById(R.id.tv_size);
				
				convertView.setTag(holder);
			} else {
				holder = (AudioViewHolder) convertView.getTag();
			}
			
			final AudioItem audioItem = list.get(position);
			String durationStr = SystemUtil.timeToString(audioItem.getDuration());
			String artist = audioItem.getArtist();
			
			holder.tvTitle.setText(audioItem.getTitle());
			holder.tvSize.setText(SystemUtil.sizeToString(audioItem.getSize()));
			holder.tvContent.setText(getString(R.string.audio_item_content, durationStr, artist));
			return convertView;
		}

		@Override
		public Filter getFilter() {
			Filter filter = new Filter() {
				
				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					// TODO Auto-generated method stub
					list = (List<AudioItem>) results.values;
					notifyDataSetChanged();
				}
				
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults results = new FilterResults();
					List<AudioItem> filterList = new ArrayList<>();
					constraint = constraint.toString().toLowerCase(Locale.getDefault());
					for (int i = 0; i < mAudioItems.size(); i++) {
						AudioItem item = mAudioItems.get(i);
						String title = item.getTitle();
						if (title.toLowerCase(Locale.getDefault()).startsWith(constraint.toString())) {
							filterList.add(item);
						}
					}
					results.count = filterList.size();
					results.values = filterList;
					return results;
				}
			};
			return filter;
		}
		
	}

	final class AudioViewHolder {
		TextView tvTitle;
		TextView tvSize;
		TextView tvContent;
	}
}
