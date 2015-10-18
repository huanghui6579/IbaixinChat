package net.ibaixin.chat.smack.provider;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.ibaixin.chat.smack.packet.VcardX;

/**
 * vcardX的provider
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月8日 下午5:58:17
 */
public class VcardXProvider extends IQProvider<VcardX> {

	@Override
	public VcardX parse(XmlPullParser parser, int initialDepth)
			throws XmlPullParserException, IOException, SmackException {
		VcardX vcardX = new VcardX();
		String name = null;
		String value = null;
		while (true) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				name = parser.getName();
				value = parser.nextText();
				switch (name) {
				case "MIMETYPE":
					vcardX.setMimeType(value);
					break;
				case "ICONHASH":
					vcardX.setIconHash(value);
					break;
				default:
					break;
				}
			} else if (eventType == XmlPullParser.END_TAG && initialDepth == parser.getDepth()) {
				break;
			}
		}
		return vcardX;
	}

}
