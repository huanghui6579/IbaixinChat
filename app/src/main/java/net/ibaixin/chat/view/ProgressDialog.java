package net.ibaixin.chat.view;

import net.ibaixin.chat.R;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

/** 圆形对话框
 * @author tiger
 * @version 2015年2月14日 下午10:16:29
 */
public class ProgressDialog extends MaterialDialog {
	
	protected ProgressDialog(Builder builder) {
		super(builder);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 显示圆形对话框
	 * @author tiger
	 * @update 2015年2月14日 下午11:28:01
	 * @param context
	 * @param title 标题
	 * @param message 内容
	 * @return
	 */
	public static ProgressDialog show(Context context, CharSequence title, CharSequence message) {
		return show(context, title, message, true);
	}
	
	/**
	 * 显示圆形对话框
	 * @author tiger
	 * @update 2015年2月14日 下午11:29:58
	 * @param context
	 * @param title 标题
	 * @param message 内容
	 * @param cancelable 是否可返回消失
	 * @return
	 */
	public static ProgressDialog show(Context context, CharSequence title, CharSequence message, boolean cancelable) {
		return show(context, title, message, cancelable, null);
	}
	
	/**
	 * 显示圆形对话框
	 * @author tiger
	 * @update 2015年2月14日 下午11:34:23
	 * @param context
	 * @param title 标题
	 * @param message 内容
	 * @param cancelable 是否可返回消失
	 * @param cancelListene 对话框消失的监听器
	 * @return
	 */
	public static ProgressDialog show(Context context, CharSequence title, CharSequence message, boolean cancelable, OnCancelListener cancelListene) {
		MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
		builder.title(title)
			.customView(R.layout.progress_dialog_holo, false)
			.cancelable(cancelable)
			.cancelListener(cancelListene);
		ProgressDialog dialog = new ProgressDialog(builder);
		View view = dialog.getCustomView();
		TextView textView = (TextView) view.findViewById(R.id.message);
		textView.setText(message);
		dialog.show();
		return dialog;
	}
}
