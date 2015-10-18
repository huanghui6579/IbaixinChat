package net.ibaixin.chat.record;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;

/**
 * 音频文件的录音
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年9月14日 下午6:02:22
 */
public class AudioRecorder implements RecordStrategy {
	private MediaRecorder mMediaRecorder;
	
	/**
	 * 录音文件的全路径，包含文件名
	 */
	private String filePath;
	
	private boolean mIsRecording;
	
	/**
	 * 会话id，根据该id和当前登录的用户来生存文件的存储目录
	 */
	private int mThreadId;

	public AudioRecorder(int threadId) {
		this.mThreadId = threadId;
	}

	@Override
	public boolean prepare() {
		try {
			filePath = generateRecordFilePath();
			if (filePath != null) {
				mMediaRecorder = new MediaRecorder();
				mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);	//设置MediaRecorder的音频源为麦克风
				mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);	//设置MediaRecorder录制的音频格式
				mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);	// 设置MediaRecorder录制音频的编码为amr
				mMediaRecorder.setOutputFile(filePath);
				return true;
			} else {
				return false;
			}
		} catch (IllegalStateException e) {
			Log.e(e.getMessage());
		}
		return true;
	}
	
	/**
	 * 生成录音文件的全路径，包含文件名
	 * @return
	 * @update 2015年9月15日 下午4:47:37
	 */
	private String generateRecordFilePath() {
		File file = SystemUtil.generateChatAttachFile(mThreadId, SystemUtil.generateChatAttachFilename(System.currentTimeMillis()));
		if (file != null) {
			return file.getAbsolutePath();
		} else {
			return null;
		}
	}

	@Override
	public boolean start() {
		if (!mIsRecording) {
			try {
				mMediaRecorder.prepare();
				mMediaRecorder.start();
				mIsRecording = true;
				return true;
			} catch (IllegalStateException e) {
				Log.e(e.getMessage());
			} catch (IOException e) {
				Log.e(e.getMessage());
			}
		}
		return false;
	}

	@Override
	public boolean stop() {
		if (mIsRecording) {
			try {
				mMediaRecorder.stop();
				mMediaRecorder.release();
				mIsRecording = false;
				return true;
			} catch (IllegalStateException e) {
				Log.e(e.getMessage());
			}
		}
		return false;
	}

	@Override
	public boolean deleteRecordFile() {
		SystemUtil.deleteFile(filePath);
		return true;
	}

	@Override
	public double getAmplitude() {
		if (!mIsRecording) {
			return 0;
		} else {
			return mMediaRecorder.getMaxAmplitude();
		}
	}

	@Override
	public String getRecordFilePath() {
		return filePath;
	}

	@Override
	public boolean hasData() {
		if (filePath != null) {
			File file = new File(filePath);
			if (file.exists() && file.length() > 0) {
				return true;
			}
		}
		return false;
	}

}
