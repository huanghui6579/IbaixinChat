package net.ibaixin.chat.util;

import net.ibaixin.chat.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
/**
 * WebView常用设置
 * @author dudejin
 *
 */
public class WebviewSettings {

	/**
	 * 初始化WebView和刷新的控件SwipeRefreshLayout
	 * @param webView
	 * @param swipeLayout
	 * @param url
	 */
	@SuppressLint("SetJavaScriptEnabled")
	public static void initWebView(final Context context,final WebView webView,final SwipeRefreshLayout swipeLayout){
		// 自动的适应屏幕
		webView.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {//表示按返回键 时的操作
						webView.goBack(); //后退 
						return true;
					}
				}
				return false;
			}
		});
		swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            
            @Override  
            public void onRefresh() {
                //重新刷新页面  
            	webView.loadUrl(webView.getUrl());
            }
        });
        swipeLayout.setColorScheme(R.color.holo_blue_bright,  
                R.color.holo_green_light, R.color.holo_orange_light,  
                R.color.holo_red_light);
        //取消滚动条  
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);  
        //触摸焦点起作用 
        webView.requestFocus();
        // 水平不显示
//        webView.setHorizontalScrollBarEnabled(false);
//        webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
        //添加javaScript支持  
        WebSettings webseting = webView.getSettings();  
        webseting.setJavaScriptEnabled(true);   
        //加载url前，设置图片阻塞
        webseting.setBlockNetworkImage(true);
        //提高渲染的优先级
//        webView.getSettings().setRenderPriority(RenderPriority.HIGH);
        webseting.setJavaScriptCanOpenWindowsAutomatically(true);
        webseting.setPluginState(PluginState.ON);//可以使用插件
        webseting.setAllowFileAccess(true);  
        webseting.setLoadWithOverviewMode(true);
        webseting.setDomStorageEnabled(true);
//        webseting.setAppCacheMaxSize(1024*1024*8);//设置缓冲大小，我设的是8M  
//        String appCacheDir = ChatApplication.getInstance().getCacheDir().getAbsolutePath();
//        webseting.setAppCachePath(appCacheDir);
//        webseting.setAllowFileAccess(true);
//        webseting.setAppCacheEnabled(true);
//        webseting.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        //设置进度条  
        webView.setWebChromeClient(new WebChromeClient(){
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
//            @Override  
//            public void onReachedMaxAppCacheSize(long spaceNeeded,long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
//                    quotaUpdater.updateQuota(spaceNeeded * 2);    
//                }         
        });
        /*webView.setWebViewClient(new WebViewClient(){
        	//点击链接继续在当前browser中响应   
        	@Override  
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {  
//        		view.loadUrl(url);         
//        		return true;
//        		Uri uri = Uri.parse(url); //url为你要链接的地址
//              Intent intent =new Intent(Intent.ACTION_VIEW, uri);
//              context.startActivity(intent);
        		return super.shouldOverrideUrlLoading(view, url) ;
        		
        	}
        	
			@Override
			public void onPageFinished(WebView view, String url) {
				//网页加载好了  开始加载图片
				view.getSettings().setBlockNetworkImage(false);
				super.onPageFinished(view, url);
			}
		});*/
	}
	
	/**
	  * 同步一下cookie 
	  * @param context
	  * @param url
	  * @param cookies
	  */
	public static void synCookies(Context context, String url,String cookies) {
		 CookieSyncManager.createInstance(context);
		 CookieManager cookieManager = CookieManager.getInstance();
	     cookieManager.setAcceptCookie(true);
	     cookieManager.removeSessionCookie();//移除 
	     cookieManager.setCookie(url, cookies);//cookies是在HttpClient中获得的cookie
	     CookieSyncManager.getInstance().sync();
	 }  
	 
	/**
	 * webview 的属性设置
	 * 
	 * @param webView
	 */
	public static void setWebviewProperty(WebView webView, Context ac) {
//	    final	ProgressDialog pd = new ProgressDialog(ac);
//		pd.setMessage("loading...");
	    final WebSettings webSettings = webView.getSettings();
		/*DisplayMetrics metrics = new DisplayMetrics();
		ac.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int mDensity = metrics.densityDpi;
		if (mDensity == 240) { // 可以让不同的density的情况下，可以让页面进行适配
			webSettings.setDefaultZoom(ZoomDensity.FAR);
		} else if (mDensity == 160) {
			webSettings.setDefaultZoom(ZoomDensity.MEDIUM);
		} else if (mDensity == 120) {
			webSettings.setDefaultZoom(ZoomDensity.CLOSE);
		}*/
		// 设置js调用 启用
		webSettings.setJavaScriptEnabled(true);
		// 前台的文本框会有焦点
		webView.requestFocus();
		//提高渲染的优先级
		webView.getSettings().setRenderPriority(RenderPriority.HIGH);
		// 自动的适应屏幕
		webSettings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		// 水平不显示
		webView.setHorizontalScrollBarEnabled(false);
		// 垂直不显示
		webView.setVerticalScrollBarEnabled(true);
	    webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET) ;
		//android- WebView 文字 、图片分开加载
		//加载url前，设置图片阻塞
		webSettings.setBlockNetworkImage(true);
		//cache的模式 有待实验
		//webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		webView.setWebViewClient(new WebViewClient(){

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				// TODO Auto-generated method stub
				super.onPageStarted(view, url, favicon);
				//pd.show();
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				// TODO Auto-generated method stub
				//网页加载好了  开始加载图片
				webSettings.setBlockNetworkImage(false);
				super.onPageFinished(view, url);
				//pd.dismiss();
			}
			
		});
	}

}