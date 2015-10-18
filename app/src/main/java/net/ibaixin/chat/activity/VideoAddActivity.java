package net.ibaixin.chat.activity;

import java.util.HashMap;
import java.util.Map;

import net.ibaixin.chat.R;
import net.ibaixin.chat.model.FormFile;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.HttpRequest;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.EmojiconEditText;
import net.ibaixin.chat.view.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
/**
 * 趣味视频分享界面
 * @author dudejin
 */
public class VideoAddActivity extends BaseActivity {
//	private Button btnSend;
	private EmojiconEditText addjoke_content;
	private EmojiconEditText addjoke_url;
	private RadioGroup addjoke_type ;
	private RadioGroup addjoke_from ;
	private int joke_type = 0;//段子类型，1——笑话，2——新闻，3——感悟，100——其他
	private int joke_from = 0;//段子类型，1——腾讯视频，2——优酷视频
	private ProgressDialog proDialog;
	
	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_videoadd;
	}

	@Override
	protected void initView() {
		addjoke_content = (EmojiconEditText) findViewById(R.id.addjoke_content);
		addjoke_url = (EmojiconEditText) findViewById(R.id.addjoke_url);
		addjoke_type = (RadioGroup) findViewById(R.id.addjoke_type) ;
		addjoke_from = (RadioGroup) findViewById(R.id.addjoke_from) ;
	}
	
	@Override
	protected void initData() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void addListener() {
		addjoke_type.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.addjoke_type1){
					joke_type = 1 ;
				}
				if(checkedId == R.id.addjoke_type2){
					joke_type = 2 ;
				}
				if(checkedId == R.id.addjoke_type3){
					joke_type = 3 ;
				}
				if(checkedId == R.id.addjoke_type4){
					joke_type = 100 ;
				}
			}
		});
		addjoke_from.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.addjoke_from1){
					joke_from = 1 ;
				}
				if(checkedId == R.id.addjoke_from2){
					joke_from = 2 ;
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_jokeadd, menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case R.id.action_jokesend://发送
			if(joke_type == 0){
				SystemUtil.makeShortToast(R.string.error_type_null);
				return false ;
			}
			if(joke_from == 0){
				SystemUtil.makeShortToast(R.string.error_from_null);
				return false ;
			}
			String content = addjoke_content.getText().toString().trim() ;
			String url = addjoke_url.getText().toString().trim() ;
			if("".equals(content)){
				SystemUtil.makeShortToast(R.string.error_content_null);
				return false ;
			}
			if("".equals(url)){
				SystemUtil.makeShortToast(R.string.error_url_null);
				return false ;
			}
			Map<String, String> map = new HashMap<String, String>();
			map.put("content", content);
			map.put("remoteVideo", url);
			map.put("type", String.valueOf(joke_type));
			map.put("from", String.valueOf(joke_from));
			map.put("createUser", application.getCurrentAccount());
			new AddVideoTask().executeOnExecutor(SystemUtil.getCachedThreadPool(), map ,"UTF-8") ;
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	class AddVideoTask extends AsyncTask<Object, Boolean, Boolean>{
		@Override
		protected void onPreExecute() {
			if (proDialog == null) {
				proDialog = ProgressDialog.show(mContext, null, getString(R.string.submittingdata), true);
			} else {
				proDialog.show();
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Boolean doInBackground(Object... params) {
			Map<String,String> map = (Map<String,String>) params[0] ;
			Object obj = params[1] ;
			boolean flag = false ;
			if(obj instanceof FormFile){
				FormFile f = (FormFile) obj ;
				try {
					flag = HttpRequest.post(Constants.addvideoUrl, map, f,null) ;
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}else if(obj instanceof String){
				String enc = (String) obj ;
				try {
					flag = HttpRequest.post(Constants.addvideoUrl, map, enc,null);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
			return flag ;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(proDialog != null && proDialog.isShowing()) {
				proDialog.dismiss();
			}
			if(result){
				VideoReadMainActivity.isNeedReload = true ;
				SystemUtil.makeShortToast(R.string.ok_addvideo);
				finish();
			}else{
				SystemUtil.makeShortToast(R.string.error_uploadvideo);
			}
			super.onPostExecute(result);
		}
	}
}