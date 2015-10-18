package net.ibaixin.chat.activity;

import net.ibaixin.chat.R;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.util.SSHHelper;
import net.ibaixin.chat.view.ProgressDialog;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 后面界面
 * @author dudejin
 * 2015-03-09
 */
public class BackDoorActivity extends BaseActivity implements OnClickListener {
	
	private ProgressDialog pDialog;

	private TextView outputstr ;
	private EditText inputstr ;
	private Button backdoor_do_btn ;
	
	@Override
	protected int getContentView() {
		return R.layout.activity_backdoor;
	}

	@Override
	protected void initView() {
		outputstr = (TextView) findViewById(R.id.outputstr);
		inputstr = (EditText) findViewById(R.id.inputstr);
		backdoor_do_btn = (Button) findViewById(R.id.backdoor_do_btn);
	}

	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected void addListener() {
		backdoor_do_btn.setOnClickListener(this);
		inputstr.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				if(TextUtils.isEmpty(s)) {
					setBtnState(backdoor_do_btn,false);
				} else {
					setBtnState(backdoor_do_btn,true);
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
	}

	/**
	 * 设置按钮状态，true表示可用，false表示不可用
	 * @param isEnable 使用、否可用
	 */
	private void setBtnState(Button btn ,boolean enable) {
		btn.setEnabled(enable);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.backdoor_do_btn:
			new RegistTask().execute();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 注册的后台任务
	 * @author dudejin
	 *
	 */
	class RegistTask extends AsyncTask<Void, Void, String> {
		@Override
		protected void onPreExecute() {
			if (pDialog == null) {
				pDialog = ProgressDialog.show(mContext, null, getString(R.string.registing), true);
			} else {
				pDialog.show();
			}
		}

		@Override
		protected String doInBackground(Void... params) {
			String outdata = SSHHelper.exec("123.56.116.45", "root", "DDJddj123456", 22, backdoor_do_btn.getText().toString());
			return outdata;
		}
		
		@Override
		protected void onPostExecute(String result) {
			hideLoadingDialog(pDialog);
			outputstr.setText(result);
		}
	}

	@Override
	protected void initData() {
		
	}
	

}
