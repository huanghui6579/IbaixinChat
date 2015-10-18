package net.ibaixin.chat.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import net.ibaixin.chat.R;
import net.ibaixin.chat.loader.FileItemLoder;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.NativeUtil;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressWheel;

/**
 * 文件浏览的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月21日 下午2:40:32
 */
public class FileExplorerActivity extends BaseActivity implements LoaderCallbacks<List<FileItem>> {
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	private MsgManager msgManager = MsgManager.getInstance();
	
	private ListView lvData;
	private View emptyView;
	private TextView backView;
	private ProgressWheel pbLoading;
	
	private ProgressDialog pDialog;
	
	/**
	 * 当前所在的目录
	 */
	private File currentPath;
	
	private FileItemAdpter mFileItemAdapter;
	private List<FileItem> mFileItems = new ArrayList<>();
//	private TextView btnOpt;
	
	private MenuItem mMenuDone;
	
	private MsgInfo msgInfo;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:
				Intent data = new Intent();
				data.putParcelableArrayListExtra(ChatActivity.ARG_MSG_INFO_LIST, (ArrayList<MsgInfo>)msg.obj);
				setResult(RESULT_OK, data);
				break;
			case Constants.MSG_FAILED:
				SystemUtil.makeShortToast(R.string.album_photo_chose_error);
				setResult(RESULT_CANCELED);
				break;
			default:
				break;
			}
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			finish();
		}
		
	};

	@Override
	protected int getContentView() {
		return R.layout.activity_file_explorer;
	}

	@Override
	protected void initView() {
		lvData = (ListView) findViewById(R.id.lv_data);
		emptyView = findViewById(R.id.empty_view);
		backView = (TextView) findViewById(R.id.back_view);
		pbLoading = (ProgressWheel) findViewById(R.id.pb_loading);
	}

	@Override
	protected void initData() {
		
		currentPath = SystemUtil.getSDCardRoot();
		backView.setText(currentPath.getAbsolutePath());
		
		msgInfo = getIntent().getParcelableExtra(ChatActivity.ARG_MSG_INFO);
		
		mFileItemAdapter = new FileItemAdpter(mFileItems, mContext);
		lvData.setAdapter(mFileItemAdapter);
		
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void addListener() {
		backView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				backToParent();
			}
		});
		lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				File file = mFileItems.get(position).getFile();
				if (file.canRead()) {
					if (file.isDirectory()) {
						currentPath = file;
						getSupportLoaderManager().restartLoader(0, null, FileExplorerActivity.this);
					} else {	//文件就选中
						FileItemViewHolder holder = (FileItemViewHolder) view.getTag();
						holder.cbChose.setChecked(!holder.cbChose.isChecked());
					}
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_save, menu);
		mMenuDone = menu.findItem(R.id.action_select_complete);
		mMenuDone.setEnabled(false);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_select_complete:
			final List<FileItem> selects = mFileItemAdapter.getSelectList();
			pDialog = ProgressDialog.show(mContext, null, getString(R.string.chat_sending_file), false, true);
			//发送文件
			SystemUtil.getCachedThreadPool().execute(new Runnable() {
				
				@Override
				public void run() {
					final ArrayList<MsgInfo> msgList = msgManager.getMsgInfoListByFileItems(msgInfo, selects);
					Message msg = mHandler.obtainMessage();
					if (!SystemUtil.isEmpty(msgList)) {	//消息集合
						msg.what = Constants.MSG_SUCCESS;
						msg.obj = msgList;
					} else {
						msg.what = Constants.MSG_FAILED;
					}
					mHandler.sendMessage(msg);
				}
			});
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * 返回到上一层
	 * @update 2014年11月21日 下午8:57:07
	 */
	private void backToParent() {
		if (currentPath != null && !SystemUtil.isRoot(currentPath)) {	//是根目录
			currentPath = currentPath.getParentFile();
			getSupportLoaderManager().restartLoader(0, null, this);
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	public void onBackPressed() {
		backToParent();
	}

	@Override
	public Loader<List<FileItem>> onCreateLoader(int id, Bundle args) {
		mFileItemAdapter.clearSelectList();
		if (mMenuDone != null) {
			resetActionMenu();
		}
		if (!SystemUtil.isViewVisible(pbLoading)) {
			pbLoading.setVisibility(View.VISIBLE);
		}
		return new FileItemLoder(mContext, currentPath);
	}

	@Override
	public void onLoadFinished(Loader<List<FileItem>> loader,
			List<FileItem> data) {
		mFileItems.clear();
		if (!SystemUtil.isEmpty(data)) {
			mFileItems.addAll(data);
		}
		if (lvData.getEmptyView() == null) {
			lvData.setEmptyView(emptyView);
		}
		backView.setText(currentPath.getAbsolutePath());
		mFileItemAdapter.notifyDataSetChanged();
		if (SystemUtil.isViewVisible(pbLoading)) {
			pbLoading.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<FileItem>> loader) {
		mFileItems.clear();
		mFileItemAdapter.clearSelectList();
		mFileItemAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 恢复菜单原样
	 * @update 2014年11月14日 下午9:06:53
	 */
	private void resetActionMenu() {
		mMenuDone.setEnabled(false);
		mMenuDone.setTitle(R.string.action_select_complete);
	}
	
	/**
	 * 文件的适配器
	 * @author huanghui1
	 * @update 2014年11月21日 下午5:07:23
	 */
	class FileItemAdpter extends CommonAdapter<FileItem> {
		int selectSize = 0;
		SparseBooleanArray selectArray = new SparseBooleanArray();
		DisplayImageOptions options = SystemUtil.getAlbumImageOptions();

		public FileItemAdpter(List<FileItem> list, Context context) {
			super(list, context);
		}
		
		/**
		 * 清除选择的项
		 * @update 2014年11月21日 下午9:57:21
		 */
		public void clearSelectList() {
			selectSize = 0;
			selectArray.clear();
		}
		
		/**
		 * 获得选中的项
		 * @update 2014年11月21日 下午10:06:50
		 * @return
		 */
		public List<FileItem> getSelectList() {
			List<FileItem> selectList = new ArrayList<>();
			for (int i = 0; i < selectArray.size(); i++) {
				int key = selectArray.keyAt(i);
				boolean value = selectArray.get(key, false);
				if (value) {
					selectList.add(list.get(key));
				}
			}
			return selectList;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FileItemViewHolder holder = null;
			if (convertView == null) {
				holder = new FileItemViewHolder();
				
				convertView = inflater.inflate(R.layout.item_file_attach, parent, false);
				
				holder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_head_icon);
				holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
				holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
				holder.tvDesc = (TextView) convertView.findViewById(R.id.tv_desc);
				holder.cbChose = (CheckBox) convertView.findViewById(R.id.cb_chose);
				
				convertView.setTag(holder);
			} else {
				holder = (FileItemViewHolder) convertView.getTag();
			}
			
			final FileItem fileItem = list.get(position);
			File file = fileItem.getFile();
			holder.tvTitle.setText(file.getName());
			holder.tvContent.setText(SystemUtil.formatTime(file.lastModified(), Constants.DATEFORMA_TPATTERN_DEFAULT));
			if (file.isDirectory()) {	//文件夹
				holder.ivIcon.setImageResource(R.drawable.ic_folder);
				String desc = null;
				if (file.canRead()) {
					int count = NativeUtil.listFileNames(file.getAbsolutePath()).size();
					desc = getString(R.string.file_explorer_folder_item_num, count);
				} else {
					desc = getString(R.string.file_explorer_folder_unreadable);
				}
				holder.tvDesc.setText(desc);
				holder.cbChose.setVisibility(View.GONE);
			} else {	//文件
				holder.cbChose.setVisibility(View.VISIBLE);
				String filePath = file.getAbsolutePath();
				
				holder.cbChose.setOnCheckedChangeListener(null);
				
				holder.cbChose.setChecked(selectArray.indexOfKey(position) >= 0 ? selectArray.get(position) : false);
				
				holder.cbChose.setOnCheckedChangeListener(new MyCheckedChangeListener(position));
				
				String desc = null;
				if (file.canRead()) {
					desc = SystemUtil.sizeToString(file.length());
				} else {
					desc = getString(R.string.file_explorer_folder_unreadable);
				}
				holder.tvDesc.setText(desc);
				
				Integer resId = SystemUtil.getResIdByFile(fileItem, R.drawable.ic_file);
				holder.ivIcon.setImageResource(resId);
				switch (fileItem.getFileType()) {
				case IMAGE:	//图片,则直接加载图片缩略图
					String imagePath = msgManager.getImageThumbPath(filePath);
					if (TextUtils.isEmpty(imagePath)) {
						imagePath = filePath;
					}
					mImageLoader.displayImage(Scheme.FILE.wrap(imagePath), holder.ivIcon, options);
					break;
				case AUDIO:	//音频
					String thumbPath = msgManager.getAudioThumbPath(filePath);
					if (!TextUtils.isEmpty(thumbPath)) {
						mImageLoader.displayImage(Scheme.FILE.wrap(thumbPath), holder.ivIcon, options);
					}
					break;
				case APK:	//安装文件
					new LoadApkIconTask(holder).execute(filePath);
					break;
				default:
					break;
				}
			}
			
			return convertView;
		}
		
		class MyCheckedChangeListener implements OnCheckedChangeListener {
			private int position;

			public MyCheckedChangeListener(int position) {
				super();
				this.position = position;
			}

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					if (selectSize == Constants.ALBUM_SELECT_SIZE) {
						buttonView.setChecked(false);
						SystemUtil.makeShortToast(getString(R.string.file_tip_max_select, Constants.ALBUM_SELECT_SIZE));
						return;
					}
					selectSize ++;
				} else {
					selectSize --;
					selectSize = selectSize < 0 ? 0 : selectSize;
				}
				if (selectSize == 0) {	//没有选中文件
					resetActionMenu();
				} else {
					mMenuDone.setEnabled(true);
					mMenuDone.setTitle(getString(R.string.action_select_complete) + "(" + selectSize + "/" + Constants.ALBUM_SELECT_SIZE + ")");
				}
				selectArray.put(position, isChecked);
			}
			
		}
		
	}
	
	/**
	 * 异步加载apk图标的线程
	 * @author huanghui1
	 * @update 2014年11月21日 下午6:03:44
	 */
	class LoadApkIconTask extends AsyncTask<String, Drawable, Drawable> {
		FileItemViewHolder holder;
		public LoadApkIconTask(FileItemViewHolder holder) {
			super();
			this.holder = holder;
		}
		@Override
		protected Drawable doInBackground(String... params) {
			Drawable drawable = SystemUtil.getApkIcon(params[0]);
			return drawable;
		}
		@Override
		protected void onPostExecute(Drawable result) {
			if(result != null) {
				holder.ivIcon.setImageDrawable(result);
			}
			super.onPostExecute(result);
		}
	}
	
	final class FileItemViewHolder {
		ImageView ivIcon;
		TextView tvTitle;
		TextView tvContent;
		TextView tvDesc;
		CheckBox cbChose;
	}
	
}
