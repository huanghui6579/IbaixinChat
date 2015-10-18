package net.ibaixin.chat.activity;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.WebviewSettings;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
/**
 * 趣味视频主界面
 * @author dudejin
 */
public class VideoReadMainActivity extends BaseActivity {
	public static boolean isNeedReload = false ;
	
//	private Boolean islandport = true;//true表示此时是竖屏，false表示此时横屏。
	private Toolbar toolbar ;
	private View xCustomView;
	private xWebChromeClient xwebchromeclient;
	private WebChromeClient.CustomViewCallback 	xCustomViewCallback;
	private FrameLayout videoview;// 全屏时视频加载view
	/**
	 * 定义浏览器内核对象
	 */
	private WebView webView;
	/**
	 * 下拉刷新view
	 */
	private SwipeRefreshLayout swipeLayout;

	private String url = Constants.websitePrefix+"/ibaixin/videomobile/listVideos" ;
//	private String url = "http://192.168.42.28/ibaixin/videomobile/listVideos" ;
			
	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_video;
	}

	@Override
	protected void initView() {
		toolbar = (Toolbar) findViewById(R.id.toolbar) ;
		videoview = (FrameLayout) findViewById(R.id.video_view) ;
		webView = (WebView) findViewById(R.id.jokeswebView) ;
		swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
	}
	
	@Override
	protected void initData() {
		WebviewSettings.initWebView(mContext,webView, swipeLayout);
		webView.addJavascriptInterface(new MobileJS(), "mobileJS");
		xwebchromeclient = new xWebChromeClient();
		webView.setWebChromeClient(xwebchromeclient);
		webView.loadUrl(url);
	}
	
	@Override
	protected void addListener() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inCustomView()) {
            	hideCustomView();
            	return true;
            }
            super.onKeyDown(keyCode, event);
    	}
    	return true;
    }
	
   /**
    * 判断是否是全屏
    * @return
    */
    public boolean inCustomView() {
 		return (xCustomView != null);
 	}
     /**
      * 全屏时按返加键执行退出全屏方法
      */
     public void hideCustomView() {
    	 xwebchromeclient.onHideCustomView();
 	}
	   
	@Override  
    public void onPause() {// 继承自Activity  
		webView.onPause();  
        super.onPause();  
    }  
  
	@Override
	public void onResume() {
		webView.onResume();
		if(isNeedReload)
			webView.loadUrl(webView.getUrl());
		isNeedReload = false ;
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		webView.loadDataWithBaseURL(null, "","text/html", "utf-8",null);//清理缓存
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_jokelist, menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.action_take_pic:
			intent = new Intent(mContext, VideoAddActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * 定制js组件
	 */
	private final class MobileJS {
		
		@JavascriptInterface
		public void shareLink(String linkurl,String content){
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
//             intent.setType("image/*");
//			intent.putExtra(Intent.EXTRA_SUBJECT, "百信趣味阅读");
			intent.putExtra(Intent.EXTRA_TEXT, content+"【来自百信趣味视频】"+linkurl);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
		}
		
		@JavascriptInterface
		public String getCurrentAccount() {
			return ChatApplication.getInstance().getCurrentAccount() ;
		}
		
		@JavascriptInterface
		public void toastMsg(String msg) {
			SystemUtil.makeShortToast(msg);
		}
	}
	
	/**
	 * 处理Javascript的对话框、网站图标、网站标题以及网页加载进度等
	 * @author
	 */
	public class xWebChromeClient extends WebChromeClient {
		private Bitmap xdefaltvideo;
		private View xprogressvideo;
		@Override  
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                //隐藏进度条  
                swipeLayout.setRefreshing(false);
                view.getSettings().setBlockNetworkImage(false);//网页加载好了  开始加载图片
            } else {
                if (!swipeLayout.isRefreshing())  
                    swipeLayout.setRefreshing(true);  
            }
            super.onProgressChanged(view, newProgress);  
        }
		
		@Override
    	//播放网络视频时全屏会被调用的方法
		public void onShowCustomView(View view, CustomViewCallback callback)
		{
			toolbar.setVisibility(View.GONE);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
	        webView.setVisibility(View.GONE);
	        //如果一个视图已经存在，那么立刻终止并新建一个
	        if (xCustomView != null) {
	            callback.onCustomViewHidden();
	            return;
	        }
	        videoview.addView(view);
	        xCustomView = view;
	        xCustomViewCallback = callback;
	        videoview.setVisibility(View.VISIBLE);
		}
		
		@Override
		//视频播放退出全屏会被调用的
		public void onHideCustomView() {
			if (xCustomView == null)//不是全屏播放状态
				return;
			// Hide the custom view.
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
			xCustomView.setVisibility(View.GONE);
			// Remove the custom view from its container.
			videoview.removeView(xCustomView);
			xCustomView = null;
			videoview.setVisibility(View.GONE);
			xCustomViewCallback.onCustomViewHidden();
			webView.setVisibility(View.VISIBLE);
			toolbar.setVisibility(View.VISIBLE);
	        //Log.i(LOGTAG, "set it to webVew");
		}
		//视频加载添加默认图标
		@Override
		public Bitmap getDefaultVideoPoster() {
			//Log.i(LOGTAG, "here in on getDefaultVideoPoster");	
			if (xdefaltvideo == null) {
				xdefaltvideo = BitmapFactory.decodeResource(
						getResources(), R.drawable.att_item_video);
		    }
			return xdefaltvideo;
		}
		//视频加载时进程loading
		@Override
		public View getVideoLoadingProgressView() {
			//Log.i(LOGTAG, "here in on getVideoLoadingPregressView");
			
	        if (xprogressvideo == null) {
	            LayoutInflater inflater = LayoutInflater.from(mContext);
	            xprogressvideo = inflater.inflate(R.layout.video_loading_progress, null);
	        }
	        return xprogressvideo; 
		}
    	//网页标题
    	 @Override
         public void onReceivedTitle(WebView view, String title) {
//            ((Activity) mContext).setTitle(title);
         }

//         @Override
//       //当WebView进度改变时更新窗口进度
//         public void onProgressChanged(WebView view, int newProgress) {
//        	 (MainActivity.this).getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress*100);
//         }
         

	}
}