package net.ibaixin.chat.view;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.ibaixin.chat.R;
import net.ibaixin.chat.record.RecordStrategy;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;

/**
 * 录音按钮
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年9月14日 下午5:54:27
 */
public class RecordButton extends TextView {
	private static final int MIN_RECORD_TIME = 1; // 最短录音时间，单位秒
	private static final int RECORD_OFF = 0; // 不在录音
	private static final int RECORD_ON = 1; // 正在录音
	
	private Dialog mRecordDialog;
	private RecordStrategy mAudioRecorder;
	private Thread mRecordThread;
	private RecordListener mRecordListener;
	
	private int mRecordState = 0; // 录音状态
	private float mRecodeTime = 0.0f; // 录音时长，如果录音时间太短则录音失败
	private double mVoiceValue = 0.0; // 录音的音量值
	private boolean mIsCanceled = false; // 是否取消录音
	
	/**
	 * 录音对话框中的图标
	 */
	private ImageView mRecordView;
	
	/**
	 * 录音对话框中的提示
	 */
	private TextView mRecordText;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			//根据音量的大小来显示对应的图标
			showVolumeImg(mVoiceValue);
		}
	};
	
	public RecordButton(Context context) {
		super(context);
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public RecordButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public RecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public RecordButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setAudioRecorder(RecordStrategy audioRecorder) {
		this.mAudioRecorder = audioRecorder;
	}
	
	public RecordStrategy getAudioRecorder() {
		return mAudioRecorder;
	}
	
	public void setRecordListener(RecordListener listener) {
		this.mRecordListener = listener;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float downY = 0;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:	//按下按钮
			setPressed(true);
			if (mRecordState != RECORD_ON) {
				downY = event.getY();
				if (mAudioRecorder != null) {
					if (mAudioRecorder.prepare()) {
						mRecordState = RECORD_ON;
						showRecordDialog(0);
						if (mAudioRecorder.start()) {
							if (SystemUtil.isPermissionEnable(getContext(), Manifest.permission.RECORD_AUDIO)) {
								startRecord();
							} else {
								SystemUtil.makeShortToast(R.string.chat_record_disable_tip);
//								dismissRecordDialog();
							}
						} else {
							dismissRecordDialog();
						}
					} else {
						dismissRecordDialog();
					}
				} else {
					dismissRecordDialog();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:	//移动手指
			if (!isPressed()) {
				setPressed(true);
			}
			float moveY = event.getY();
			float distance = downY - moveY;
			if (distance > 50) {	//此时的状态是松手即可取消录音
				mIsCanceled = true;
				showRecordDialog(1);
			} else {	//此时的状态是松手即可完成录音
				mIsCanceled = false;
				showRecordDialog(0);
			}
			break;
		case MotionEvent.ACTION_UP:	//松开手指
			setPressed(false);
			if (mRecordState == RECORD_ON) {
				mRecordState = RECORD_OFF;
				stopRecord(mIsCanceled);
			}
			break;
		case MotionEvent.ACTION_CANCEL:	//取消
			setPressed(false);
			mRecordState = RECORD_OFF;
			mIsCanceled = true;
			stopRecord(mIsCanceled);
			break;
		default:
			break;
		}
		return true;
	}
	
	/**
	 * 开始录音，启用新的线程
	 * @update 2015年9月14日 下午7:26:43
	 */
	private void startRecord() {
		mRecordThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				mRecodeTime = 0.0f;
				while (mRecordState == RECORD_ON) {
					try {
						SystemClock.sleep(100);
						mRecodeTime += 0.1;
						//获取音量，并更新界面
						if (!mIsCanceled) {	//不想取消录音就更新音量信息
							mVoiceValue = mAudioRecorder.getAmplitude();
							mHandler.sendEmptyMessage(1);
						}
					} catch (Exception e) {
						Log.e(e.getMessage());
					}
				}
			}
		});
		mRecordThread.start();
	}
	
	/**
	 * 停止录音，主要做一些停止的操作
	 * @param isCanceled 是否是取消录音操作，true:是取消录音操作，此时，需要将录音文件删除；false:不是取消操作，此时，需要保存录音文件
	 * @update 2015年9月15日 上午10:29:54
	 */
	private void stopRecord(boolean isCanceled) {
		dismissRecordDialog();
		
		mAudioRecorder.stop();
		mVoiceValue = 0.0;
		if (isCanceled) {
			mAudioRecorder.deleteRecordFile();
		} else {
			if (mRecodeTime < MIN_RECORD_TIME) {	//录音时间太短
				showRecordTip(R.string.chat_record_short_tip);
				mAudioRecorder.deleteRecordFile();
			} else {	//录音完成
				if (mRecordListener != null) {
					mRecordListener.onRecordFinished(mAudioRecorder.getRecordFilePath(), (int) mRecodeTime);
				}
			}
		}
		mIsCanceled = false;
		resetRecordButton();
	}
	
	/**
	 * 重置录音按钮
	 * @update 2015年9月15日 上午10:58:12
	 */
	private void resetRecordButton() {
		if (!isEnabled()) {
			setEnabled(true);
		}
		setText(R.string.chat_btn_make_voice_start);
	}
	
	/**
	 * 根据音量的大小来显示对应的图片
	 * @param voiceValue
	 * @update 2015年9月15日 上午11:12:53
	 */
	private void showVolumeImg(double voiceValue) {
		if (voiceValue <= 600.0) {
			mRecordView.setImageResource(R.drawable.record_animate_01);
		} else if (voiceValue > 600.0 && voiceValue <= 1000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_02);
		} else if (voiceValue > 1000.0 && voiceValue <= 1200.0) {
			mRecordView.setImageResource(R.drawable.record_animate_03);
		} else if (voiceValue > 1200.0 && voiceValue <= 1400.0) {
			mRecordView.setImageResource(R.drawable.record_animate_04);
		} else if (voiceValue > 1400.0 && voiceValue <= 1600.0) {
			mRecordView.setImageResource(R.drawable.record_animate_05);
		} else if (voiceValue > 1600.0 && voiceValue <= 1800.0) {
			mRecordView.setImageResource(R.drawable.record_animate_06);
		} else if (voiceValue > 1800.0 && voiceValue <= 2000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_07);
		} else if (voiceValue > 2000.0 && voiceValue <= 3000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_08);
		} else if (voiceValue > 3000.0 && voiceValue <= 4000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_09);
		} else if (voiceValue > 4000.0 && voiceValue <= 6000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_10);
		} else if (voiceValue > 6000.0 && voiceValue <= 8000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_11);
		} else if (voiceValue > 8000.0 && voiceValue <= 10000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_12);
		} else if (voiceValue > 10000.0 && voiceValue <= 12000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_13);
		} else if (voiceValue > 12000.0) {
			mRecordView.setImageResource(R.drawable.record_animate_14);
		}
	}
	
	/**
	 * 显示录音提示视图
	 * @param tipRes
	 * @update 2015年9月15日 上午10:34:18
	 */
	private void showRecordTip(int tipRes) {
		Toast toast = new Toast(getContext());
		View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_record, null);
		ImageView ivIcon = (ImageView) view.findViewById(R.id.iv_icon);
		ivIcon.setImageResource(R.drawable.voice_to_short);
		TextView tvTip = (TextView) view.findViewById(R.id.tv_tip);
		tvTip.setText(tipRes);
		toast.setView(view);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.show();
	}
	
	/**
	 * 根据不同的状态来显示不同的对话框
	 * @param recordFlag 录音的操作状态，0：当前正在录音，且手指没有离开录音按钮，松开即可完成录音；1：当前正在录音，但手指离开了录音按钮，松开即可取消录音
	 * @update 2015年9月14日 下午6:20:50
	 */
	private void showRecordDialog(int recordFlag) {
		if (mRecordDialog == null) {
			mRecordDialog = new Dialog(getContext(), R.style.AppTheme_RecordStyle);
			mRecordDialog.setContentView(R.layout.layout_record);
			mRecordDialog.setCancelable(false);
			mRecordView = (ImageView) mRecordDialog.findViewById(R.id.iv_icon);
			mRecordText = (TextView) mRecordDialog.findViewById(R.id.tv_tip);
		}
		switch (recordFlag) {
		case 1:	//当前正在录音，但手指离开了录音按钮，松开即可取消录音
			mRecordView.setImageResource(R.drawable.record_cancel);
			mRecordText.setText(R.string.chat_record_dialog_cancel);
			setText(R.string.chat_record_btn_cancel);
			break;
		default:
			mRecordView.setImageResource(R.drawable.record_animate_01);
			mRecordText.setText(R.string.chat_record_dialog_done);
			setText(R.string.chat_record_btn_done);
			break;
		}
		if (!mRecordDialog.isShowing()) {
			mRecordDialog.show();
		}
	}
	
	/**
	 * 让录音对话框消失
	 * @update 2015年9月15日 上午10:11:17
	 */
	private void dismissRecordDialog() {
		if (mRecordDialog != null && mRecordDialog.isShowing()) {
			mRecordDialog.dismiss();
		}
		if (!isEnabled()) {
			setEnabled(true);
		}
	}
	
	/**
	 * 录音的监听器
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年9月14日 下午6:07:55
	 */
	public interface RecordListener {
		/**
		 * 录音完成
		 * @param filePath 录音文件的全路径，包含文件名
		 * @param recordTime 音频文件的时长,单位（秒）
		 * @update 2015年9月16日 上午10:25:28
		 */
		public void onRecordFinished(String filePath, int recordTime);
	}

}
