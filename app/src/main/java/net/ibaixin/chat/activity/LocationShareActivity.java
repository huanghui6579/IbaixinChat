package net.ibaixin.chat.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import net.ibaixin.chat.R;
import net.ibaixin.chat.model.LocationInfo;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.view.ProgressWheel;

/**
 * 发送地理位置的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年1月5日 下午9:32:22
 */
public class LocationShareActivity extends BaseActivity implements OnGetGeoCoderResultListener, 
	BaiduMap.OnMapLoadedCallback, BaiduMap.SnapshotReadyCallback {
	
	/**
	 * 刷新菜单按钮的消息
	 */
	private static final int MSG_REFRESH_MENU = 0x1;
	
	/**
	 * 创建聊天消息对象
	 */
	private static final int MSG_CREATE_MSGINFO = 0x2;
	
	/**
	 * 创建聊天消息对象失败
	 */
	private static final int MSG_OPT_FAILED = 0x3;
	
	private BaiduMap mBaiduMap;

	// UI相关
//	boolean isFirstLoc = true;// 是否首次定位
	
	/**
	 * 我的 位置的经纬度
	 */
	private LatLng myLatLng;
	
	/**
	 * 地图支持的fragment
	 */
	private SupportMapFragment mMapFragment;
	private ImageButton btnMyLocation;
	private GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
	// 定位相关
	private LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	private List<LocationInfo> mLocationInfos = new ArrayList<>();
	private InfoWindow mInfoWindow;
	private Marker mMarker;
	private BitmapDescriptor mCurrentBitmap;
	
	/**
	 * 我的地理位置
	 */
	private BDLocation mLocation;

	private LocationAdapter mAdapter;
	
	private ListView lvData;
	
	private ProgressWheel pbLoading;
	
	/**
	 * 显示popup信息的view
	 */
	private TextView mPopuInfoView;
	
	/**
	 * 当前的地图是否加载完毕，只有当地图加载完毕后才可定位，否则会抛异常
	 */
	private boolean isMapLoaded = false;

//	private TextView btnOpt;
	
	private MenuItem mMenuDone;

	/**
	 * 当前选中的位置
	 */
	private int mCurrentPosition;
	
	private ProgressDialog mProgressDialog;
	
	/**
	 * 创建的消息对象
	 */
	private MsgInfo mMsgInfo;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			switch (msg.what) {
			case MSG_REFRESH_MENU:	//刷新菜单按钮
				if (mMenuDone != null && !mMenuDone.isEnabled()) {
					mMenuDone.setEnabled(true);
				}
				break;
			case MSG_CREATE_MSGINFO:	//创建聊天消息对象
				Intent data = new Intent();
				data.putExtra(ChatActivity.ARG_MSG_INFO, mMsgInfo);
				setResult(RESULT_OK, data);
				finish();
				break;
			case MSG_OPT_FAILED:	//创建消息对象失败
				SystemUtil.makeShortToast(R.string.chat_loction_opt_failed);
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected int getContentView() {
		return R.layout.activity_location_share;
	}

	@Override
	protected void initView() {
		if (mMapFragment == null) {
			mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		}
		lvData = (ListView) findViewById(R.id.lv_data);
		pbLoading = (ProgressWheel) findViewById(R.id.pb_loading);
		btnMyLocation = (ImageButton) findViewById(R.id.btn_my_location);
	}

	@Override
	protected void initData() {
		
		mMsgInfo = getIntent().getParcelableExtra(ChatActivity.ARG_MSG_INFO);
		
		//初始化百度地图
		setUpMap();
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
			mProgressDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
			mBaiduMap.snapshot(LocationShareActivity.this);
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(final BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			drawLocation(location);
		}
	}
	
	/**
	 * 绘制地理位置信息
	 * @update 2015年2月7日 下午3:34:00
	 * @param location
	 */
	private void drawLocation(final BDLocation location) {
		if (!isMapLoaded) {
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					drawLocation(location);
				}
			}, 500);
		} else {
			if (location == null || mBaiduMap == null) {
				return;
			}
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			mHandler.sendEmptyMessage(MSG_REFRESH_MENU);	//刷新菜单按钮
			mLocation = location;
			LatLng ll = new LatLng(location.getLatitude(),
					location.getLongitude());
			myLatLng = ll;
			MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
			mBaiduMap.animateMapStatus(u);
			// 反Geo搜索
			mSearch.reverseGeoCode(new ReverseGeoCodeOption()
					.location(ll));
		}
	}
	
	/**
	 * 设置map对象的相关参数
	 * @update 2015年1月5日 下午9:59:30
	 */
	private void setUpMap() {
		if (mBaiduMap == null) {
			mBaiduMap = mMapFragment.getBaiduMap();
		}
		
		MapView mapView = mMapFragment.getMapView();
		//去掉缩放控件
		mapView.showZoomControls(false);

		// 初始化搜索模块，注册事件监听
		mSearch = GeoCoder.newInstance();
		mSearch.setOnGetGeoCodeResultListener(this);
		
		//地图加载完毕的监听器
		mBaiduMap.setOnMapLoadedCallback(this);
		
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
		
		// 定位初始化
		mLocClient = new LocationClient(this);
		mLocClient.registerLocationListener(myListener);
		//设置定位选项
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开gps
		option.setCoorType("bd09ll"); // 设置坐标类型
//		option.setScanSpan(2000);	//定位的间隔时间,单位毫秒
		option.setIsNeedAddress(true);
		mLocClient.setLocOption(option);
		mLocClient.start();
	}

	@Override
	protected void addListener() {
		btnMyLocation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(myLatLng);
				mBaiduMap.animateMapStatus(u);
			}
		});
		
		mBaiduMap.setOnMyLocationClickListener(new BaiduMap.OnMyLocationClickListener() {
			
			@Override
			public boolean onMyLocationClick() {
				if (mLocation != null) {
					if (mInfoWindow != null) {
						mBaiduMap.hideInfoWindow();
					}
					showInfoWindow(getString(R.string.my_location_prompt, mLocation.getAddrStr()), myLatLng);
					return true;
				}
				return false;
			}
		});
		
		mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
			
			@Override
			public boolean onMarkerClick(Marker marker) {
				showInfoWindow(marker.getTitle(), marker.getPosition());
				return true;
			}
		});
		lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position != 0 && position != mCurrentPosition) {
					mCurrentPosition = position;
					LocationInfo locationInfo = mLocationInfos.get(position);
					LatLng location = new LatLng(locationInfo.getLatitude(), locationInfo.getLongitude());
					if (mCurrentBitmap == null) {
						mCurrentBitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_markers);
					}
					OverlayOptions ooA = new MarkerOptions().position(location).icon(mCurrentBitmap)
							.zIndex(9);
					mBaiduMap.clear();
					mMarker = (Marker) mBaiduMap.addOverlay(ooA);
					mMarker.setTitle(locationInfo.getAddress());
					mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(location));
				}
			}
		});
	}
	
	/**
	 * 显示popupwindow信息
	 * @update 2015年2月7日 下午5:04:36
	 * @param msg
	 * @param latLng
	 */
	private void showInfoWindow(String msg, LatLng latLng) {
		if (mPopuInfoView == null) {
			mPopuInfoView = new TextView(getApplicationContext());
			mPopuInfoView.setBackgroundResource(R.drawable.location_popu_info_bg_normal);
			mPopuInfoView.setTextColor(Color.WHITE);
			mPopuInfoView.setText(msg);
		}
		mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(mPopuInfoView), latLng, -40, new InfoWindow.OnInfoWindowClickListener() {
			
			@Override
			public void onInfoWindowClick() {
				// TODO Auto-generated method stub
				mBaiduMap.hideInfoWindow();
			}
		});
		mBaiduMap.showInfoWindow(mInfoWindow);
	}

	@Override
	public void onMapLoaded() {
		isMapLoaded = true;
	}
	
	@Override
	protected void onDestroy() {
		if (mCurrentBitmap != null) {
			mCurrentBitmap.recycle();
		}
		// 退出时销毁定位
		mLocClient.stop();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		super.onDestroy();
	}

	@Override
	public void onGetGeoCodeResult(GeoCodeResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
		if (result != null && result.error == SearchResult.ERRORNO.NO_ERROR) {
			if (mLocationInfos.size() > 0) {
				mLocationInfos.clear();
			}
			LatLng location = result.getLocation();
			LocationInfo info = new LocationInfo(location.latitude, location.longitude, result.getAddress());
			mLocationInfos.add(info);
			List<PoiInfo> pois = result.getPoiList();
			if (pois != null && pois.size() > 0) {
				for (PoiInfo poiInfo : pois) {
					location = poiInfo.location;
					info = new LocationInfo(location.latitude, location.longitude, poiInfo.address + poiInfo.name);
					mLocationInfos.add(info);
				}
			}
			if (mAdapter == null) {
				mAdapter = new LocationAdapter(mLocationInfos, getApplicationContext());
				lvData.setAdapter(mAdapter);
				lvData.setItemChecked(0, true);	//第一位默认选中
			} else {
				mAdapter.notifyDataSetChanged();
			}
			if (pbLoading.getVisibility() == View.VISIBLE) {
				pbLoading.setVisibility(View.GONE);
			}
		}
	}
	
	/**
	 * 位置适配器
	 * @author huanghui1
	 * @update 2015年1月6日 上午10:06:46
	 */
	class LocationAdapter extends CommonAdapter<LocationInfo> {

		public LocationAdapter(List<LocationInfo> list, Context context) {
			super(list, context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LocationViewHolder holder = null;
			if (convertView == null) {
				holder = new LocationViewHolder();
				convertView = inflater.inflate(R.layout.item_list_single_choice, parent, false);
				
				holder.checkedTextView = (CheckedTextView) convertView.findViewById(R.id.tv_content);
				convertView.setTag(holder);
			} else {
				holder = (LocationViewHolder) convertView.getTag();
			}
			
			final LocationInfo locationInfo = list.get(position);
			
			holder.checkedTextView.setText(locationInfo.getAddress());
			
			return convertView;
		}
		
	}

	final class LocationViewHolder {
		CheckedTextView checkedTextView;
	}

	@Override
	public void onSnapshotReady(final Bitmap snapshot) {
		if (mMsgInfo != null) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					long time = System.currentTimeMillis();
					String filername = SystemUtil.generateChatAttachFilename(time);
//					File file = SystemUtil.generateLocationFile(mMsgInfo.getThreadID());
					File file = SystemUtil.generateChatAttachFile(mMsgInfo.getThreadID(), filername);
					try {
						boolean success = ImageUtil.createLocationFile(snapshot, file);
						if (success) {
							LocationInfo locationInfo = mLocationInfos.get(mCurrentPosition);
							mMsgInfo.setContent(locationInfo.getAddress());	//地理位置名称
							mMsgInfo.setMsgType(MsgInfo.Type.LOCATION);
							mMsgInfo.setCreationDate(time);
							//设置附件信息
							MsgPart msgPart = new MsgPart();
							msgPart.setCreationDate(System.currentTimeMillis());
							msgPart.setFileName(filername);
							msgPart.setFilePath(file.getAbsolutePath());
							msgPart.setMimeType(MimeUtils.MIME_TYPE_IMAGE_JPG);
							msgPart.setSize(file.length());
							msgPart.setDesc(locationInfo.getLatitude() + Constants.SPLITE_TAG_LOCATION + locationInfo.getLongitude());	//地理位置的经纬度
							
							mMsgInfo.setMsgPart(msgPart);
							
							mHandler.sendEmptyMessage(MSG_CREATE_MSGINFO);
						} else {
							mHandler.sendEmptyMessage(MSG_OPT_FAILED);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						if (snapshot != null) {
							snapshot.recycle();
						}
					}
				}
			}).start();
		}
	}
}
