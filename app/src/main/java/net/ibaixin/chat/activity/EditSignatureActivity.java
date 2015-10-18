package net.ibaixin.chat.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.model.Personal;

/**
 * 编辑个性签名的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年9月7日 下午3:48:05
 */
public class EditSignatureActivity extends BaseActivity {

	public static final String ARG_SIGNATURE = "arg_signature";
	
	/**
	 * 个性签名最大的长度
	 */
	private int SIGNATURE_MAX_LENGTH;
	  
	private EditText etSignature;
	private TextView tvSignaturePrompt;
	
	private MenuItem mMenuDone;
	
	@Override
	protected int getContentView() {
		return R.layout.activity_edit_signature;
	}

	@Override
	protected void initView() {
		etSignature = (EditText) findViewById(R.id.et_signature);
		tvSignaturePrompt = (TextView) findViewById(R.id.tv_signature_prompt);
	}

	@Override
	protected void initData() {
		SIGNATURE_MAX_LENGTH = getResources().getInteger(R.integer.signature_max_length);
		Personal personal = ((ChatApplication) getApplication()).getCurrentUser();
		int textCount = 0;
		if (personal != null) {
			String signature = personal.getDesc();
			if (!TextUtils.isEmpty(signature)) {
				textCount = signature.length();
			}
			etSignature.setText(signature);
		}
		tvSignaturePrompt.setText(getString(R.string.personal_info_prompt_signature, textCount, SIGNATURE_MAX_LENGTH));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_save, menu);
		mMenuDone = menu.findItem(R.id.action_select_complete);
		mMenuDone.setTitle(R.string.save);
		mMenuDone.setEnabled(false);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_select_complete:	//保存
			CharSequence signature = etSignature.getText();
			if (signature == null) {
				signature = "";
			}
			Intent intent = new Intent();
			intent.putExtra(ARG_SIGNATURE, signature);
			setResult(RESULT_OK, intent);
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void addListener() {
		etSignature.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				int textCount = 0;
				if (s != null) {
					textCount = s.length();
				}
				if (textCount > 0) {
					if (mMenuDone != null && !mMenuDone.isEnabled()) {
						mMenuDone.setEnabled(true);
					}
				} else {
					if (mMenuDone != null && mMenuDone.isEnabled()) {
						mMenuDone.setEnabled(false);
					}
				}
				tvSignaturePrompt.setText(getString(R.string.personal_info_prompt_signature, textCount, SIGNATURE_MAX_LENGTH));
			}
		});
	}

}
