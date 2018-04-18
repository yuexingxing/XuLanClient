package com.xulan.client.activity.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import com.xulan.client.MyApplication;
import com.xulan.client.R;
import com.xulan.client.activity.BaseActivity;
import com.xulan.client.adapter.MenuAdapter;
import com.xulan.client.adapter.MenuAdapter.OnBottomClickListener;
import com.xulan.client.data.MenuInfo;
import com.xulan.client.data.ScanInfo;
import com.xulan.client.pdascan.RFID;
import com.xulan.client.pdascan.RFID_Scan;
import com.xulan.client.util.CommandTools;
import com.xulan.client.util.Constant;
import com.xulan.client.util.MenuInfoUtil;

/** 
 * 主界面(登录后从后台获取动作)
 * 
 * @author yxx
 *
 * @date 2016-12-15 下午3:00:47
 * 
 */
public class MainMenuActivity extends BaseActivity {

	private String action;
	private GridView mGridView;
	private MenuAdapter scanMenuAdapter;

	private List<MenuInfo> dataList = new ArrayList<MenuInfo>();

	@Override
	protected void onBaseCreate(Bundle savedInstanceState) {
		setContentViewId(R.layout.activity_main_menu, this);

		action = getIntent().getStringExtra("action");
	}

	@Override
	public void initView() {
		new MenuInfoUtil().getMenuList(dataList, action, this);

		mGridView = (GridView) findViewById(R.id.gv_menu_home);
		scanMenuAdapter = new MenuAdapter(mContext, dataList, action, new OnBottomClickListener(){

			@Override
			public void onBottomClick(View v, int position) {

				if(dataList.get(position).getActivity() != null) {
					MenuInfo menuInfo = dataList.get(position);

					Intent intent = new Intent(MainMenuActivity.this, (Class<?>) menuInfo.getActivity());

					intent.putExtra("data", (Serializable)dataList);
					intent.putExtra("actionId", menuInfo.getActionId());
					intent.putExtra("link_no", position + 1);
					intent.putExtra("type", menuInfo.getScanType());
					intent.putExtra("actionName", menuInfo.getActionName());
					intent.putExtra("taskNum", menuInfo.getTaskNum());
					intent.putExtra("pos", position + 1);

					if (menuInfo.getActivity() == TaskListActivity.class) {
						intent.putExtra("isArriver", 1);
					}

					MyApplication.m_link_num = menuInfo.getLinkNum();
					MyApplication.m_physic_link_num = menuInfo.getLinkNum();
					MyApplication.m_scan_type = menuInfo.getScanType();

					startActivity(intent);
				}
			}
		});

		mGridView.setAdapter(scanMenuAdapter);
		scanMenuAdapter.notifyDataSetChanged();
	}

	@Override
	public void initData() {

		setTitle(action);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	public void onResume(){
		super.onResume();

		MyApplication.m_scan_type = Constant.SCAN_TYPE_MENU;
		if(CommandTools.getDeviceName().equals("Android")){
			RFID.closeRFID();
			RFID_Scan.initRFIDScanner(MyApplication.getInstance());
		}
	}

	/* (non-Javadoc)
	 * @see com.xulan.client.activity.BaseActivity#onEventMainThread(android.os.Message)
	 */
	public void onEventMainThread(Message msg) {

		ScanInfo scanInfo = (ScanInfo) msg.obj;
		if(scanInfo.getWhat() == Constant.SCAN_DATA && scanInfo.getType().equals(Constant.SCAN_TYPE_MENU)){
			//			Toast.makeText(this, "菜单栏", Toast.LENGTH_LONG).show();
		}
	}

}
