package net.ibaixin.chat.fragment;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.JokeReadMainActivity;
import net.ibaixin.chat.activity.PersonalInfoActivity;
import net.ibaixin.chat.activity.SettingActivity;
import net.ibaixin.chat.activity.VideoReadMainActivity;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.receiver.BasePersonalInfoReceiver;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.listener.OnItemClickListener;
/**
 * "我"的fragment界面，包含有个人信息，设置等
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月8日 下午7:46:37
 */
public class MineFragment extends BaseFragment implements OnClickListener {
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options = SystemUtil.getGeneralImageOptions();
	
	PersonalInfoReceiver mInfoReceiver;
	
	PersonalInfoObserver mPersonalInfoObserver;
	
	/**
	 * 个人中心列表
	 */
	private RecyclerView mRecyclerView;
	private List<CenterItem> mDatas = new ArrayList<>();
	private MineCenterAdapter mCenterAdapter;
	
	/**
	 * hander
	 */
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:	//加载本地数据库的个人信息成功，则刷新个人信息，头像
				Personal person = (Personal) msg.obj;
				if (person != null) {
					String iconPath = person.getIconShowPath();
//					if (SystemUtil.isFileExists(iconPath)) {
//						String iconUri = Scheme.FILE.wrap(iconPath);
//						//从缓存中删除该图像的内存缓存
//						MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
////						mImageLoader.displayImage(iconUri, ivIcon, options);
//					}
//					tvNickname.setText(person.getName());
					CenterItem accountItem = mDatas.get(0);
					accountItem.title = person.getName();
					accountItem.iconPath = iconPath;
					if (mCenterAdapter != null) {	//只更新第0个
						Log.d("---MineFragment-----handleMessage---notifyItemChanged--0---");
						mCenterAdapter.notifyItemChanged(0);
					}
				}
				break;

			default:
				break;
			}
		}
	};
	
	/**
	 * 初始化fragment
	 * @update 2014年10月8日 下午10:08:39
	 * @return
	 */
	public static MineFragment newInstance() {
		MineFragment fragment = new MineFragment();
		return fragment;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mInfoReceiver = new PersonalInfoReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BasePersonalInfoReceiver.ACTION_REFRESH_PERSONAL_INFO);
		activity.registerReceiver(mInfoReceiver, intentFilter);
		
		registerContentObserver(activity);
	}
	
	/**
	 * 注册个人信息的观察者
	 * 
	 * @update 2015年7月21日 上午11:04:07
	 */
	private void registerContentObserver(Context context) {
		mPersonalInfoObserver = new PersonalInfoObserver(mHandler);
		context.getContentResolver().registerContentObserver(Provider.PersonalColums.CONTENT_URI, true, mPersonalInfoObserver);
	}
	
	
	@Override
	public void onDetach() {
		if (mInfoReceiver != null) {
			getActivity().unregisterReceiver(mInfoReceiver);
		}
		if (mPersonalInfoObserver != null) {
			mContext.getContentResolver().unregisterContentObserver(mPersonalInfoObserver);
		}
		super.onDetach();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_mine, container, false);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mRecyclerView = (RecyclerView) view.findViewById(R.id.lv_data);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());
		mRecyclerView.setHasFixedSize(true);
		/*item_mine = view.findViewById(R.id.item_mine);
		item_readjokestext = view.findViewById(R.id.item_readjokestext);
		item_readjokesvideo = view.findViewById(R.id.item_readjokesvideo);
		item_setting = view.findViewById(R.id.item_setting);
		
		ivIcon = (ImageView) view.findViewById(R.id.item_mine_photo);
		tvNickname = (TextView) view.findViewById(R.id.mine_nickname);
		tvAccount = (TextView) view.findViewById(R.id.mine_account);
		
		item_mine.setOnClickListener(this);
		item_readjokestext.setOnClickListener(this);
		item_readjokesvideo.setOnClickListener(this);
		item_setting.setOnClickListener(this);
		
		showCurrentUserInfo(false);*/
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		initData();
		
		showCurrentUserInfo(false);
		
		mCenterAdapter = new MineCenterAdapter(mContext, mDatas);
		mRecyclerView.setAdapter(mCenterAdapter);
		
		mCenterAdapter.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(View view, int position) {
				CenterItem item = mDatas.get(position);
				String actionClassName = item.actionClassName;
				if (!TextUtils.isEmpty(actionClassName)) {
					Intent intent = new Intent();
					intent.setClassName(mContext, actionClassName);
					if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
						startActivity(intent);
					}
				} else {
					Log.w("----MineFragment-----setOnItemClickListener---无响应---position----" + position + "----actionClassName---" + actionClassName);
				}
			}
		});
	}
	
	/**
	 * 初始化数据
	 * @update 2015年8月5日 上午10:16:12
	 */
	private void initData() {
		mDatas.clear();
		CenterItem centerItem = new CenterItem(R.drawable.contact_head_icon_default, null, 0, null, PersonalInfoActivity.class.getName());
		mDatas.add(centerItem);
		
		centerItem = new CenterItem(R.drawable.item_readjokestext_ico, getString(R.string.item_readjokestext), 1, JokeReadMainActivity.class.getName());
		mDatas.add(centerItem);
		
		centerItem = new CenterItem(R.drawable.item_readjokesvideo_ico, getString(R.string.item_readjokesvideo), 2, VideoReadMainActivity.class.getName());
		mDatas.add(centerItem);
		
		centerItem = new CenterItem(R.drawable.item_setting_ico, getString(R.string.item_setting), 1, SettingActivity.class.getName());
		mDatas.add(centerItem);
	}
	
	class MineCenterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private List<CenterItem> mList;
		private LayoutInflater mInflater;
		private OnItemClickListener mItemClickListener;

		public MineCenterAdapter(Context context, List<CenterItem> mList) {
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
			case 0:	//个人资料
				itemView = mInflater.inflate(R.layout.item_mine_center_account, parent, false);
				viewHolder = new AccountViewHolder(itemView);
				break;
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
			CenterItem item = mList.get(position);
			holder.itemView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (mItemClickListener != null) {
						mItemClickListener.onItemClick(v, position);
					}
				}
			});
			int viewType = holder.getItemViewType();
			if (viewType == 0) {	//个人资料
				AccountViewHolder accountViewHolder = (AccountViewHolder) holder;
				String iconPath = item.iconPath;
				if (SystemUtil.isFileExists(iconPath)) {	//文件存在
					String iconUri = Scheme.FILE.wrap(iconPath);
					mImageLoader.displayImage(iconUri, accountViewHolder.mIvIcon, options);
				} else {	//显示默认头像
					accountViewHolder.mIvIcon.setImageResource(item.iconRes);
				}
				accountViewHolder.mTvTitle.setText(item.title);
				accountViewHolder.mTvContent.setText(item.content);
			} else {
				SettingViewHolder settingViewHolder = (SettingViewHolder) holder;
				settingViewHolder.mIvIcon.setImageResource(item.iconRes);
				settingViewHolder.mTvContent.setText(item.content);
			}
		}
		
		@Override
		public int getItemViewType(int position) {
			CenterItem item = mList.get(position);
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
	 * 显示当前登陆账号的个人信息
	 * @param clearCache 在显示用户图像前，是否需要清除该图像的缓存，主要用于修改图像后的操作
	 * @author tiger
	 * @update 2015年3月15日 下午2:25:06
	 */
	private void showCurrentUserInfo(boolean clearCache) {
		final Personal personal = ChatApplication.getInstance().getCurrentUser();
		String title = null;
		String content = null;
		String iconPath = null;
		if (personal != null) {
			iconPath = personal.getIconShowPath();
			if (SystemUtil.isFileExists(iconPath)) {
				String iconUri = Scheme.FILE.wrap(iconPath);
				if (clearCache) {	//需要清除图片缓存
					//从缓存中删除该图像的内存缓存
					MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
				}
//				mImageLoader.displayImage(iconUri, ivIcon, options);
			}
			title = personal.getName();
			content = getString(R.string.prompt_account, personal.getUsername());
//			tvNickname.setText(personal.getName());
//			tvAccount.setText(getString(R.string.prompt_account, personal.getUsername()));
		}
		//第一条是个人账号信息
		CenterItem accountItem = mDatas.get(0);
		accountItem.title = title;
		accountItem.content = content;
		accountItem.iconPath = iconPath;
	}
	
	/**
	 * 从本地数据库加载个人信息
	 * @param personal 个人信息的实体
	 * @update 2015年8月5日 下午9:21:14
	 */
	private void loadPersonalInfo(final Personal personal) {
		//查询本地数据库，并更新
		SystemUtil.getCachedThreadPool().execute(new Runnable() {
			
			@Override
			public void run() {
				Personal personInfo = PersonalManage.getInstance().getLocalSelfInfoById(personal);//查询本地数据库获取自己的详细资料
				if (personInfo != null) {	//再次更新个人信息
					ChatApplication app = (ChatApplication) mContext.getApplicationContext();
					app.setCurrentUser(personInfo);
					
					//加载本地个人信息成功
					Message msg = mHandler.obtainMessage();
					msg.what = Constants.MSG_SUCCESS;
					msg.obj = personInfo;
					mHandler.sendMessage(msg);
				}
			}
		});
	}

	@Override
	public void onClick(View v) {
		/*Intent intent = null ;
		switch (v.getId()) {
		case R.id.item_mine:
			intent = new Intent(mContext, PersonalInfoActivity.class);
			startActivity(intent);
			break;
		case R.id.item_readjokestext:
			intent = new Intent(mContext, JokeReadMainActivity.class);
			startActivity(intent);
			break;
		case R.id.item_readjokesvideo:
			intent = new Intent(mContext, VideoReadMainActivity.class);
			startActivity(intent);
			break;
		case R.id.item_setting:
			intent = new Intent(mContext, SettingActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}*/
	}
	
	class CenterItem {
		int iconRes;
		String title;
		String content;
		int viewType;
		String iconPath;
		String actionClassName;
		
		public CenterItem(int iconRes, String content, int viewType, String actionClassName) {
			super();
			this.iconRes = iconRes;
			this.content = content;
			this.viewType = viewType;
			this.actionClassName = actionClassName;
		}
		
		public CenterItem(int iconRes, String content, int viewType, String title, String actionClassName) {
			this(iconRes, content, viewType, actionClassName);
			this.title = title;
		}
		
		public CenterItem() {
			super();
		}
	}
	
	/**
	 * 接收个人信息更新的广播
	 * @author tiger
	 * @update 2015年3月15日 下午2:00:46
	 *
	 */
	public class PersonalInfoReceiver extends BasePersonalInfoReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			switch (action) {
			case ACTION_REFRESH_PERSONAL_INFO:	//更新个人信息
//				showCurrentUserInfo(true);
				break;

			default:
				break;
			}
		}
		
	}
	
	/**
	 * 个人信息的数据库观察者
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年7月21日 上午11:16:39
	 */
	private class PersonalInfoObserver extends ContentObserver {

		public PersonalInfoObserver(Handler handler) {
			super(handler);
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d("------selfChange-----" + selfChange);
			ChatApplication app = (ChatApplication) mContext.getApplicationContext();
			String username = app.getCurrentAccount();
			if (!TextUtils.isEmpty(username)) {
				Personal personal = new Personal();
				personal.setUsername(username);
				loadPersonalInfo(personal);
			}
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			Log.d("------selfChange-----" + selfChange + "---uri---" + uri.toString());
			if (SystemUtil.hasSDK16()) {
				//根据改变的 uri再查询本地个人信息，刷新界面
				String idStr = uri.getLastPathSegment();
				if (!TextUtils.isEmpty(idStr)) {
					int id = -1;
					try {
						id = Integer.parseInt(idStr);
						ChatApplication app = (ChatApplication) mContext.getApplicationContext();
						String username = app.getCurrentAccount();
						Personal personal = new Personal();
						personal.setId(id);
						personal.setUsername(username);
						
						loadPersonalInfo(personal);
					} catch (Exception e) {
						Log.e(e.getMessage());
					}
				}
			} else {
				onChange(selfChange);
			}
		}
		
	}
}
