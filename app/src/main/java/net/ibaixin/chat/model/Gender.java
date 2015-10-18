package net.ibaixin.chat.model;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;

/**
 * 性别
 * @author tiger
 * @version 2015年3月15日 上午10:40:04
 */
public enum Gender {
	/**
	 * 未知
	 */
	UNKNOWN(""),
	/**
	 * 男
	 */
	MAN(ChatApplication.getInstance().getString(R.string.sex_man)),
	/**
	 * 女
	 */
	WOMAN(ChatApplication.getInstance().getString(R.string.sex_woman));
	
	private Gender(String name) {
		this.name = name;
	}
	
	private String name;
	
	public static final String FIELD_SEX = "sex";
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static Gender valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values().length) {
            throw new IndexOutOfBoundsException("Invalid ordinal");
        }
        return values()[ordinal];
	}
}
