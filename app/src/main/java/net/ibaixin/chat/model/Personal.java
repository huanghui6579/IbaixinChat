package net.ibaixin.chat.model;

import net.ibaixin.chat.util.Constants;

import org.jxmpp.util.XmppStringUtils;

import android.text.TextUtils;

/**
 * 个人信息
 * 
 * @author coolpad
 *
 */
public class Personal {
	/**
	 * 主键id
	 */
	private int id;
	/**
	 * 个人的账号
	 */
	private String username;
	/**
	 * 个人的密码
	 */
	private String password;
	/**
	 * 个人的昵称
	 */
	private String nickname;
	/**
	 * 个人真实姓名
	 */
	private String realName;
	/**
	 * 个人邮箱
	 */
	private String email;
	/**
	 * 个人手机号码
	 */
	private String phone;
	/**
	 * 个人的登录资源，像QQ一样，用什么设备登录的，如Android、iPhone、web
	 */
	private String resource;
	/**
	 * 在个人状态的基础上的签名，在线时标记“吃饭中”等动态信息
	 */
	private String status;
	/**
	 * 个人登录的状态，如隐身、在线、空闲等等
	 */
	private String mode;
	/**
	 * 个人具体的街道地址
	 */
	private String street;
	/**
	 * 个人所在的城市
	 */
	private String city;
	/**
	 * 城市id
	 */
	private int cityId;
	/**
	 * 个人所在的省份
	 */
	private String province;
	/**
	 * 省份id
	 */
	private int provinceId;
	/**
	 * 国家
	 */
	private String country;
	/**
	 * 国家id
	 */
	private int countryId;
	/**
	 * 个人所在地址的邮编
	 */
	private String zipCode;
	/**
	 * 个人的头像本地存储路径
	 */
	private String iconPath;
	/**
	 * 头像缩略图的位置
	 */
	private String thumbPath;
	/**
	 * 头像的hash值，通过本地hash值与服务器的hash对比来判断用不用重新更新头像
	 */
	private String iconHash;
	/**
	 * 头像的mime类型
	 */
	private String mimeType;
	
	/**
	 * 性别，1表示男，2表示女，0表示未知，默认为0(未知)
	 */
	private int sex = 0;
	
	/**
	 * 描述，类似于个性签名
	 */
	private String desc;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getIconPath() {
		return iconPath;
	}

	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}
	
	public String getIconHash() {
		return iconHash;
	}

	public void setIconHash(String iconHash) {
		this.iconHash = iconHash;
	}
	
	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getThumbPath() {
		return thumbPath;
	}

	public void setThumbPath(String thumbPath) {
		this.thumbPath = thumbPath;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public int getCityId() {
		return cityId;
	}

	public void setCityId(int cityId) {
		this.cityId = cityId;
	}

	public int getProvinceId() {
		return provinceId;
	}

	public void setProvinceId(int provinceId) {
		this.provinceId = provinceId;
	}

	public int getCountryId() {
		return countryId;
	}

	public void setCountryId(int countryId) {
		this.countryId = countryId;
	}
	
	/**
	 * 比较地理位置是否相等，紧根据id来比较
	 * @param geoInfo
	 * @return
	 * @update 2015年9月6日 下午6:00:09
	 */
	public boolean equestGeoinfo(GeoInfo geoInfo) {
		if (geoInfo != null) {
			return countryId == geoInfo.getCountryId() && provinceId == geoInfo.getProvinceId() && cityId == geoInfo.getCityId();
		} else {
			return false;
		}
	}
	
	/**
	 * 获取地理地理信息
	 * @return
	 * @update 2015年9月6日 下午5:47:16
	 */
	public GeoInfo getGeoinfo() {
		GeoInfo geoInfo = new GeoInfo();
		geoInfo.setCountry(country);
		geoInfo.setCountryId(countryId);
		geoInfo.setProvince(province);
		geoInfo.setProvinceId(provinceId);
		geoInfo.setCity(city);
		geoInfo.setCityId(cityId);
		return geoInfo;
	}

	/**
	 * 获得头像的实际显示路径，默认显示缩略图，如果缩略图为空，则显示原始头像，若还为空，则返回null
	 * @return
	 * @update 2015年7月28日 下午9:37:52
	 */
	public String getIconShowPath() {
		if (!TextUtils.isEmpty(thumbPath)) {
			return thumbPath;
		} else if (!TextUtils.isEmpty(iconPath)) {
			return iconPath;
		} else {
			return null;
		}
	}

	/**
	 * 获得自己的名称，优先显示昵称，没有昵称啧显示真实名称，如果没有真实名称，则显示用户名
	 * @author tiger
	 * @update 2015年2月14日 下午9:58:29
	 * @return
	 */
	public String getName() {
		String name = username;
		if (!TextUtils.isEmpty(nickname)) {
			name = nickname;
		} else if (!TextUtils.isEmpty(realName)) {
			name = realName;
		}
		return name;
	}

	/**
	 * 获得用户的jid
	 * @return
	 */
	public String getJID() {
		return username + "@" + Constants.SERVER_NAME;
	}

	@Override
	public String toString() {
		return "Personal [id=" + id + ", username=" + username + ", password=" + password + ", nickname=" + nickname
				+ ", realName=" + realName + ", email=" + email + ", phone=" + phone + ", resource=" + resource
				+ ", status=" + status + ", mode=" + mode + ", street=" + street + ", city=" + city + ", cityId="
				+ cityId + ", province=" + province + ", provinceId=" + provinceId + ", country=" + country
				+ ", countryId=" + countryId + ", zipCode=" + zipCode + ", iconPath=" + iconPath + ", thumbPath="
				+ thumbPath + ", iconHash=" + iconHash + ", mimeType=" + mimeType + ", sex=" + sex + ", desc=" + desc
				+ "]";
	}

	public String getFullJID() {
		/*if (TextUtils.isEmpty(resource)) {
			return getJID();
		} else {
			return getJID() + "/" + resource;
		}*/
		return XmppStringUtils.completeJidFrom(username, Constants.SERVER_NAME, resource);
	}
	
	/**
	 * 判断该类是否为空
	 * @update 2015年1月21日 下午5:52:47
	 * @return
	 */
	public boolean isEmpty() {
		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
			return true;
		} else {
			return false;
		}
	}
}
