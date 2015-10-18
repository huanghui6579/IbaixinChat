package net.ibaixin.chat.model;

import org.jivesoftware.smack.chat.Chat;

import android.os.Handler;

/**
 * 消息发送实体
 * @author Administrator
 * @update 2014年11月16日 下午7:47:19
 * @version 1.0.0
 */
public class MsgSenderInfo {
	public Chat chat;
	public MsgInfo msgInfo;
	public MsgThread msgThread;
	public boolean isReSend;
	public Handler handler;
	/**
	 * 在发送图片时，该值是判断是否发送原图
	 */
	public boolean originalImage;
	
	public MsgSenderInfo(Chat chat, MsgInfo msgInfo, MsgThread msgThread,
			Handler handler) {
		super();
		this.chat = chat;
		this.msgInfo = msgInfo;
		this.msgThread = msgThread;
		this.handler = handler;
	}
	
	public MsgSenderInfo(Chat chat, MsgInfo msgInfo, MsgThread msgThread,
			Handler handler, boolean isReSend) {
		this(chat, msgInfo, msgThread, handler);
		this.isReSend = isReSend;
	}
}
