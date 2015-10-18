package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 地理信息
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月28日 下午4:13:23
 */
public class GeoInfo implements Cloneable, Parcelable {
	/**
	 * 0:普通项
	 */
	public static final int VIEW_TYPE_ITEM = 0;
	/**
	 * 1:默认选择项
	 */
	public static final int VIEW_TYPE_CHECK = 1;
	/**
	 * 2：定位视图
	 */
	public static final int VIEW_TYPE_LOCATE = 2;
	/**
	 * 试图的类型，主要有3类，0:普通项,1:默认选择项，2：定位视图
	 */
	private int viewType;
	/**
	 * 显示的内容
	 */
	private String content;

	/**
	 * 国家
	 */
	private String country;

	/**
	 * 国家编号
	 */
	private int countryId;

	/**
	 * 省份
	 */
	private String province;

	/**
	 * 省份编号
	 */
	private int provinceId;

	/**
	 * 城市
	 */
	private String city;

	/**
	 * 城市编号
	 */
	private int cityId;
	/**
	 * 当前项是否选中
	 */
	private boolean checked;
	
	/**
	 * 是否有下级
	 */
	private boolean hasChildren;

	public int getViewType() {
		return viewType;
	}

	public void setViewType(int viewType) {
		this.viewType = viewType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public int getCountryId() {
		return countryId;
	}

	public void setCountryId(int countryId) {
		this.countryId = countryId;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public int getProvinceId() {
		return provinceId;
	}

	public void setProvinceId(int provinceId) {
		this.provinceId = provinceId;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public int getCityId() {
		return cityId;
	}

	public void setCityId(int cityId) {
		this.cityId = cityId;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}
	
	public boolean hasChildren() {
		return hasChildren;
	}

	public void setHasChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}

	@Override
	public GeoInfo clone() throws CloneNotSupportedException {
		return (GeoInfo) super.clone();
	}
	
	/**
	 * 获取国家、省、市的内容
	 * @return
	 * @update 2015年9月3日 下午4:25:18
	 */
	public String toGeneralString() {
		StringBuilder builder = new StringBuilder();
		if (country != null) {
			builder.append(country);
		}
		if (province != null) {
			builder.append(" ").append(province);
		}
		if (city != null) {
			builder.append(" ").append(city);
		}
		return builder.toString();
	}
	
	/**
	 * 获取省、市的内容,若没有省、市，则添加国家
	 * @return
	 * @update 2015年9月6日 下午3:16:43
	 */
	public String toGeneralSimpleString() {
		StringBuilder builder = new StringBuilder();
		boolean hasProvince = false;
		if (province != null) {
			builder.append(" ").append(province);
			hasProvince = true;
		}
		boolean hasCity = false;
		if (city != null) {
			builder.append(" ").append(city);
			hasCity = true;
		}
		String str = builder.toString();
		if (!hasProvince && !hasCity) {
			str = country;
		}
		return str;
	}

	@Override
	public String toString() {
		return "GeoInfo [viewType=" + viewType + ", content=" + content + ", country=" + country + ", countryId="
				+ countryId + ", province=" + province + ", provinceId=" + provinceId + ", city=" + city + ", cityId="
				+ cityId + ", checked=" + checked + ", hasChildren=" + hasChildren + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	public GeoInfo() {}
	
	public GeoInfo(Parcel in) {
		viewType = in.readInt();
		content = in.readString();
		country = in.readString();
		countryId = in.readInt();
		province = in.readString();
		provinceId = in.readInt();
		city = in.readString();
		cityId = in.readInt();
		checked = in.readInt() == 1 ? true : false;
		hasChildren = in.readInt() == 1 ? true : false;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(viewType);
		dest.writeString(content);
		dest.writeString(country);
		dest.writeInt(countryId);
		dest.writeString(province);
		dest.writeInt(provinceId);
		dest.writeString(city);
		dest.writeInt(cityId);
		dest.writeInt(checked ? 1 : 0);
		dest.writeInt(hasChildren ? 1 : 0);
	}
	
	public static final Creator<GeoInfo> CREATOR = new Creator<GeoInfo>() {
		
		@Override
		public GeoInfo[] newArray(int size) {
			return new GeoInfo[size];
		}
		
		@Override
		public GeoInfo createFromParcel(Parcel source) {
			return new GeoInfo(source);
		}
	};
}
