package net.ibaixin.chat.activity;

import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.JokeFragment;
import net.ibaixin.chat.view.PagerSlidingTabStrip2;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
/**
 * 趣味阅读主界面
 * @author dudejin
 */
public class JokeReadMainActivity extends BaseActivity {
	
	/**
	 * 所有内容Fragment
	 */
	private JokeFragment jokeFragment;
	/**
	 * 搞笑段子Fragment
	 */
	private JokeFragment joketextFragment;

	/**
	 * 搞笑视频Fragment
	 */
	private JokeFragment jokelifeFragment;

	/**
	 * 趣图Fragment
	 */
	private JokeFragment jokepicFragment;

	/**
	 * PagerSlidingTabStrip的实例
	 */
	private PagerSlidingTabStrip2 tabs;

	/**
	 * 获取当前屏幕的密度
	 */
	private DisplayMetrics dm;

	/**
	 * 对PagerSlidingTabStrip的各项属性进行赋值。
	 */
	private void setTabsValue() {
		// 设置Tab是自动填充满屏幕的
		tabs.setShouldExpand(true);
		// 设置Tab的分割线是透明的
		tabs.setDividerColor(Color.TRANSPARENT);
		// 设置Tab底部线的高度
		tabs.setUnderlineHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm));
		// 设置Tab Indicator的高度
		tabs.setIndicatorHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm));
		// 设置Tab标题文字的大小
		tabs.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, dm));
		// 设置Tab Indicator的颜色
		tabs.setIndicatorColor(Color.parseColor("#03a9f4"));
		// 设置选中Tab文字的颜色 (这是我自定义的一个方法)
		tabs.setSelectedTextColor(Color.parseColor("#03a9f4"));
		// 取消点击Tab时的背景色
		tabs.setTabBackground(0);
	}

	public class MyPagerAdapter extends FragmentPagerAdapter {

		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		private final String[] titles = getResources().getStringArray(R.array.jokeread_function_lable);

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}

		@Override
		public int getCount() {
			return titles.length;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				if (jokeFragment == null) {
					jokeFragment = new JokeFragment(0);
				}
				return jokeFragment;
			case 1:
				if (joketextFragment == null) {
					joketextFragment = new JokeFragment(1);
				}
				return joketextFragment;
			case 2:
				if (jokepicFragment == null) {
					jokepicFragment = new JokeFragment(2);
				}
				return jokepicFragment;
			case 3:
				if (jokelifeFragment == null) {
					jokelifeFragment = new JokeFragment(3);
				}
				return jokelifeFragment;
			default:
				if (jokeFragment == null) {
					jokeFragment = new JokeFragment(0);
				}
				return jokeFragment;
			}
		}

	}

	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_jokelist;
	}

	@Override
	protected void initView() {
		dm = getResources().getDisplayMetrics();
		ViewPager pager = (ViewPager) findViewById(R.id.joke_pager);
		tabs = (PagerSlidingTabStrip2) findViewById(R.id.joke_tabs);
		pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
		pager.setOffscreenPageLimit(3);//设置缓存fragment个数
		tabs.setViewPager(pager);
		setTabsValue();
	}
	
	@Override
	protected void initData() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void addListener() {
		// TODO Auto-generated method stub
		
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
		case R.id.action_take_pic://发段子
			intent = new Intent(mContext, JokeAddActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}