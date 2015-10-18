package net.ibaixin.chat.activity;

import net.ibaixin.chat.R;
import net.ibaixin.chat.util.SystemUtil;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.baidu.mapapi.model.LatLng;

/**
 * 显示地理位置的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月4日 下午3:46:31
 */
public class LocationShowActivity extends BaseActivity implements BaiduMap.OnMapLoadedCallback {
	
	/**
	 * 纬度的参数
	 */
	public static final String ARG_LATITUDE = "arg_latitude";
	/**
	 * 经度的参数
	 */
	public static final String ARG_LONGITUDE = "arg_longitude";
	
	public static final String ARG_LOCATION_INFO = "arg_location_info";
	
	/**
	 * 要显示的地理经纬度
	 */
	private LatLng mLatLng;
	
	/**
	 * 地图对象
	 */
	private MapView mMapView;
	
	private BaiduMap mBaiduMap;
	
	private ImageButton btnMyLocation;
	
	/**
	 * 地理位置提示信息
	 */
	private InfoWindow mInfoWindow;
	
	/**
	 * 显示popup信息的view
	 */
	private TextView mPopuInfoView;
	private BitmapDescriptor mCurrentBitmap;
	private Marker mMarker;

	@Override
	protected int getContentView() {
		return R.layout.activity_show_location;
	}

	@Override
	protected void initView() {
		mMapView = (MapView) findViewById(R.id.mapview);
		btnMyLocation = (ImageButton) findViewById(R.id.btn_my_location);
	}

	@Override
	protected void initData() {
		setUpMap();
		
		Intent intent = getIntent();
		if (intent != null) {
			double latitude = intent.getDoubleExtra(ARG_LATITUDE, 0.00);
			double longitude = intent.getDoubleExtra(ARG_LONGITUDE, 0.00);
			String locationInfo = intent.getStringExtra(ARG_LOCATION_INFO);
			mLatLng = new LatLng(latitude, longitude);
			MyLocationData locData = new MyLocationData.Builder()
				// 此处设置开发者获取到的方向信息，顺时针0-360
				.direction(100).latitude(latitude)
				.longitude(longitude)
				.build();
			mBaiduMap.setMyLocationData(locData);
			
			if (mCurrentBitmap == null) {
				mCurrentBitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_markers);
			}
			OverlayOptions ooA = new MarkerOptions().position(mLatLng).icon(mCurrentBitmap)
					.zIndex(9);
			mBaiduMap.clear();
			mMarker = (Marker) mBaiduMap.addOverlay(ooA);
			mMarker.setTitle(locationInfo);
			
			MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(mLatLng);
			mBaiduMap.animateMapStatus(u);
			
			if (mInfoWindow != null) {
				mBaiduMap.hideInfoWindow();
			}
			if (!TextUtils.isEmpty(locationInfo)) {
				showInfoWindow(locationInfo, mLatLng);
			}
		}
	}
	
	@Override
	protected void onResume() {
		mMapView.onResume();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		mMapView.onDestroy();
		if (mCurrentBitmap != null) {
			mCurrentBitmap.recycle();
		}
		super.onDestroy();
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
	
	/**
	 * 设置map对象的相关参数
	 * @update 2015年3月4日 下午4:25:34
	 */
	private void setUpMap() {
		if (mBaiduMap == null) {
			mBaiduMap = mMapView.getMap();
		}
		//去掉缩放控件
		mMapView.showZoomControls(false);
		mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
		//地图加载完毕的监听器
		mBaiduMap.setOnMapLoadedCallback(this);
	}

	@Override
	protected void addListener() {
		btnMyLocation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mLatLng != null) {
					MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(mLatLng);
					mBaiduMap.animateMapStatus(u);
				} else {
					SystemUtil.makeShortToast(R.string.show_location_error);
				}
			}
		});
		mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
			
			@Override
			public boolean onMarkerClick(Marker marker) {
				showInfoWindow(marker.getTitle(), marker.getPosition());
				return true;
			}
		});
	}

	@Override
	public void onMapLoaded() {
		// TODO Auto-generated method stub
		
	}

}
