package net.ibaixin.chat.view;

/**
 * 图片tab的adapterProvider
 * @author huanghui1
 * @update 2015年1月28日 下午9:38:03
 */
public interface IconPagerAdapterProvider extends PagerAdapterProvider {
    /**
     * Get icon representing the page at {@code index} in the adapter.
     */
    int getIconResId(int index);

}
