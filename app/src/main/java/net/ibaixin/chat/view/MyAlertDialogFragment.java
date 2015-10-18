package net.ibaixin.chat.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * 自定义对话框，已废弃，使用{@link com.afollestad.materialdialogs.MaterialDialog}代替
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月12日 下午5:21:42
 */
@Deprecated
public class MyAlertDialogFragment extends DialogFragment {
	public static final String ARG_ICON_ID = "arg_icon_id";
	public static final String ARG_TITLE = "arg_title";
	public static final String ARG_MESSAGE = "arg_message";
	public static final String ARG_POSITIVE_BUTTON_TEXT = "arg_positive_button_text";
	public static final String ARG_NEGATIVEBUTTONTEXT = "arg_negative_button_text";
	public static final String ARG_NEUTRAL_BUTTON_TEXT = "arg_neutral_button_text";
	public static final String ARG_CANCELABLE = "arg_cancelable";
	
	public DialogInterface.OnClickListener mPositiveButtonListener;
	public DialogInterface.OnClickListener mNegativeButtonListener;
	public DialogInterface.OnClickListener mNeutralButtonListener;
	
	public static class Builder {
		private int mIconId;
		private CharSequence mTitle;
		private CharSequence mMessage;
		private CharSequence mPositiveButtonText;
		private DialogInterface.OnClickListener positiveButtonListener;
        private CharSequence mNegativeButtonText;
        private DialogInterface.OnClickListener negativeButtonListener;
        private CharSequence mNeutralButtonText;
        private DialogInterface.OnClickListener neutralButtonListener;
        private boolean mCancelable = true;
        
		public Builder setIconId(int mIconId) {
			this.mIconId = mIconId;
			return this;
		}

		public Builder setTitle(CharSequence mTitle) {
			this.mTitle = mTitle;
			return this;
		}

		public Builder setMessage(CharSequence mMessage) {
			this.mMessage = mMessage;
			return this;
		}

		public Builder setPositiveButtonText(CharSequence mPositiveButtonText) {
			this.mPositiveButtonText = mPositiveButtonText;
			return this;
		}

		public Builder setPositiveButtonListener(
				DialogInterface.OnClickListener positiveButtonListener) {
			this.positiveButtonListener = positiveButtonListener;
			return this;
		}

		public Builder setNegativeButtonText(CharSequence mNegativeButtonText) {
			this.mNegativeButtonText = mNegativeButtonText;
			return this;
		}

		public Builder setNegativeButtonListener(
				DialogInterface.OnClickListener negativeButtonListener) {
			this.negativeButtonListener = negativeButtonListener;
			return this;
		}

		public Builder setNeutralButtonText(CharSequence mNeutralButtonText) {
			this.mNeutralButtonText = mNeutralButtonText;
			return this;
		}

		public Builder setNeutralButtonListener(
				DialogInterface.OnClickListener neutralButtonListener) {
			this.neutralButtonListener = neutralButtonListener;
			return this;
		}

		public Builder setCancelable(boolean mCancelable) {
			this.mCancelable = mCancelable;
			return this;
		}
		
		public DialogFragment create() {
			MyAlertDialogFragment alertDialogFragment = new MyAlertDialogFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_ICON_ID, mIconId);
			args.putCharSequence(ARG_TITLE, mTitle);
			args.putCharSequence(ARG_MESSAGE, mMessage);
			args.putCharSequence(ARG_POSITIVE_BUTTON_TEXT, mPositiveButtonText);
			args.putCharSequence(ARG_NEGATIVEBUTTONTEXT, mNegativeButtonText);
			args.putCharSequence(ARG_NEUTRAL_BUTTON_TEXT, mNeutralButtonText);
			args.putBoolean(ARG_CANCELABLE, mCancelable);
			alertDialogFragment.setArguments(args);
			alertDialogFragment.mPositiveButtonListener = positiveButtonListener;
			alertDialogFragment.mNegativeButtonListener = negativeButtonListener;
			alertDialogFragment.mNeutralButtonListener = neutralButtonListener;
			return alertDialogFragment;
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int iconId = args.getInt(ARG_ICON_ID, -1);
		CharSequence title = args.getCharSequence(ARG_TITLE, null);
		CharSequence message = args.getCharSequence(ARG_MESSAGE, null);
		CharSequence positiveButtonText = args.getCharSequence(ARG_POSITIVE_BUTTON_TEXT, null);
		CharSequence negativeButtonText = args.getCharSequence(ARG_NEGATIVEBUTTONTEXT, null);
		CharSequence neutralButtonText = args.getCharSequence(ARG_NEUTRAL_BUTTON_TEXT, null);
		boolean cancelable = args.getBoolean(ARG_CANCELABLE, true);
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(title)
			.setIcon(iconId)
			.setMessage(message)
			.setPositiveButton(positiveButtonText, mPositiveButtonListener)
			.setNegativeButton(negativeButtonText, mNegativeButtonListener)
			.setNeutralButton(neutralButtonText, mNeutralButtonListener)
			.setCancelable(cancelable)
			.create();
	}
	
}
