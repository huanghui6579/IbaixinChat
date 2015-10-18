package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 地理位置信息，主要存放经纬度和位置的名称、地图搜索的半径，以及截屏后保存的本地路径
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年1月5日 下午10:07:12
 */
public class LocationInfo implements Parcelable {
	/**
	 * 纬度
	 */
	private double latitude;
	
	/**
	 * 经度
	 */
	private double longitude;
	
	/**
	 * 地图查询的半径
	 */
	private float radius;
	
	/**
	 * 地理名称
	 */
	private String address;
	
	/**
	 * 保存的截图路径
	 */
	private String filePath;

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		return "LocationInfo [latitude=" + latitude + ", longitude="
				+ longitude + ", radius=" + radius + ", address=" + address
				+ ", filePath=" + filePath + "]";
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeFloat(radius);
		dest.writeString(address);
		dest.writeString(filePath);
	}
	
	public LocationInfo(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
		radius = in.readFloat();
		address = in.readString();
		filePath = in.readString();
	}

	public LocationInfo(double latitude, double longitude, float radius,
			String address) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.radius = radius;
		this.address = address;
	}

	public LocationInfo(double latitude, double longitude, String address) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.address = address;
	}

	public LocationInfo() {}
	
	public static final Creator<LocationInfo> CREATOR = new Creator<LocationInfo>() {
		
		@Override
		public LocationInfo[] newArray(int size) {
			return new LocationInfo[size];
		}
		
		@Override
		public LocationInfo createFromParcel(Parcel source) {
			return new LocationInfo(source);
		}
	};
}
