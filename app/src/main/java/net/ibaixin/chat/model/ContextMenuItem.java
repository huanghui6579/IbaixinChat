package net.ibaixin.chat.model;

import net.ibaixin.chat.ChatApplication;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;


/**
 * 自定义长按菜单，菜单的顺序按照添加的顺序来排列
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年2月16日 上午9:36:23
 */
public class ContextMenuItem {
	/**
	 * 菜单id
	 */
	private int itemId;
	
	/**
	 * 菜单标题的资源id
	 */
	private int titleRes;
	
	/**
	 * 菜单的标题
	 */
	private String title;
	
	/**
	 * 菜单的图标资源 
	 */
	private int iconRes;
	
	/**
	 *	菜单的图标
	 */
	private Drawable icon;
	
	/**
	 * 该菜单项是否可用
	 */
	private boolean enable = true;
	
	/**
	 * 该菜单项是否可选中
	 */
	private boolean checkable = true;
	
	/**
	 * 该菜单项是否可见
	 */
	private boolean visible = true;

	public ContextMenuItem(int itemId, String title) {
		super();
		this.itemId = itemId;
		this.title = title;
	}

	public ContextMenuItem(int itemId, int titleRes) {
		super();
		this.itemId = itemId;
		this.titleRes = titleRes;
	}

	public ContextMenuItem(int itemId, int titleRes, int iconRes) {
		super();
		this.itemId = itemId;
		this.titleRes = titleRes;
		this.iconRes = iconRes;
	}

	public ContextMenuItem(int itemId, String title, Drawable icon) {
		super();
		this.itemId = itemId;
		this.title = title;
		this.icon = icon;
	}

	@Override
	public String toString() {
		return title;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public int getTitleRes() {
		return titleRes;
	}

	public void setTitleRes(int titleRes) {
		this.titleRes = titleRes;
	}

	public CharSequence getTitle() {
		if (!TextUtils.isEmpty(title)) {
			return title;
		}
		if (titleRes <= 0) {
			return title;
		} else {
			return ChatApplication.getInstance().getString(titleRes);
		}
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getIconRes() {
		return iconRes;
	}

	public void setIconRes(int iconRes) {
		this.iconRes = iconRes;
	}

	public Drawable getIcon() {
		if (icon != null) {
			return icon;
		}
		if (iconRes <= 0) {
			return icon;
		} else {
			return ChatApplication.getInstance().getResources().getDrawable(iconRes);
		}
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isCheckable() {
		return checkable;
	}

	public void setCheckable(boolean checkable) {
		this.checkable = checkable;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
