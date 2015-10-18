package net.ibaixin.chat.record;

/**
 * 录音的接口
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年9月14日 下午5:56:48
 */
public interface RecordStrategy {
	/**
	 * 在这里进行录音准备工作，重置录音文件名等
	 */
	public boolean prepare();
	
	/**
	 * 开始录音
	 * @return
	 * @update 2015年9月14日 下午5:58:48
	 */
	public boolean start();
	
	/**
	 * 停止录音
	 * @return
	 * @update 2015年9月14日 下午5:59:08
	 */
	public boolean stop();
	
	/**
	 * 删除录音文件
	 * @return
	 * @update 2015年9月14日 下午5:59:49
	 */
	public boolean deleteRecordFile();
	
	/**
	 * 获取音量的大小
	 * @return
	 * @update 2015年9月14日 下午6:00:11
	 */
	public double getAmplitude();
	
	/**
	 * 获取录音文件的全路径，包含文件名
	 * @return
	 * @update 2015年9月14日 下午6:00:41
	 */
	public String getRecordFilePath();
	
	/**
	 * 是否有录音数据
	 * @return
	 * @update 2015年9月14日 下午6:01:30
	 */
	public boolean hasData();
}
