package net.ibaixin.chat.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.download.ImageDownloader;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.view.listener.OnItemClickListener;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 设置界面
 * @author dudejin
 * @version 1.3.1
 * @update 2015年12月29日
 */
public class SettingActivity extends BaseActivity /*implements OnClickListener*/ {
	private ProgressDialog proDialog;
	
	/** 清理缓存 **/
//	private View setting_item_clear ;
	/** 关于 **/
//	private View setting_item_about ;
	/** 退出 **/
//	private View setting_item_exit ;

	/**
	 * 个人中心列表
	 */
	private RecyclerView mRecyclerView;
	private List<SettingItem> mDatas = new ArrayList<>();
	private SettingItemAdapter mSettingAdapter;
	
	private final int MSG_CLEAR_OK = 0x01;
	private final int MSG_CLEAR_ERROR = 0x02;
	
	Handler mHander = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(proDialog != null && proDialog.isShowing()) {
				proDialog.dismiss();
			}
			switch (msg.what) {
			case MSG_CLEAR_OK:
				SystemUtil.makeShortToast(R.string.clear_done);
				break;
			case MSG_CLEAR_ERROR:
				SystemUtil.makeShortToast(R.string.clear_error);
				break;
			default:
				break;
			}
		};
	};
	
	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_setting;
	}

	@Override
	protected void initView() {
//		setting_item_clear = findViewById(R.id.setting_item_clear);
//		setting_item_about = findViewById(R.id.setting_item_about);
//		setting_item_exit = findViewById(R.id.setting_item_exit);
		mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());
		mRecyclerView.setHasFixedSize(true);
	}

	@Override
	protected void initData() {
		setmDatas();
		mSettingAdapter = new  SettingItemAdapter(mContext, mDatas);
		mRecyclerView.setAdapter(mSettingAdapter);
		mSettingAdapter.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(View view, int position) {
				final SettingItem infoItem = mDatas.get(position);
				int actionType = infoItem.actionType;
				Intent intent = null;
				switch (actionType) {
					case SettingItem.TYPE_CLEAR:
						MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
						builder.title(R.string.prompt)
								.content(R.string.setting_clear_confirm)
								.positiveText(android.R.string.ok)
								.negativeText(android.R.string.cancel)
								.callback(new MaterialDialog.ButtonCallback() {
									@Override
									public void onPositive(MaterialDialog dialog) {
										clearLocalFiles() ;
									}
								}).show();
					break;
					case SettingItem.TYPE_ABOUT:
						intent = new Intent(mContext, AboutActivity.class);
						startActivity(intent);
					break;
					case SettingItem.TYPE_EXIT:
						ChatApplication.getInstance().exit();
					break;
				}
			}
		});
	}

	@Override
	protected void addListener() {
//		setting_item_about.setOnClickListener(this);
//		setting_item_exit.setOnClickListener(this);
//		setting_item_clear.setOnClickListener(this);
	}

/*	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.setting_item_clear:
			MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
			builder.title(R.string.prompt)
				.content(R.string.setting_clear_confirm)
				.positiveText(android.R.string.ok)
				.negativeText(android.R.string.cancel)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						clearLocalFiles() ;
					}
				}).show();
			break;
		case R.id.setting_item_about:
			Intent intent = new Intent(mContext, AboutActivity.class);
			startActivity(intent);
			break;
		case R.id.setting_item_exit:
			ChatApplication.getInstance().exit();
			break;
		default:
			break;
		}
	}*/

	private void setmDatas(){
		//清理缓存
		SettingItem infoItem = new SettingItem(R.drawable.ico_clear,getString(R.string.setting_clear),SettingItem.TYPE_CLEAR,1);
		mDatas.add(infoItem);
		//关于
		infoItem = new SettingItem(R.drawable.ico_about,getString(R.string.about),SettingItem.TYPE_ABOUT,1);
		mDatas.add(infoItem);
		//退出
		infoItem = new SettingItem(R.drawable.ico_exit,getString(R.string.action_exit),SettingItem.TYPE_EXIT,2);
		mDatas.add(infoItem);

	}

	class SettingItem {
		static final int TYPE_CLEAR = 0;
		static final int TYPE_EXIT = 1;
		static final int TYPE_ABOUT = 2;

		int iconRes;
		String title;
		int actionType;
		/**
		 * 布局的类型
		 */
		int viewType;

		public SettingItem(int iconRes,String title, int actionType,int viewType) {
			super();
			this.iconRes = iconRes;
			this.title = title;
			this.actionType = actionType;
			this.viewType = viewType;
		}

		public SettingItem() {
			super();
		}
	}

	class SettingItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private List<SettingItem> mList;
		private LayoutInflater mInflater;
		private OnItemClickListener mItemClickListener;

		public SettingItemAdapter(Context context, List<SettingItem> mList) {
			super();
			this.mList = mList;
			mInflater = LayoutInflater.from(context);
		}

		public void setOnItemClickListener(OnItemClickListener itemClickListener) {
			this.mItemClickListener = itemClickListener;
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = null;
			RecyclerView.ViewHolder viewHolder = null;
			switch (viewType) {
				case 1:	//有上间距的设置项
					itemView = mInflater.inflate(R.layout.item_mine_center, parent, false);
					viewHolder = new SettingViewHolder(itemView);
					break;
				case 2:	//没有上间距的设置项
					itemView = mInflater.inflate(R.layout.item_mine_center, parent, false);
					RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) itemView.getLayoutParams();
					layoutParams.topMargin = 1;
					viewHolder = new SettingViewHolder(itemView);
					break;
				default:
					itemView = mInflater.inflate(R.layout.item_mine_center, parent, false);
					viewHolder = new SettingViewHolder(itemView);
					break;
			}
			return viewHolder;
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
			SettingItem item = mList.get(position);
			holder.itemView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mItemClickListener != null) {
						mItemClickListener.onItemClick(v, position);
					}
				}
			});
			SettingViewHolder settingViewHolder = (SettingViewHolder) holder;
			settingViewHolder.mIvIcon.setImageResource(item.iconRes);
			settingViewHolder.mTvContent.setText(item.title);
		}

		@Override
		public int getItemViewType(int position) {
			SettingItem item = mList.get(position);
			return item.viewType;
		}

		@Override
		public int getItemCount() {
			return mList == null ? 0 : mList.size();
		}

		class SettingViewHolder extends RecyclerView.ViewHolder {
			ImageView mIvIcon;
			TextView mTvContent;

			public SettingViewHolder(View itemView) {
				super(itemView);
				mIvIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
				mTvContent = (TextView) itemView.findViewById(R.id.tv_content);
			}

		}

		class AccountViewHolder extends RecyclerView.ViewHolder {
			ImageView mIvIcon;
			TextView mTvTitle;
			TextView mTvContent;

			public AccountViewHolder(View itemView) {
				super(itemView);
				mIvIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
				mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
				mTvContent = (TextView) itemView.findViewById(R.id.tv_content);
			}

		}

	}


	/**
	 * 清理本地缓存
	 */
	private void clearLocalFiles() {
		if (proDialog == null) {
			proDialog = ProgressDialog.show(mContext, null, getString(R.string.clear_doing), true);
		} else {
			proDialog.show();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					SystemUtil.deletFiles(SystemUtil.getPublicFilePath());//删除公共文件目录资源
					SystemUtil.deletFiles(SystemUtil.getDefaultRoot().getAbsolutePath()+File.separator + Constants.DATA_MSG_ATT_FOLDER_NAME);//删除自己的聊天记录文件
					SystemUtil.deletFiles(SystemUtil.getDataParentFile()+File.separator+"app_webview");//删除webview缓存
					List<MsgThread> list = MsgManager.getInstance().getMsgThreadList();
					for(MsgThread m: list){
						MsgManager.getInstance().deleteMsgThreadById(m.getId());
					}
					mHander.sendEmptyMessage(MSG_CLEAR_OK);
				} catch (Exception e) {
					mHander.sendEmptyMessage(MSG_CLEAR_ERROR);
					Log.e(TAG, e.toString());
				}
			}
		}).start();
	}
}
