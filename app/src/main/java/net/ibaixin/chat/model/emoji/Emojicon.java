package net.ibaixin.chat.model.emoji;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * emoji表情的实体类
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年1月23日 上午9:16:30
 */
public class Emojicon implements Parcelable {
	/**
	 * 表情的资源id
	 */
	private int icon;
	/**
	 * 表情的ASCII码
	 */
	private char value;
	/**
	 * 表情的对应的字符串值
	 */
	private String emoji;

	public int getIcon() {
		return icon;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public char getValue() {
		return value;
	}

	public void setValue(char value) {
		this.value = value;
	}

	public String getEmoji() {
		return emoji;
	}

	public void setEmoji(String emoji) {
		this.emoji = emoji;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(icon);
		dest.writeInt(value);
		dest.writeString(emoji);
	}
	
	public Emojicon() {}
	
	public Emojicon(Parcel in) {
		icon = in.readInt();
		value = (char) in.readInt();
		emoji = in.readString();
	}
	
	@Override
    public boolean equals(Object o) {
        return o instanceof Emojicon && emoji.equals(((Emojicon) o).emoji);
    }

    @Override
    public int hashCode() {
        return emoji.hashCode();
    }

	public static final Creator<Emojicon> CREATOR = new Creator<Emojicon>() {
		
		@Override
		public Emojicon[] newArray(int size) {
			return new Emojicon[size];
		}
		
		@Override
		public Emojicon createFromParcel(Parcel source) {
			return new Emojicon(source);
		}
	};
	
	public static Emojicon fromCodePoint(int codePoint) {
        Emojicon emoji = new Emojicon();
        emoji.emoji = newString(codePoint);
        return emoji;
    }

    public static Emojicon fromChar(char ch) {
        Emojicon emoji = new Emojicon();
        emoji.emoji = Character.toString(ch);
        return emoji;
    }

    public static Emojicon fromChars(String chars) {
        Emojicon emoji = new Emojicon();
        emoji.emoji = chars;
        return emoji;
    }
    
    public static final String newString(int codePoint) {
        if (Character.charCount(codePoint) == 1) {
            return String.valueOf(codePoint);
        } else {
            return new String(Character.toChars(codePoint));
        }
    }
}
