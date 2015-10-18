package net.ibaixin.chat.activity;

import net.ibaixin.chat.R;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.task.RegistTask;
import net.ibaixin.chat.util.SystemUtil;
import android.content.Intent;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 注册界面
 * @author Administrator
 * @version 2014年10月7日 下午3:13:07
 */
public class RegistActivity extends BaseActivity implements OnClickListener {
	
	private EditText etAccount;
	private EditText etNickname;
	private EditText etEmail;
	private EditText etPassword;
	private EditText etConfirmPassword;
	
	private boolean isCanRegister = false;//输入框的内容是否允许注册
	
	private Button btnRegist;
	
	private TextView tvLogin;
	
	private SystemConfig systemConfig;

	@Override
	protected int getContentView() {
		return R.layout.activity_regist;
	}

	@Override
	protected void initView() {
		etAccount = (EditText) findViewById(R.id.et_account);
		etNickname = (EditText) findViewById(R.id.et_nickname);
		etEmail = (EditText) findViewById(R.id.et_email);
		etPassword = (EditText) findViewById(R.id.et_password);
		etConfirmPassword = (EditText) findViewById(R.id.et_confirm_password);
		
		btnRegist = (Button) findViewById(R.id.btn_regist);
		tvLogin = (TextView) findViewById(R.id.tv_login);
	}

	@Override
	protected void initData() {
		systemConfig = application.getSystemConfig();
	}

	@Override
	protected void addListener() {
		// TODO Auto-generated method stub
		etAccount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String password = etPassword.getText().toString();
				String confirmPassword = etConfirmPassword.getText().toString();
				if (TextUtils.isEmpty(s) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
					setRegistBtnState(false);
				} else if (!password.equals(confirmPassword)) {
					setRegistBtnState(false);
				} else {
					setRegistBtnState(true);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		etPassword.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String confirmPassword = etConfirmPassword.getText().toString();
				if (TextUtils.isEmpty(s) || TextUtils.isEmpty(etAccount.getText().toString()) || TextUtils.isEmpty(confirmPassword)) {
					setRegistBtnState(false);
				} else if (!s.toString().equals(confirmPassword)) {
					setRegistBtnState(false);
				} else {
					setRegistBtnState(true);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		etConfirmPassword.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String password = etPassword.getText().toString();
				if (TextUtils.isEmpty(s) || TextUtils.isEmpty(etAccount.getText().toString()) || TextUtils.isEmpty(password)) {
					setRegistBtnState(false);
				} else if (!s.toString().equals(password)) {
					setRegistBtnState(false);
				} else {
					setRegistBtnState(true);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		etConfirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.ACTION_DOWN) {
					SystemUtil.hideSoftInput(v);
					v.clearFocus();
					if (!application.getSystemConfig().isOnline() && isCanRegister) {
						systemConfig.setAccount(etAccount.getText().toString());
						systemConfig.setPassword(etPassword.getText().toString());
						systemConfig.setNickname(etNickname.getText().toString());
						systemConfig.setEmail(etEmail.getText().toString());
						new RegistTask(mContext).execute(systemConfig);
					}
					return true;
				}
				return false;
			}
		});
		
		btnRegist.setOnClickListener(this);
		tvLogin.setOnClickListener(this);
	}

	/*@Override
	protected void initAlimamaSDK() {
		super.initAlimamaSDK();
		ViewGroup nat = (ViewGroup) findViewById(R.id.nat);
	    String slotId = "65571";
	    setupAlimama(nat, slotId);
	}*/
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_regist:	//注册
			systemConfig.setAccount(etAccount.getText().toString());
			systemConfig.setPassword(etPassword.getText().toString());
			systemConfig.setNickname(etNickname.getText().toString());
			systemConfig.setEmail(etEmail.getText().toString());
			if (SystemUtil.isSoftInputActive()) {	//输入法已展开，则关闭输入法
				SystemUtil.hideSoftInput(this);
			}
			new RegistTask(mContext).execute(systemConfig);
			break;
		case R.id.tv_login:	//返回登录界面
			Intent intent = new Intent(mContext, LoginActivity.class);
			startActivity(intent);
			finish();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 设置注册按钮的状态
	 * @author Administrator
	 * @update 2014年10月7日 下午3:47:36
	 * @param isEnable
	 */
	private void setRegistBtnState(boolean enable) {
		isCanRegister = enable ;
		btnRegist.setEnabled(enable);
	}
}
