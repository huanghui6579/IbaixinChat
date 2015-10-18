package net.ibaixin.chat.view;

import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.imageaware.ViewAware;

/**
 * 在textview中显示图片
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月19日 下午7:52:34
 */
public class TextViewAware extends ViewAware {
	
	protected ImageSize imageSize;

	public TextViewAware(View view) {
		super(view);
		imageSize = new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT);
	}

	public TextViewAware(View view, boolean checkActualViewSize) {
		super(view, checkActualViewSize);
		imageSize = new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT);
	}

	public TextViewAware(View view, ImageSize imageSize) {
		super(view);
		this.imageSize = imageSize;
	}

	public TextViewAware(View view, boolean checkActualViewSize, ImageSize imageSize) {
		super(view, checkActualViewSize);
		this.imageSize = imageSize;
	}

	@Override
	public int getWidth() {
		if (imageSize != null) {
			return imageSize.getWidth();
		} else {
			return super.getWidth();
		}
	}

	@Override
	public int getHeight() {
		if (imageSize != null) {
			return imageSize.getHeight();
		} else {
			return super.getHeight();
		}
	}

	@Override
	protected void setImageDrawableInto(Drawable drawable, View view) {
		((TextView) view).setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
		if (drawable instanceof AnimationDrawable) {
			((AnimationDrawable) drawable).start();
		}
	}

	@Override
	protected void setImageBitmapInto(Bitmap bitmap, View view) {
		Drawable drawable = ImageUtil.bitmapToDrawable(bitmap);
		setImageDrawableInto(drawable, view);
	}

}
