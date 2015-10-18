package net.ibaixin.chat.view;

import net.ibaixin.chat.model.EmojiType;

/**
 * 表情的tab扩展
 * @author huanghui1
 * @update 2015年1月28日 下午9:39:26
 */
public interface EmojiIconPagerAdapterProvider extends IconPagerAdapterProvider {
	/**
	 * 根据位置索引获得表情的类型
	 * @update 2015年1月28日 下午9:39:38
	 * @param index tab页的位置索引，从0开始
	 * @return
	 */
    EmojiType getEmojiType(int index);
}
