package net.ibaixin.chat.model.emoji;

import android.content.Context;

/**
 * 最近使用过的表情接口
 * @author huanghui1
 * @update 2015年1月26日 下午8:55:11
 */
public interface EmojiconRecents {
    public void addRecentEmoji(Context context, Emojicon emojicon);
}