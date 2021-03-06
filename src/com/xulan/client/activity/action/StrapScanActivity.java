package com.xulan.client.activity.action;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.xulan.client.MyApplication;
import com.xulan.client.R;
import com.xulan.client.activity.BaseActivity;
import com.xulan.client.activity.action.load.LoadCompanyActivity;
import com.xulan.client.adapter.CommonAdapter;
import com.xulan.client.adapter.ViewHolder;
import com.xulan.client.camera.CaptureActivity;
import com.xulan.client.data.ScanData;
import com.xulan.client.data.ScanInfo;
import com.xulan.client.data.ScanNumInfo;
import com.xulan.client.db.dao.ScanDataDao;
import com.xulan.client.net.AsyncNetTask;
import com.xulan.client.net.AsyncNetTask.OnPostExecuteListener;
import com.xulan.client.net.LoadTextNetTask;
import com.xulan.client.net.LoadTextResult;
import com.xulan.client.net.NetTaskResult;
import com.xulan.client.pdascan.RFID;
import com.xulan.client.service.UserService;
import com.xulan.client.util.CommandTools;
import com.xulan.client.util.Constant;
import com.xulan.client.util.DataUtilTools;
import com.xulan.client.util.HandleDataTools;
import com.xulan.client.util.Logs;
import com.xulan.client.util.PostTools;
import com.xulan.client.util.PostTools.ObjectCallback;
import com.xulan.client.util.VoiceHint;
import com.xulan.client.view.CustomProgress;

/** 
 * 绑扎
 * 
 * @author yxx
 *
 * @date 2017-6-7 下午1:50:29
 * 
 */
public class StrapScanActivity extends BaseActivity implements OnClickListener {
	@ViewInject(R.id.lv_public) ListView mListView;
	@ViewInject(R.id.strap_edt_taskname) EditText edtTaskName;//任务名称
	@ViewInject(R.id.strap_edt_commpany) EditText edtCompany;

	@ViewInject(R.id.strap_edt_package_billcode) EditText edtPackBarcode;//包装条码
	@ViewInject(R.id.strap_edt_package_number) EditText edtPackNo;//包装号码

	private EditText strap_count;
	
	@ViewInject(R.id.strap_edt_remark) EditText edtRemark;//绑扎信息

	private RelativeLayout billCodeImg;

	private CommonAdapter<ScanData> commonAdapter;
	private List<ScanData> dataList = new ArrayList<ScanData>();
	private List<ScanData> uploadList = new ArrayList<ScanData>();

	private String strCompanyName;
	private String strCompanyId;
	private String taskId;
	private int scan_num = 0;

	private ScanDataDao mScandataDao = new ScanDataDao();

	@Override
	protected void onBaseCreate(Bundle savedInstanceState) {
		setContentViewId(R.layout.activity_strap_scan, this);

		ViewUtils.inject(this);

		if(MyApplication.m_flag == 0 && HandleDataTools.getFirstLinkNum() == MyApplication.m_physic_link_num){
			requestGetShip(MyApplication.m_userID, taskId, 0);
		}
	}

	@Override
	public void initView() {
		strap_count = (EditText) findViewById(R.id.strap_count);
		billCodeImg = (RelativeLayout) findViewById(R.id.bill_code_img);
		//本地数据
		dataList = mScandataDao.getNotUploadDataList(MyApplication.m_scan_type, MyApplication.m_link_num + "", MyApplication.m_nodeId);
		
		scan_num = dataList.size();
		
		mListView.setAdapter(commonAdapter = new CommonAdapter<ScanData>(mContext, dataList, R.layout.land_item) {
			@Override
			public void convert(ViewHolder helper, ScanData item) {

				helper.setText(R.id.land_tv1, commonAdapter.getIndex());
				helper.setText(R.id.land_tv2, item.getPackBarcode());
				helper.setText(R.id.land_tv3, item.getPackNumber());
				helper.setText(R.id.land_tv4, item.getScanUser());
				helper.setText(R.id.land_tv5, item.getScanTime());
			}
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				ScanData scanData = dataList.get(arg2);

				edtPackBarcode.setText(scanData.getPackBarcode());
				edtPackNo.setText(scanData.getPackNumber());
				edtRemark.setText(scanData.getMemo());
			}
		});

		billCodeImg.setOnClickListener(this);
	}

	@Override
	public void initData() {

		setTitle(getResources().getString(R.string.lashing_service));
		setRightTitle(getResources().getString(R.string.submit));
	}

	public void onEventMainThread(Message msg) {

		ScanInfo scanInfo = (ScanInfo) msg.obj;
		if(scanInfo.getWhat() == Constant.SCAN_DATA && scanInfo.getType().equals(Constant.SCAN_TYPE_STRAP)){

			String strBillcode = scanInfo.getBarcode();
			edtPackBarcode.setText(strBillcode);

			ScanData scanData = DataUtilTools.checkScanData(Constant.SCAN_TYPE_STRAP, strBillcode, dataList);
			if(scanData != null){

				edtPackBarcode.setText(scanData.getPackBarcode());
				edtPackNo.setText(scanData.getPackNumber());
				addData(null);
			}else{
				VoiceHint.playErrorSounds();
				CommandTools.showToast(getResources().getString(R.string.barcode_does_not_exist));
			}
		}
	}


	/**
	 * 任务名称选择
	 * @param v
	 */
	public void chooseTask(View v) {
		Intent intent = new Intent(mContext, TaskListActivity.class);
		intent.putExtra("type", Constant.SCAN_TYPE_STRAP);
		intent.putExtra("link_no", MyApplication.m_link_num + "");
		startActivityForResult(intent, Constant.SELECT_TASK);
	}

	/**
	 * 选择公司
	 * @param v
	 */
	public void chooseCompany(View v) {
		Intent intent = new Intent(mContext, LoadCompanyActivity.class);
		intent.putExtra("type", Constant.SCAN_TYPE_STRAP);
		intent.putExtra("link_no", MyApplication.m_link_num + "");
		startActivityForResult(intent, Constant.SELECT_COMPANY);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bill_code_img:
			// 扫描
			Intent openCameraIntent = new Intent(StrapScanActivity.this, CaptureActivity.class);
			startActivityForResult(openCameraIntent, Constant.CAPTURE_BILLCODE);
			break;

		default:
			break;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == Constant.SELECT_TASK && resultCode == RESULT_OK) {
			scan_num = 0;
			edtTaskName.setText(data.getStringExtra("taskName"));
			taskId = data.getStringExtra("taskCode");

			strCompanyName = data.getStringExtra("car_plate");
			strCompanyId = data.getStringExtra("car_count");

			edtCompany.setText(strCompanyName);

			edtPackBarcode.setText("");
			edtPackNo.setText("");

			requestGetShip(MyApplication.m_userID, taskId, MyApplication.m_flag);
		} else if(requestCode == Constant.SELECT_COMPANY && resultCode == RESULT_OK){

			strCompanyName = data.getStringExtra("companyName");
			strCompanyId = data.getStringExtra("company_id");

			edtCompany.setText(strCompanyName);
		} else if (requestCode == Constant.CAPTURE_BILLCODE) {
			if (data == null) {
				return;
			}

			Bundle bundle = data.getExtras();
			String strBillcode = bundle.getString("result");

			ScanData scanData = DataUtilTools.checkScanData(Constant.SCAN_TYPE_STRAP, strBillcode, dataList);
			if (scanData != null) {

				edtPackBarcode.setText(scanData.getPackBarcode());
				edtPackNo.setText(scanData.getPackNumber());
				addData(null);
			} else {
				VoiceHint.playErrorSounds();
				CommandTools.showToast("条码不存在");
			}
			return;
		}
	}

	private int scan_count_num = 0;
	
	/**
	 * 获取海运信息
	 */
	protected LoadTextNetTask requestGetShip(String userId, final String taskId, int flag) {
		PostTools.getLoadNumber(mContext, taskId, new ObjectCallback() {
			@Override
			public void callback(int res, String remark, Object object) {

				ScanNumInfo info = (ScanNumInfo) object;

				scan_count_num = info.getMust_scan_number();
				strap_count.setText(scan_num + " / " + info.getMust_scan_number());
			}
		});

		OnPostExecuteListener listener = new OnPostExecuteListener() {
			@Override
			public void onPostExecute(AsyncNetTask t, NetTaskResult result, Object tag) {

				CustomProgress.dissDialog();
				if (result.m_nResultCode == 0) {
					LoadTextResult mresult = (LoadTextResult) result;
					try {
						JSONObject jsonObj = new JSONObject(mresult.m_strContent);

						Logs.i("hexiuhui---", jsonObj.toString());

						int success = jsonObj.getInt("success");
						if (success == 0) {
							JSONArray jsonArray = jsonObj.getJSONArray("data");
							dataList.clear();
							List<ScanData> list = new ArrayList<ScanData>();
							for (int i = 0; i < jsonArray.length(); i++) {

								JSONObject jsonObject = jsonArray.getJSONObject(i);
								String pack_number = jsonObject.optString("Pack_No");
								String pack_code = jsonObject.optString("Pack_BarCode");
								String goods_id = jsonObject.optString("ID");

								ScanData scanData = new ScanData();
								scanData.setCacheId(CommandTools.getUUID());
								scanData.setPackBarcode(pack_code);
								scanData.setPackNumber(pack_number);
								scanData.setMainGoodsId(goods_id);

								list.add(scanData);
							}
							List<ScanData> notUploadDataList = mScandataDao.getNotUploadDataList(MyApplication.m_scan_type, MyApplication.m_link_num + "", MyApplication.m_nodeId, taskId);
							dataList.addAll(notUploadDataList);
							
							//去除重复数据
							for (int j = 0; j < list.size(); j++) {
								for (int i = 0; i < dataList.size(); i++) {
									ScanData scanData = dataList.get(i);
									if (scanData.getPackNumber().equals(list.get(j).getPackNumber())) {
										list.remove(j);
									}
								}
							}
							dataList.addAll(list);
							commonAdapter.notifyDataSetChanged();

							RFID.startRFID();
						} else {

							String message = jsonObj.getString("message");
							CommandTools.showToast(message);
						}

					} catch (JSONException e) {
						Toast.makeText(StrapScanActivity.this, "解析错误", 1).show();
						e.printStackTrace();
					}
				} else {
				}
			}
		};

		CustomProgress.showDialog(StrapScanActivity.this, getResources().getString(R.string.searching), false, null);
		LoadTextNetTask task = UserService.getLand(userId, taskId, flag, listener, null);
		return task;
	}

	/**
	 * 保存数据
	 * @param v
	 */
	public void addData(View v) {
		String strTaskName = edtCompany.getText().toString();

		if(TextUtils.isEmpty(strTaskName)){
			CommandTools.showToast("绑扎公司不能为空");
			return;
		}

		String strPackageBarcode = edtPackBarcode.getText().toString();
		if (TextUtils.isEmpty(strPackageBarcode)) {

			VoiceHint.playErrorSounds();
			CommandTools.showToast(getResources().getString(R.string.code_not_null));
			return;
		}

		String strPackageNumber = edtPackNo.getText().toString();
		if (TextUtils.isEmpty(strPackageNumber)) {

			VoiceHint.playErrorSounds();
			CommandTools.showToast("请录入包装号码");
			return;
		}

		for (int i = 0; i < dataList.size(); i++) {

			ScanData data = dataList.get(i);

			if (data.getPackBarcode().equals(strPackageBarcode) && data.getScaned().equals("1")) {
				VoiceHint.playErrorSounds();
				CommandTools.showToast("条码已扫描");
				return;
			}

			if (data.getPackNumber().equals(strPackageNumber)) {

				data.setTaskName(strTaskName);
				data.setTaskId(taskId);
				data.setPackBarcode(strPackageBarcode);
				data.setCompany(strCompanyName);
				data.setCompany_id(strCompanyId);
				data.setMemo(edtRemark.getText().toString());
				data.setScanTime(CommandTools.getTime());
				data.setLink(MyApplication.m_link_num + "");
				data.setScanType(Constant.SCAN_TYPE_STRAP);
				data.setScanUser(MyApplication.m_userName);
				data.setNode_id(MyApplication.m_nodeId);
				data.setScaned("1");
				data.setUploadStatus("0");

				mScandataDao.addData(data);  //保存数据
				CommandTools.showToast("保存成功");
			}
		}

		commonAdapter.notifyDataSetChanged();

		strap_count.setText(++scan_num + " / " + scan_count_num);
		edtPackBarcode.setText("");
		edtPackNo.setText("");
	}

	/* (non-Javadoc)
	 * @see com.xulan.client.activity.BaseActivity#clickRight(android.view.View)
	 */
	public void clickRight(View v){

		uploadList.clear();
		for (int i = 0; i < dataList.size(); i++) {
			if (!TextUtils.isEmpty(dataList.get(i).getScanTime())) {
				uploadList.add(dataList.get(i));
			}
		}

		if (uploadList.size() <= 0) {
			CommandTools.showToast(getResources().getString(R.string.scan_records));
		} else {
			requestUpload(uploadList, taskId);
		}
	}

	/**
	 * 上传数据
	 */
	protected LoadTextNetTask requestUpload(final List<ScanData> list, final String taskId) {

		OnPostExecuteListener listener = new OnPostExecuteListener() {

			@Override
			public void onPostExecute(AsyncNetTask t, NetTaskResult result, Object tag) {

				CustomProgress.dissDialog();
				if (result.m_nResultCode == 0) {

					LoadTextResult mresult = (LoadTextResult) result;
					try {
						JSONObject jsonObj = new JSONObject(mresult.m_strContent);

						Logs.i("hexiuhui---", jsonObj.toString());

						int success = jsonObj.getInt("success");
						String message = jsonObj.getString("message");
						CommandTools.showToast(message);

						if (success == 0) {
							//修改上传状态
							mScandataDao.updateUploadState(list);
							HandleDataTools.handleUploadData(commonAdapter, dataList, uploadList);
							
							PostTools.getLoadNumber(mContext, taskId, new ObjectCallback() {
								@Override
								public void callback(int res, String remark, Object object) {

									ScanNumInfo info = (ScanNumInfo) object;

									scan_count_num = info.getMust_scan_number();
									strap_count.setText(scan_num + " / " + info.getMust_scan_number());
								}
							});
							
						}
					} catch (JSONException e) {
						Toast.makeText(StrapScanActivity.this, "解析错误", 1).show();
						e.printStackTrace();
					}
				} else {

				}
			}
		};

		CustomProgress.showDialog(StrapScanActivity.this, getResources().getString(R.string.searching), false, null);
		LoadTextNetTask task = UserService.upload(list, taskId, Constant.SCAN_TYPE_STRAP, listener, null);
		return task;
	}

	/* (non-Javadoc)
	 * @see com.xulan.client.activity.BaseActivity#sortByPackBarcode(android.view.View)
	 */
	public void sortByPackBarcode(View v){

		DataUtilTools.sortByPackBarCode(dataList, commonAdapter);
	}

	/* (non-Javadoc)
	 * @see com.xulan.client.activity.BaseActivity#sortByPackNo(android.view.View)
	 */
	public void sortByPackNo(View v){

		DataUtilTools.sortByPackNo(dataList, commonAdapter);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	public void onStop(){
		super.onStop();

		RFID.stopRFID();
	}
}
