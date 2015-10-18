package net.ibaixin.chat.fragment;

import java.util.ArrayList;
import java.util.List;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.CommonAdapter;
import net.ibaixin.chat.activity.GeoChoiceActivity;
import net.ibaixin.chat.manager.GeoInfoManager;
import net.ibaixin.chat.model.GeoInfo;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.util.SystemUtil;

/**
 * 地区选择的界面,内嵌于{@link GeoChoiceActivity}中
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月28日 下午4:38:22
 */
public class GeoChoiceFragment extends BaseFragment {
	public static final String ARG_GEOINFO = "arg_geoinfo";
	public static final String ARG_GEO_LEVEL = "arg_geo_level";
	
	/**
	 * 显示的行政区划的级别，国家级：0
	 */
	public static final int GEO_LEVEL_COUNTRY = 0;
	/**
	 * 市级:2
	 */
	public static final int GEO_LEVEL_PROVINCE = 1;
	/**
	 * 市级:2
	 */
	public static final int GEO_LEVEL_CITY = 2;
	
	private ListView mLvData;
	private Toolbar mToolbar;
	private List<GeoInfo> mGeoInfos = new ArrayList<>();
	
	private GeoInfoAdapter mGeoInfoAdapter;
	
	/**
	 * 当前的geoinfo
	 */
	private GeoInfo mGeoInfo;
	
	/**
	 * 当前用户的geo
	 */
	private GeoInfo mDefaultGeoInfo;

	// 定位相关
	private LocationClient mLocClient;
	public MyLocationListenner mLocationListenner = new MyLocationListenner();
	
	/**
	 * 显示的行政区划的级别，默认为国家级：0，省级：1，市级:2
	 */
	private int mGeoLevel = GEO_LEVEL_COUNTRY;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	/**
	 * 生成一个实例
	 * @param args
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年9月5日 上午11:37:33
	 */
	public static GeoChoiceFragment newInstance(Bundle args) {
		GeoChoiceFragment fragment = new GeoChoiceFragment(args);
		return fragment;
	}
	
	public GeoChoiceFragment(Bundle args) {
		if (args == null) {
			args = new Bundle();
		} else {
			mGeoInfo = args.getParcelable(ARG_GEOINFO);
			mGeoLevel = args.getInt(ARG_GEO_LEVEL, GEO_LEVEL_COUNTRY);
		}
		setArguments(args);
	}
	
	public GeoChoiceFragment() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_geo_choice, container, false);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mLvData = (ListView) view.findViewById(R.id.lv_data);
		mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
		mToolbar.setTitle(getActivity().getTitle());
		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				fragmentManager.popBackStack();
				fragmentTransaction.commit();
				int stackCount = fragmentManager.getBackStackEntryCount();
				if (stackCount == 0) {	//没有其他的返回栈了，就结束activity
					getActivity().finish();
				} 
			}
		});
		
		/*mLvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				GeoInfo geoInfo = mGeoInfos.get(position);
				if (geoInfo != null) {
					switch (mGeoLevel) {
					case GEO_LEVEL_COUNTRY:	//国家级
						if (geoInfo.hasChildren()) {	//有下一级就进入
							SystemUtil.makeShortToast("进入下级目录....");
						} else {	//直接选择
							SystemUtil.makeShortToast("返回结果" + geoInfo.toGeneralString());
							//finish该fragment所在的activity
						}
						break;

					default:
						break;
					}
				}
			}
		});*/
		
		/*btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				GeoChoiceFragment fragment = new GeoChoiceFragment(args);
				fragmentManager.beginTransaction()
					.setCustomAnimations(R.anim.slide_right_in, 0, 0, R.anim.slide_right_out)
					.add(R.id.main_container, fragment, "tag" + tagIndex)
					.addToBackStack(null)
					.commit();
			}
		});*/
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (mGeoLevel == GEO_LEVEL_COUNTRY) {	//只有国家级才定位
			initLocateSdk();
		}
		Personal personal = ((ChatApplication) getActivity().getApplication()).getCurrentUser();
		if (personal != null) {
			mDefaultGeoInfo = personal.getGeoinfo();
		}
		new LoadGeoTask().execute(mGeoInfo);
	}
	
	@Override
	public void onDestroyView() {
		// 退出时销毁定位
		if (mLocClient != null) {
			mLocClient.stop();
		}
		super.onDestroyView();
	}
	
	/**
	 * 列表每一项的点击接口
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年9月5日 上午10:53:46
	 */
	class OnListItemClickListener implements View.OnClickListener {
		private int position;
		private View parent;
		private GeoInfo geoInfo;
		
		public OnListItemClickListener(GeoInfo geoInfo, int position, View parent) {
			super();
			this.geoInfo = geoInfo;
			this.position = position;
			this.parent = parent;
		}

		@Override
		public void onClick(View v) {
			if (geoInfo != null) {
				if (geoInfo.hasChildren()) {	//有下一级就进入
					FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
					Bundle args = new Bundle();
					args.putParcelable(ARG_GEOINFO, geoInfo);
					int level = mGeoLevel + 1;
					level = level > GEO_LEVEL_CITY ? GEO_LEVEL_CITY : level;
					args.putInt(ARG_GEO_LEVEL, level);
					GeoChoiceFragment fragment = new GeoChoiceFragment(args);
					fragmentManager.beginTransaction()
						.setCustomAnimations(R.anim.slide_right_in, 0, 0, R.anim.slide_right_out)
						.add(R.id.main_container, fragment)
						.addToBackStack(null)
						.commit();
				} else {	//直接选择
					Intent data = new Intent();
					data.putExtra(ARG_GEOINFO, geoInfo);
					getActivity().setResult(Activity.RESULT_OK, data);
					getActivity().finish();
					//finish该fragment所在的activity
				}
			}
		}
	}
	
	/**
	 * 初始化百度定位参数
	 * 
	 * @update 2015年9月2日 上午10:03:28
	 */
	private void initLocateSdk() {
		// 定位初始化
		mLocClient = new LocationClient(mContext);
		mLocClient.registerLocationListener(mLocationListenner);
		//设置定位选项
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开gps
		option.setCoorType("bd09ll"); // 设置坐标类型
//		option.setScanSpan(3000);	//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setIsNeedAddress(true);
		option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
		mLocClient.setLocOption(option);
	}
	
	/**
	 * 定位SDK监听函数
	 */
	class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(final BDLocation location) {
			String country = location.getCountry();
			String province = location.getProvince();
			String city = location.getCity();
			
			GeoInfo info = mGeoInfos.get(0);
			info.setCountry(country);
			info.setProvince(province);
			info.setCity(city);
			info.setViewType(GeoInfo.VIEW_TYPE_LOCATE);
			
			GeoInfoManager infoManager = new GeoInfoManager();
			GeoInfo tmpInfo = infoManager.geoReCoder(info, true);
			if (tmpInfo != null) {
				info = tmpInfo;
				String tCountry = info.getCountry();
				String tProvince = info.getProvince();
				String tCity = info.getCity();
				if (TextUtils.isEmpty(tCountry) && TextUtils.isEmpty(tProvince) && TextUtils.isEmpty(tCity)) {	//定位失败
					info.setContent(getResources().getString(R.string.geo_locate_error));
				} else {
					StringBuilder content = new StringBuilder();
					content.append(tCountry).append(" ")
						.append(tProvince).append(" ")
						.append(tCity);
					info.setContent(content.toString());
				}
				notifyDataSetChanged(info, 0);
			}
			infoManager.closeDb();
//			Log.d("----当前位置：----国家：--" + location.getCountry() + "---国家编号---" + location.getCountryCode() + "-----省份--" + location.getProvince() + "---城市---" + location.getCity() + "-----城市编号：----" + location.getCityCode() + "--getAddrStr---" + location.getAddrStr() + "--getDistrict---" + location.getDistrict() + "---getStreet--" + location.getStreet());
		}
	}
	
	/**
	 * 局部更新list列表数据
	 * @param geoInfo 要更新的数据
	 * @param index 更新的索引位置，从0开始
	 * @update 2015年9月2日 下午4:39:45
	 */
	private void notifyDataSetChanged(GeoInfo geoInfo, int index) {
		//得到第一个可显示控件的位置，  
        int visiblePosition = mLvData.getFirstVisiblePosition();
        //只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新 
        int relativePosition = index - visiblePosition;
        View rootView = null;
        String content = geoInfo.getContent();
		switch (geoInfo.getViewType()) {
		case GeoInfo.VIEW_TYPE_LOCATE:	//更新定位信息
			rootView = mLvData.getChildAt(relativePosition);
			if (rootView != null) {
				GeoLocateViewHolder locateViewHolder = (GeoLocateViewHolder) rootView.getTag();
				locateViewHolder.mTvLocation.setText(content);
			} else {
				mGeoInfoAdapter.notifyDataSetChanged();
			}
			break;
		case GeoInfo.VIEW_TYPE_CHECK:	//默认选择
			rootView = mLvData.getChildAt(relativePosition);
			if (rootView != null) {
				GeoCheckViewHolder checkViewHolder = (GeoCheckViewHolder) rootView.getTag();
				checkViewHolder.mTvChecked.setText(content);
			} else {
				mGeoInfoAdapter.notifyDataSetChanged();
			}
			break;
		case GeoInfo.VIEW_TYPE_ITEM:	//每一项
			rootView = mLvData.getChildAt(relativePosition);
			if (rootView != null) {
				GeoItemViewHolder itemViewHolder = (GeoItemViewHolder) rootView.getTag();
				itemViewHolder.mTvContent.setText(content);
			} else {
				mGeoInfoAdapter.notifyDataSetChanged();
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * 根据上层的geoinfo来加载下级的geoinfo列表
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月31日 下午2:47:13
	 */
	class LoadGeoTask extends AsyncTask<GeoInfo, Void, List<GeoInfo>> {

		@Override
		protected List<GeoInfo> doInBackground(GeoInfo... params) {
			GeoInfoManager geoManager = new GeoInfoManager();
			GeoInfo parentGeo = null;
			if (params != null && params.length > 0) {
				parentGeo = params[0];
			}
			List<GeoInfo> list = null;
			switch (mGeoLevel) {
			case GEO_LEVEL_COUNTRY:	//显示国家
				list = geoManager.getCountries(parentGeo);
				if (list == null) {
					list = new ArrayList<>();
				}
				//添加定位项
				GeoInfo locateInfo = new GeoInfo();
				locateInfo.setViewType(GeoInfo.VIEW_TYPE_LOCATE);
				list.add(0, locateInfo);
				break;
			case GEO_LEVEL_PROVINCE:	//显示省份
				if (mDefaultGeoInfo != null) {
					parentGeo.setProvince(mDefaultGeoInfo.getProvince());
					parentGeo.setProvinceId(mDefaultGeoInfo.getProvinceId());
				}
				list = geoManager.getProvinces(parentGeo);
				break;
			case GEO_LEVEL_CITY:	//显示城市
				if (mDefaultGeoInfo != null) {
					parentGeo.setCity(mDefaultGeoInfo.getCity());
					parentGeo.setCityId(mDefaultGeoInfo.getCityId());
				}
				list = geoManager.getCities(parentGeo);
				break;
			default:
				break;
			}
			geoManager.closeDb();
			return list;
		}
		
		@Override
		protected void onPostExecute(List<GeoInfo> result) {
			if (SystemUtil.isNotEmpty(result)) {
				mGeoInfos.clear();
				mGeoInfos.addAll(result);
				if (mGeoInfoAdapter == null) {
					mGeoInfoAdapter = new GeoInfoAdapter(mGeoInfos, mContext);
					mLvData.setAdapter(mGeoInfoAdapter);
				} else {
					mGeoInfoAdapter.notifyDataSetChanged();
				}
				
				if (mLocClient != null) {
					mLocClient.start();
				}
			}
		}
		
	}
	
	/**
	 * 行政区划的adapter
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月31日 下午8:35:13
	 */
	class GeoInfoAdapter extends CommonAdapter<GeoInfo> {
		private int viewTypeCount = 3;

		public GeoInfoAdapter(List<GeoInfo> list, Context context) {
			super(list, context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int viewType = getItemViewType(position);
			GeoInfo geoInfo = list.get(position);
			String content = null;
			int contentId = -1;
			switch (mGeoLevel) {
			case GEO_LEVEL_COUNTRY:	//显示国家级数据
				if (viewType == GeoInfo.VIEW_TYPE_LOCATE) {	//定位项
					content = geoInfo.toGeneralString();
				} else {
					content = geoInfo.getCountry();
				}
				contentId = geoInfo.getCountryId();
				break;
			case GEO_LEVEL_PROVINCE:	//省级
				content = geoInfo.getProvince();
				contentId = geoInfo.getProvinceId();
				break;
			case GEO_LEVEL_CITY:	//市级
				content = geoInfo.getCity();
				contentId = geoInfo.getCityId();
				break;
			default:
				break;
			}
			
			GeoItemViewHolder itemViewHolder = null;
			GeoCheckViewHolder checkViewHolder = null;
			GeoLocateViewHolder locateViewHolder = null;
			if (convertView == null) {
				switch (viewType) {
				case GeoInfo.VIEW_TYPE_ITEM:	//普通项
					convertView = inflater.inflate(R.layout.item_geo_info, parent, false);
					itemViewHolder = new GeoItemViewHolder();
					itemViewHolder.mTvContent = (TextView) convertView.findViewById(R.id.tv_content);
					convertView.setTag(itemViewHolder);

					itemViewHolder.mTvContent.setText(content);
					itemViewHolder.mTvContent.setOnClickListener(new OnListItemClickListener(geoInfo, position, parent));
					break;
				case GeoInfo.VIEW_TYPE_CHECK:	//当前选择项
					convertView = inflater.inflate(R.layout.item_geo_checked, parent, false);
					checkViewHolder = new GeoCheckViewHolder();
					checkViewHolder.mTvChecked = (TextView) convertView.findViewById(R.id.tv_content);
					checkViewHolder.contentLayout = convertView.findViewById(R.id.content_layout);
					convertView.setTag(checkViewHolder);
					
					checkViewHolder.mTvChecked.setText(content);
					checkViewHolder.contentLayout.setOnClickListener(new OnListItemClickListener(geoInfo, position, parent));
					break;
				case GeoInfo.VIEW_TYPE_LOCATE:	//定位
					convertView = inflater.inflate(R.layout.item_geo_locate, parent, false);
					locateViewHolder = new GeoLocateViewHolder();
					locateViewHolder.mTvLocation = (TextView) convertView.findViewById(R.id.tv_content);
					convertView.setTag(locateViewHolder);
					if (TextUtils.isEmpty(content)) {
						content = getString(R.string.geo_locating);
					}
					locateViewHolder.mTvLocation.setText(content);
					locateViewHolder.mTvLocation.setOnClickListener(new OnListItemClickListener(geoInfo, position, parent));
					break;
				default:
					break;
				}
			} else {
				switch (viewType) {
				case GeoInfo.VIEW_TYPE_ITEM:	//普通项
					itemViewHolder = (GeoItemViewHolder) convertView.getTag();
					itemViewHolder.mTvContent.setText(content);
					itemViewHolder.mTvContent.setOnClickListener(new OnListItemClickListener(geoInfo, position, parent));
					break;
				case GeoInfo.VIEW_TYPE_CHECK:	//当前选择项
					checkViewHolder = (GeoCheckViewHolder) convertView.getTag();
					checkViewHolder.mTvChecked.setText(content);
					checkViewHolder.mTvChecked.setOnClickListener(new OnListItemClickListener(geoInfo, position, parent));
					break;
				case GeoInfo.VIEW_TYPE_LOCATE:	//定位
					if (TextUtils.isEmpty(content)) {
						content = getString(R.string.geo_locating);
					}
					locateViewHolder = (GeoLocateViewHolder) convertView.getTag();
					locateViewHolder.mTvLocation.setText(content);
					locateViewHolder.mTvLocation.setOnClickListener(new OnListItemClickListener(geoInfo, position, parent));
					break;

				default:
					break;
				}
			}
			return convertView;
		}
		
		@Override
		public int getViewTypeCount() {
			return viewTypeCount;
		}
		
		@Override
		public int getItemViewType(int position) {
			GeoInfo geoInfo = list.get(position);
			return geoInfo.getViewType();
		}
		
	}
	
	/**
	 * 定位的viewholder
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月31日 下午8:03:58
	 */
	final class GeoLocateViewHolder {
		TextView mTvLocation;
	}
	
	/**
	 * 地区默认选择的viewholder
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月31日 下午8:05:36
	 */
	final class GeoCheckViewHolder {
		TextView mTvChecked;
		View contentLayout;
	}
	
	/**
	 * 地区的item项的viewholder
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月31日 下午8:06:18
	 */
	final class GeoItemViewHolder {
		TextView mTvContent;
	}
}
