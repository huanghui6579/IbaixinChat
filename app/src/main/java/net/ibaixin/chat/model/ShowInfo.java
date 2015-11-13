package net.ibaixin.chat.model;

import android.graphics.drawable.Drawable;

/**
 * 主要展示一些基本的信息，如头像，名称
 * 创建人：huanghui1
 * 创建时间： 2015/11/13 17:12
 * 修改人：huanghui1
 * 修改时间：2015/11/13 17:12
 * 修改备注：
 *
 * @version: 0.0.1
 */
public class ShowInfo {
    private String name;
    
    private Drawable icon;
    
    private String iconPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
}
