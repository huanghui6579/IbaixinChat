package net.ibaixin.chat.view;

/**
 * pageAdapter的扩展
 * @author huanghui1
 * @update 2015年1月28日 下午9:36:54
 */
public interface PagerAdapterProvider {

    // From PagerAdapter
    int getCount();
    
    /**
     * 获得附加的操作性的指示器的数量
     * @update 2015年1月26日 下午3:31:03
     * @return
     */
    int getExtraCount();
}
