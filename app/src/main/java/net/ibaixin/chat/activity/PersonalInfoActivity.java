package net.ibaixin.chat.activity;

import java.util.ArrayList;
import java.util.List;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.GeoChoiceFragment;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.manager.web.UserEngine;
import net.ibaixin.chat.model.Gender;
import net.ibaixin.chat.model.GeoInfo;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.receiver.BasePersonalInfoReceiver;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.listener.OnItemClickListener;

/**
 * 个人信息的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月11日 下午8:58:52
 */
public class PersonalInfoActivity extends BaseActivity implements OnClickListener {
	
	private static final int REQ_NICKNAME = 100;
	private static final int REQ_GEO = 101;
	private static final int REQ_SIGNATURE = 102;
	
//	private ImageView ivHeadIcon;
//	private TextView tvNick;
//	private TextView tvAccount;
//	private TextView tvSex;
//	private TextView tvAddress;
//	private TextView tvSignature;
//	
//	private TableRow iconRow;
//	private TableRow nickRow;
//	private TableRow accountRow;
//	private TableRow sexRow;
//	private TableRow addressRow;
//	private TableRow signatureRow;
	
	private RecyclerView mRecyclerView;
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options = SystemUtil.getGeneralImageOptions();
	
	private PersonalInfoReceiver mInfoReceiver;
	private List<InfoItem> mDatas = new ArrayList<>();
	
	private PersonalInfoAdapter mPersonalAdapter;

	@Override
	protected int getContentView() {
		return R.layout.activity_personal_info;
	}

	@Override
	protected void initView() {
		
		mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());
		mRecyclerView.setHasFixedSize(true);
		
		/*ivHeadIcon = (ImageView) findViewById(R.id.iv_head_icon);
		tvNick = (TextView) findViewById(R.id.tv_nick);
		tvAccount = (TextView) findViewById(R.id.tv_account);
		tvSex = (TextView) findViewById(R.id.tv_sex);
		tvAddress = (TextView) findViewById(R.id.tv_address);
		tvSignature = (TextView) findViewById(R.id.tv_signature);
		
		iconRow = (TableRow) findViewById(R.id.icon_row);
		nickRow = (TableRow) findViewById(R.id.nick_row);
		accountRow = (TableRow) findViewById(R.id.account_row);
		sexRow = (TableRow) findViewById(R.id.sex_row);
		addressRow = (TableRow) findViewById(R.id.address_row);
		signatureRow = (TableRow) findViewById(R.id.signature_row);*/
	}

	@Override
	protected void initData() {
		mInfoReceiver = new PersonalInfoReceiver();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BasePersonalInfoReceiver.ACTION_REFRESH_PERSONAL_INFO);
		registerReceiver(mInfoReceiver, intentFilter);
		
		loadPersonalInfo(false);
		
		mPersonalAdapter = new  PersonalInfoAdapter(mContext, mDatas);
		mRecyclerView.setAdapter(mPersonalAdapter);
		mPersonalAdapter.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(View view, int position) {
				final InfoItem infoItem = mDatas.get(position);
				int actionType = infoItem.actionType;
				int reqCode = infoItem.requestCode;
				Intent intent = null;
				switch (actionType) {
				case InfoItem.TYPE_ICON:	//点击的是有图像的item
					intent = new Intent();
					intent.setClassName(mContext, infoItem.actionClassName);
					intent.putExtra(AlbumActivity.ARG_IS_SINGLE_CHOICE, true);
					intent.putExtra(AlbumActivity.ARG_REQ_CODE, reqCode);
					if (intent.resolveActivity(getPackageManager()) != null) {
						startActivityForResult(intent, reqCode);
					}
					break;
				case InfoItem.TYPE_EDIT:	//带有编辑性质的item
					if (!TextUtils.isEmpty(infoItem.actionClassName)) {
						intent = new Intent();
						intent.setClassName(mContext, infoItem.actionClassName);
						if (intent.resolveActivity(getPackageManager()) != null) {
							startActivityForResult(intent, reqCode);
						}
					}
					break;
				case InfoItem.TYPE_DIALOG:	//对话框的item
					String[] items = mContext.getResources().getStringArray(R.array.sex_choice_menu);
					int index = -1;
					String sex = infoItem.content;
					for (int i = 0; i < items.length; i++) {
						if (sex.equals(items[i])) {
							index = i;
							break;
						}
					}
					final int initIndex = index;
					MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
					builder.title(R.string.personal_info_sex)
						.items(items)
						.itemsCallbackSingleChoice(index, new MaterialDialog.ListCallback() {
							
							@Override
							public void onSelection(MaterialDialog dialog, View itemView, final int which, final CharSequence text) {
								if (initIndex != which) {	//只有改变，才操作
									//显示选择的数据
									infoItem.content = text.toString();
									if (mPersonalAdapter != null) {
										mPersonalAdapter.notifyItemChanged(3);
									}
									//后台保存数据
									SystemUtil.getCachedThreadPool().execute(new Runnable() {
										
										@Override
										public void run() {
											Personal personal = ChatApplication.getInstance().getCurrentUser();
											personal.setSex(which + 1);
											//保存到本地数据库上
											PersonalManage personalManage = PersonalManage.getInstance();
											personalManage.updateGender(personal);
											
											//保存到网络上
											UserEngine userEngine = new UserEngine(mContext);
											userEngine.updateGender(personal);
										}
									});
								}
							}
						})
						.positiveText(android.R.string.ok)
						.negativeText(android.R.string.cancel)
						.autoDismiss(true)
						.show();
					
					break;
				default:
					break;
				}
			}
		});
//		showCurrentUserInfo(false);

	}

	@Override
	protected void addListener() {
//		iconRow.setOnClickListener(this);
//		nickRow.setOnClickListener(this);
//		accountRow.setOnClickListener(this);
//		sexRow.setOnClickListener(this);
//		addressRow.setOnClickListener(this);
//		signatureRow.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		/*Intent intent = null; 
		switch (v.getId()) {
		case R.id.icon_row:	//头像
			intent = new Intent(mContext, AlbumActivity.class);
			intent.putExtra(AlbumActivity.ARG_IS_SINGLE_CHOICE, true);
			int reqCode = AlbumActivity.REQ_PARENT_CLIP_ICON;
			intent.putExtra(AlbumActivity.ARG_REQ_CODE, AlbumActivity.REQ_PARENT_CLIP_ICON);
			intent.putExtra(AlbumActivity.ARG_IS_SINGLE_CHOICE, true);
			startActivityForResult(intent, reqCode);
			break;
		case R.id.nick_row:	//昵称
			intent = new Intent(mContext, EditNicknameActivity.class);
			startActivityForResult(intent, REQ_NICKNAME);
			break;
		case R.id.account_row:	//账号
			
			break;
		case R.id.sex_row:	//性别
			
			break;
		case R.id.address_row:	//地区
			
			break;
		case R.id.signature_row:	//签名
			
			break;

		default:
			break;
		}*/
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case AlbumActivity.REQ_PARENT_CLIP_ICON:	//更换头像
				if (data != null) {
					String iconPath = data.getStringExtra(ClipHeadIconActivity.ARG_DATA);
//					String iconUri = Scheme.FILE.wrap(iconPath);
					//从缓存中删除该图像的内存缓存
//					MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
//					mImageLoader.displayImage(iconUri, ivHeadIcon, options);
					
					InfoItem infoItem = mDatas.get(0);
					infoItem.content = iconPath;
					if (mPersonalAdapter != null) {
						mPersonalAdapter.notifyItemChanged(0);
					}
					
					//通知“我”的基本界面刷新头像等信息
//					Intent intent = new Intent(BasePersonalInfoReceiver.ACTION_REFRESH_PERSONAL_INFO);
//					sendBroadcast(intent);
				}
				break;
			case REQ_NICKNAME:	//修改昵称
				if (data != null) {
					String nickname = data.getStringExtra(EditNicknameActivity.ARG_NICKNAME);
//					tvNick.setText(nickname);
					
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						
						@Override
						public void run() {
							Personal personal = ChatApplication.getInstance().getCurrentUser();
							
							//后台更新昵称
							UserEngine userEngine = new UserEngine(mContext);
							userEngine.updateNickname(personal);
						}
					});
					
					InfoItem infoItem = mDatas.get(1);
					infoItem.content = nickname;
					if (mPersonalAdapter != null) {
						mPersonalAdapter.notifyItemChanged(1);
					}
					//刷新“我”的界面昵称显示
//					Intent intent = new Intent(BasePersonalInfoReceiver.ACTION_REFRESH_PERSONAL_INFO);
//					sendBroadcast(intent);
				}
				break;
			case REQ_GEO:	//选择地理位置
				if (data != null) {
					final GeoInfo geoInfo = data.getParcelableExtra(GeoChoiceFragment.ARG_GEOINFO);
					
					final Personal personal = ChatApplication.getInstance().getCurrentUser();
					
					if (!personal.equestGeoinfo(geoInfo)) {	//地理位置不相等
						personal.setCountry(geoInfo.getCountry());
						personal.setCountryId(geoInfo.getCountryId());
						personal.setProvince(geoInfo.getProvince());
						personal.setProvinceId(geoInfo.getProvinceId());
						personal.setCity(geoInfo.getCity());
						personal.setCityId(geoInfo.getCityId());
						
						if (geoInfo != null) {	//选择了地理位置
							SystemUtil.getCachedThreadPool().execute(new Runnable() {
								
								@Override
								public void run() {
									
									PersonalManage personalManage = PersonalManage.getInstance();
									personalManage.updateGeo(personal);
									
									//后台更新昵称
									UserEngine userEngine = new UserEngine(mContext);
									userEngine.updateGeo(personal);
								}
							});
							
							InfoItem infoItem = mDatas.get(4);
							infoItem.content = geoInfo.toGeneralSimpleString();
							if (mPersonalAdapter != null) {
								mPersonalAdapter.notifyItemChanged(4);
							}
						}
					}
				}
				break;
			case REQ_SIGNATURE:	//个性签名
				final Personal personal = ChatApplication.getInstance().getCurrentUser();
				CharSequence signature = data.getCharSequenceExtra(EditSignatureActivity.ARG_SIGNATURE);
				if (personal != null && signature != null) {
					personal.setDesc(signature.toString());
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						
						@Override
						public void run() {
							PersonalManage personalManage = PersonalManage.getInstance();
							//本地数据库更新
							personalManage.updateSignature(personal);
							
							//网络更新到服务器
							UserEngine userEngine = new UserEngine(mContext);
							userEngine.updateSignature(personal);
						}
					});
					InfoItem infoItem = mDatas.get(5);
					infoItem.content = signature.toString();
					if (mPersonalAdapter != null) {
						mPersonalAdapter.notifyItemChanged(5);
					}
				}
				break;
			default:
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		if (mInfoReceiver != null) {
			unregisterReceiver(mInfoReceiver);
		}
		super.onDestroy();
	}
	
	class InfoItem {
		static final int TYPE_ICON = 0;
		static final int TYPE_TEXT = 1;
		static final int TYPE_EDIT = 2;
		static final int TYPE_DIALOG = 3;
		
		int requestCode;
		String title;
		String content;
		int actionType;
		/**
		 * 布局的类型
		 */
		int viewType;
		String actionClassName;
		
		public InfoItem(String title, String content, int actionType, int viewType, String actionClassName, int requestCode) {
			super();
			this.title = title;
			this.content = content;
			this.actionType = actionType;
			this.viewType = viewType;
			this.actionClassName = actionClassName;
			this.requestCode = requestCode;
		}

		public InfoItem() {
			super();
		}
	}
	
	/**
	 * 显示当前登陆账号的个人信息
	 * @param clearCache 在显示用户图像前，是否需要清除该图像的缓存，主要用于修改图像后的操作
	 * @author tiger
	 * @update 2015年3月15日 下午2:25:06
	 */
	private void loadPersonalInfo(boolean clearCache) {
		String address = null;
		String nickname = null;
		String username = null;
		String sex = null;
		String iconPath = null;
		String signature = null;
		Personal personal = ChatApplication.getInstance().getCurrentUser();
		if (personal != null) {
			iconPath = personal.getIconShowPath();
			String iconUri = null;
			if (iconPath != null) {
				iconUri = Scheme.FILE.wrap(iconPath);
			}
			if (clearCache) {	//需要清除图片缓存
				//从缓存中删除该图像的内存缓存
				MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
			}
			String province = personal.getProvince();
			String city = personal.getCity();
			String country = personal.getCountry();
			boolean noCountry = country == null;
			boolean noProvince = province == null;
			boolean noCity = city == null;
			boolean addressNull = (noCountry) && (noProvince) && (noCity);
			String notFilledStr = mContext.getString(R.string.personal_info_not_filled);
			if (addressNull) {
				address = notFilledStr;
			} else {
				if (noProvince && noCity) {	//没有省份和城市，则选择国家
					address = country;
				} else {
					province = noProvince ? "" : province;
					city = noCity ? "" : city;
					address = province + " " + city;
				}
			}
			nickname = personal.getNickname();
			username = personal.getUsername();
			sex = Gender.valueOf(personal.getSex()).getName();
			if (TextUtils.isEmpty(sex)) {
				sex = notFilledStr;
			}
			signature = personal.getDesc();
			if (signature == null) {
				signature = notFilledStr;
			}
//			mImageLoader.displayImage(iconUri, ivHeadIcon, options);
//			tvNick.setText(personal.getNickname());
//			tvAccount.setText(personal.getUsername());
//			tvSex.setText(Gender.valueOf(personal.getSex()).getName());
//			tvAddress.setText(address);
//			tvSignature.setText(personal.getDesc());
		}
		
		mDatas.clear();
		
		//头像信息
		InfoItem infoItem = new InfoItem(getString(R.string.personal_info_head_icon), iconPath, InfoItem.TYPE_ICON, 0, AlbumActivity.class.getName(), AlbumActivity.REQ_PARENT_CLIP_ICON);
		mDatas.add(infoItem);
		
		//昵称信息
		infoItem = new InfoItem(getString(R.string.personal_info_nick), nickname, InfoItem.TYPE_EDIT, 1, EditNicknameActivity.class.getName(), REQ_NICKNAME);
		mDatas.add(infoItem);
		
		//用户名信息
		infoItem = new InfoItem(getString(R.string.personal_info_account), username, InfoItem.TYPE_TEXT, 1, EditNicknameActivity.class.getName(), 0);
		mDatas.add(infoItem);
		
		//性别信息
		infoItem = new InfoItem(getString(R.string.personal_info_sex), sex, InfoItem.TYPE_DIALOG, 2, null, 0);
		mDatas.add(infoItem);
		
		//地区信息
		infoItem = new InfoItem(getString(R.string.personal_info_address), address, InfoItem.TYPE_EDIT, 1, GeoChoiceActivity.class.getName(), REQ_GEO);
		mDatas.add(infoItem);
		
		//个性签名信息
		infoItem = new InfoItem(getString(R.string.personal_info_signature), signature, InfoItem.TYPE_EDIT, 1, EditSignatureActivity.class.getName(), REQ_SIGNATURE);
		mDatas.add(infoItem);
	}
	
	/**
	 * 个人信息的adapter
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月6日 下午3:37:31
	 */
	class PersonalInfoAdapter extends RecyclerView.Adapter<ViewHolder> {
		private List<InfoItem> mList;
		private LayoutInflater mInflater;
		
		private OnItemClickListener mItemClickListener;
		
		public PersonalInfoAdapter(Context context, List<InfoItem> mList) {
			super();
			this.mList = mList;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			ViewHolder viewHolder = null;
			View itemView = null;
			switch (viewType) {
			case 0:	//头像布局
				itemView = mInflater.inflate(R.layout.item_personal_info_icon, parent, false);
				viewHolder = new IconViewHolder(itemView);
				break;
			case 1:	//	没有上边距的item
				itemView = mInflater.inflate(R.layout.item_personal_info, parent, false);
				RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) itemView.getLayoutParams();
				layoutParams.topMargin = 1;
				viewHolder = new ItemViewHolder(itemView);
				break;
			case 2:	//有默认的上边距
				itemView = mInflater.inflate(R.layout.item_personal_info, parent, false);
				viewHolder = new ItemViewHolder(itemView);
				break;
			default:
				itemView = mInflater.inflate(R.layout.item_personal_info, parent, false);
				viewHolder = new ItemViewHolder(itemView);
				break;
			}
			return viewHolder;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int position) {
			InfoItem infoItem = mList.get(position);
			holder.itemView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (mItemClickListener != null) {
						mItemClickListener.onItemClick(v, position);
					}
				}
			});
			
			int viewType = holder.getItemViewType();
			if (viewType == 0) {	//头像的item
				IconViewHolder iconViewHolder = (IconViewHolder) holder;
				iconViewHolder.tvTitle.setText(infoItem.title);
				String iconPath = infoItem.content;
				if (SystemUtil.isFileExists(iconPath)) {
					mImageLoader.displayImage(Scheme.FILE.wrap(iconPath), iconViewHolder.ivIcon, options);
				} else {	//显示默认头像
					iconViewHolder.ivIcon.setImageResource(R.drawable.contact_head_icon_default);
				}
				iconViewHolder.iconBgView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						SystemUtil.makeShortToast("点击了头像");
					}
				});
			} else {
				ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
				itemViewHolder.tvTitle.setText(infoItem.title);
				itemViewHolder.tvContent.setText(infoItem.content);
			}
		}

		@Override
		public int getItemViewType(int position) {
			InfoItem infoItem = mList.get(position);
			return infoItem.viewType;
		}
		
		@Override
		public int getItemCount() {
			return mList == null ? 0 : mList.size();
		}
		
		/**
		 * 头像的viewHolder
		 * @author huanghui1
		 * @version 1.0.0
		 * @update 2015年8月6日 下午3:37:19
		 */
		class IconViewHolder extends ViewHolder {
			TextView tvTitle;
			ImageView ivIcon;
			View iconBgView;
			
			public IconViewHolder(View itemView) {
				super(itemView);
				tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
				ivIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
				iconBgView = itemView.findViewById(R.id.icon_bg_view);
			}
			
		}
		
		/**
		 * 其他普通项的viewHolder
		 * @author huanghui1
		 * @version 1.0.0
		 * @update 2015年8月6日 下午3:38:34
		 */
		class ItemViewHolder extends ViewHolder {
			TextView tvTitle;
			TextView tvContent;

			public ItemViewHolder(View itemView) {
				super(itemView);
				tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
				tvContent = (TextView) itemView.findViewById(R.id.tv_content);
			}
			
		}
		
		public void setOnItemClickListener(OnItemClickListener itemClickListener) {
			this.mItemClickListener = itemClickListener;
		}
	}
	
	/**
	 * 显示当前登陆账号的个人信息
	 * @param clearCache 在显示用户图像前，是否需要清除该图像的缓存，主要用于修改图像后的操作
	 * @author tiger
	 * @update 2015年3月15日 下午2:25:06
	 */
	private void showCurrentUserInfo(boolean clearCache) {
		/*Personal personal = ChatApplication.getInstance().getCurrentUser();
		if (personal != null) {
			String iconPath = personal.getIconPath();
			String iconUri = null;
			if (iconPath != null) {
				iconUri = Scheme.FILE.wrap(iconPath);
			}
			if (clearCache) {	//需要清除图片缓存
				//从缓存中删除该图像的内存缓存
				MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
			}
			String province = personal.getProvince();
			String city = personal.getCity();
			province = province == null ? "" : province;
			city = city == null ? "" : city;
			mImageLoader.displayImage(iconUri, ivHeadIcon, options);
			tvNick.setText(personal.getNickname());
			tvAccount.setText(personal.getUsername());
			tvSex.setText(Gender.valueOf(personal.getSex()).getName());
			tvAddress.setText(province + " " + city);
			tvSignature.setText(personal.getDesc());
		}*/
	}
	
	/**
	 * 接收个人信息更新的广播
	 * @author tiger
	 * @update 2015年3月15日 下午2:30:20
	 *
	 */
	public class PersonalInfoReceiver extends BasePersonalInfoReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			switch (action) {
			case ACTION_REFRESH_PERSONAL_INFO:	//更新个人信息
				showCurrentUserInfo(true);
				break;

			default:
				break;
			}
		}
		
	}

}
