package net.ibaixin.chat.fragment;

import java.io.File;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.ChatImagePreviewActivity;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.DownLoadFileUtil;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.WebviewSettings;
import net.ibaixin.chat.view.ProgressDialog;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
/**
 * 趣图Fragment的界面
 * 
 * @author dudejin
 */
public class JokeFragment extends BaseFragment{
	private String url = null ;
//	private final String listJokesAll = "http://192.168.42.28/ibaixin/jokemobile/listJokesAll" ;
	private final String listJokesAll = Constants.websitePrefix+"/ibaixin/jokemobile/listJokesAll" ;
	private final String listJokesText = Constants.websitePrefix+"/ibaixin/jokemobile/listJokesText" ;
	private final String listJokesPic = Constants.websitePrefix+"/ibaixin/jokemobile/listJokesPic" ;
	private final String listJokesLife = Constants.websitePrefix+"/ibaixin/jokemobile/listJokesLife" ;
	private final String readimgPrefix = Constants.websitePrefix+"/ibaixin/jokemobile/" ;
	private ProgressDialog proDialog;
	public static boolean isNeedReload = false ;
//    /** Fragment当前状态是否可见 */
//    protected boolean isVisible ;
//    /** 标志位，标志已经初始化完成 */
//    private boolean isPrepared = false;
//    /** 是否已被加载过一次，第二次就不再去请求数据了 */
//    private boolean mHasLoadedOnce;
//    /** 0、1、2、3分别代表：全部、笑话、趣图、感悟*/
	private int type = 0 ;
	/**
	 * 定义浏览器内核对象
	 */
	private WebView webView;
	/**
	 * 下拉刷新view
	 */
	private SwipeRefreshLayout swipeLayout;
	
	/**
	 * @param type 0、1、2、3分别代表：全部、笑话、趣图、感悟
	 */
	public JokeFragment(int type) {
		this.type = type ;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_joke, container, false);
		webView = (WebView) view.findViewById(R.id.jokeswebView) ;
		swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
		switch (type) {
		case 1:
			url = listJokesText;
			break;
		case 2:
			url = listJokesPic;
			break;
		case 3:
			url = listJokesLife;
			break;
		default:
			url = listJokesAll ;
			break;
		}
		WebviewSettings.initWebView(mContext,webView, swipeLayout);
		webView.addJavascriptInterface(new PhotoPreviewJS(), "mobileJS");
		webView.loadUrl(url);
//		isPrepared = true ;
//		lazyLoad() ;
		return view;
	}

	/*protected void lazyLoad() {
		if (!isPrepared || !isVisible || mHasLoadedOnce) {
            return;
        }
		WebviewSettings.initWebView(webView, swipeLayout, url);
		mHasLoadedOnce = true;
	}
	
	 @Override
     public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(getUserVisibleHint()) {
            isVisible = true;
            lazyLoad();
        } else {
            isVisible = false;
        }
     }*/
	 
	@Override
	public void onDestroyView() {
		webView.loadDataWithBaseURL(null, "","text/html", "utf-8",null);//清理缓存
		super.onDestroyView();
	}
	
	@Override
	public void onResume() {
		if(isNeedReload)
			webView.loadUrl(webView.getUrl());
		isNeedReload = false ;
		super.onResume();
	}
	
	/**
	 * 定制js组件
	 */
	private final class PhotoPreviewJS {
		@JavascriptInterface
		public void showImgage(final String url, String jokeid) {
			try {
				final String filepathName = SystemUtil.getWebViewPath() + jokeid+".gif";
				File f = new File(filepathName);
				if (f.exists()) {// 如果本地不存在就去下载
					Intent intent = new Intent(mContext,ChatImagePreviewActivity.class);
					intent.putExtra(ChatImagePreviewActivity.ARG_IMAGE_PATH,filepathName);
					intent.putExtra(PhotoFragment.ARG_TOUCH_FINISH, true);
					ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(webView, 0, 0,webView.getWidth(), webView.getHeight());
					ActivityCompat.startActivity((Activity) mContext, intent,options.toBundle());
				} else {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							new LoadImgTask().execute(url , filepathName);
						}
					});
				}
			} catch (Exception e) {
				SystemUtil.makeShortToast("图片加载失败");
			}
		}
		
	/*	@JavascriptInterface
		public void shareLink(String content,String linkurl){
			 Intent intent=new Intent(Intent.ACTION_SEND);    
             intent.setType("text/plain");
//             intent.setType("image/*");    
             intent.putExtra(Intent.EXTRA_SUBJECT, "百信趣味阅读");
             intent.putExtra(Intent.EXTRA_TEXT, "【百信趣味阅读】"+linkurl);
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             startActivity(Intent.createChooser(intent, getResources().getString(R.string.share))); 
		}*/
		@JavascriptInterface
		public void shareLink(String linkurl,String content){
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
//             intent.setType("image/*");
//			intent.putExtra(Intent.EXTRA_SUBJECT, "百信趣味阅读");
			intent.putExtra(Intent.EXTRA_TEXT, content+"【来自百信趣味阅读】"+linkurl);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
		}
		
		// ajax 请求错误
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
	 * 加载图片异步任务
	 * @author Administrator
	 * @update 2014年10月7日 上午9:55:00
	 *
	 */
	class LoadImgTask extends AsyncTask<String, Integer, String> {
		
		@Override
		protected void onPreExecute() {
			if (proDialog == null) {
				proDialog = ProgressDialog.show(mContext, null, getString(R.string.imgloading), true);
			} else {
				proDialog.show();
			}
		}

		@Override
		protected String doInBackground(String... params) {
			String picUrl = params[0] ;
			String filepathName = params[1] ;
			try {
				DownLoadFileUtil.download(picUrl.startsWith("http://") ? picUrl : readimgPrefix+"readJokeImage?imgfile="+picUrl, filepathName);
				return filepathName ;
			} catch (Exception e) {
				Log.e("LoadImgTask", e.toString());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(proDialog != null && proDialog.isShowing()) {
				proDialog.dismiss();
			}
			if(result!=null){
				Intent intent = new Intent(mContext, ChatImagePreviewActivity.class);
				intent.putExtra(ChatImagePreviewActivity.ARG_IMAGE_PATH, result);
				intent.putExtra(PhotoFragment.ARG_TOUCH_FINISH, true);
				ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(webView, 0, 0, webView.getWidth(), webView.getHeight());
				ActivityCompat.startActivity((Activity) mContext, intent, options.toBundle());
			}else{
				SystemUtil.makeShortToast(R.string.imgload_error);
			}
		}
		
	}
}
