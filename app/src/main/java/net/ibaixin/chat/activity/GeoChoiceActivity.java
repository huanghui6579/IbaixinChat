package net.ibaixin.chat.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.GeoChoiceFragment;
import net.ibaixin.chat.model.GeoInfo;
import net.ibaixin.chat.model.Personal;

/**
 * 地区选择界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月28日 下午4:05:33
 */
public class GeoChoiceActivity extends BaseActivity {
	
	@Override
	protected int getContentView() {
		return R.layout.activity_container;
	}

	@Override
	protected void initView() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void initData() {
		Personal personal = ((ChatApplication) getApplication()).getCurrentUser();
		Bundle args = new Bundle();
		if (personal != null) {
			GeoInfo geoInfo = new GeoInfo();
			geoInfo.setCity(personal.getCity());
			geoInfo.setCityId(personal.getCityId());
			geoInfo.setProvince(personal.getProvince());
			geoInfo.setProvinceId(personal.getProvinceId());
			geoInfo.setCountry(personal.getCountry());
			geoInfo.setCountryId(personal.getCountryId());
			
			args.putParcelable(GeoChoiceFragment.ARG_GEOINFO, geoInfo);
			args.putInt(GeoChoiceFragment.ARG_GEO_LEVEL, GeoChoiceFragment.GEO_LEVEL_COUNTRY);
		}
		GeoChoiceFragment choiceFragment = GeoChoiceFragment.newInstance(args);
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
			.add(R.id.main_container, choiceFragment)
			.commit();
	}

	@Override
	protected void addListener() {
		// TODO Auto-generated method stub

	}
}
