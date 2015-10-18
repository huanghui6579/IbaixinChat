package net.ibaixin.chat.smack.provider;

import java.io.IOException;

import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.smack.extension.MessageTypeExtension;
import net.ibaixin.chat.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * 消息类型的提供者
 * @author tiger
 * @version 2015年4月26日 下午7:14:46
 */
public class MessageTypeProvider extends ExtensionElementProvider<MessageTypeExtension> {

	@Override
	public MessageTypeExtension parse(XmlPullParser parser, int initialDepth)
			throws XmlPullParserException, IOException, SmackException {
		MessageTypeExtension typeExtension = new MessageTypeExtension();
		String type = parser.getAttributeValue(0);
		String fileId = parser.getAttributeValue(1);
		typeExtension.setFileId(fileId);
		try {
			typeExtension.setMsgType(MsgInfo.Type.valueOf(Integer.parseInt(type)));
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		int eventType;
		do {
			eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				String name = parser.getName();
				String value = parser.nextText();
				switch (name) {
				case "fileName":	//文件名
					typeExtension.setFileName(value);
					break;
				case "thumbName":	//缩略名
					typeExtension.setThumbName(value);
					break;
				case "mimeType":	//mimetype
					typeExtension.setMimeType(value);
					break;
				case "hash":	//hash
					typeExtension.setHash(value);
					break;
				case "size":	//文件的尺寸
					try {
						long size = Long.parseLong(value);
						typeExtension.setSize(size);
					} catch (NumberFormatException e) {
						Log.e(e.getMessage());
					}
					break;
				case "desc":	//文件的描述，也可作扩展字段
					typeExtension.setDesc(value);
					break;
				default:
					break;
				}
			}
		} while (eventType != XmlPullParser.END_TAG && parser.getDepth() != initialDepth);
		return typeExtension;
	}

}
