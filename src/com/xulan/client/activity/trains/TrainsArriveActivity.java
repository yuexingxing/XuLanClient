package com.xulan.client.activity.trains;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.xulan.client.R;
import com.xulan.client.activity.BaseActivity;
import com.xulan.client.data.ScanData;
import com.xulan.client.net.AsyncNetTask;
import com.xulan.client.net.AsyncNetTask.OnPostExecuteListener;
import com.xulan.client.net.LoadTextNetTask;
import com.xulan.client.net.LoadTextResult;
import com.xulan.client.net.NetTaskResult;
import com.xulan.client.service.UserService;
import com.xulan.client.util.CommandTools;
import com.xulan.client.util.Constant;
import com.xulan.client.util.Logs;
import com.xulan.client.util.RequestUtil;
import com.xulan.client.util.RequestUtil.RequestCallback;
import com.xulan.client.view.CustomProgress;

/** 
 * 确认抵达
 * 
 * @author hexiuhui
 *
 */
public class TrainsArriveActivity extends BaseActivity {

	@ViewInject(R.id.trains_arrive_edt_1) EditText tvTaskName;
	@ViewInject(R.id.trains_arrive_edt_2) EditText edtWagonNumber;
	@ViewInject(R.id.trains_arrive_edt_3) EditText edtTrain;
	@ViewInject(R.id.trains_arrive_edt_4) EditText edtCount;
	@ViewInject(R.id.trains_arrive_edt_5) EditText edtUser;
	@ViewInject(R.id.trains_arrive_edt_6) EditText edtTelPhone;

	private String link_no;
	private String taskId;

	@Override
	protected void onBaseCreate(Bundle savedInstanceState) {
		setContentViewId(R.layout.activity_trains_arrive, this);
		ViewUtils.inject(this);
	}

	@Override
	public void initView() {
		link_no = getIntent().getStringExtra("link_no");
		taskId = getIntent().getStringExtra("taskCode");
		tvTaskName.setText(getIntent().getStringExtra("taskName"));
	}

	@Override
	public void initData() {
		setTitle(getResources().getString(R.string.arrival));
		getArriveData(taskId);
	}

	public void getArriveData(String task_id){

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("task_id", task_id);
			jsonObject.put("type", Constant.SCAN_TYPE_RAILEAY);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		CustomProgress.showDialog(mContext, "数据获取中", false, null);
		RequestUtil.postDataIfToken(mContext, "action/arrive", jsonObject, false, new RequestCallback() {

			@Override
			public void callback(int res, String remark, JSONObject jsonObject) {
				// TODO 自动生成的方法存根
				CustomProgress.dissDialog();
				CommandTools.showToast(remark);
				if(res == 0){

					jsonObject = jsonObject.optJSONObject("data");
					Log.i("hexiuhui----", jsonObject.toString());

					edtWagonNumber.setText(jsonObject.optString("Wagon_Code"));
					edtTrain.setText(jsonObject.optString("Train_No"));
					edtCount.setText(jsonObject.optInt("GoodsCount") + "");
				}
			}
		});
	}

	/**
	 * 上传数据
	 */
	protected LoadTextNetTask requestUpload(List<ScanData> list, String taskId) {

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

							finish();
						} else {

						}
					} catch (JSONException e) {
						Toast.makeText(TrainsArriveActivity.this, "解析错误", 1).show();
						e.printStackTrace();
					}
				} else {
				}
			}
		};

		CustomProgress.showDialog(TrainsArriveActivity.this, getResources().getString(R.string.searching), false, null);
		LoadTextNetTask task = UserService.arrive(list, taskId, listener, null);
		return task;
	}

	/**
	 * 提交
	 * @param v
	 */
	public void sureArrive(View v){

		String strTaskName = tvTaskName.getText().toString();
		if(TextUtils.isEmpty(strTaskName)){
			CommandTools.showToast(getResources().getString(R.string.select_task));
			return;
		}

		String edtWagonNumberValue = edtWagonNumber.getText().toString();
		String edtTrainValue = edtTrain.getText().toString();
		String edtTelPhoneValue = edtTelPhone.getText().toString();
		String count_txt_value = edtCount.getText().toString();

		ScanData data = new ScanData();
		data.setTaskName(strTaskName);
		data.setCacheId(CommandTools.getUUID());
		data.setTelPerson(edtTelPhoneValue);
		data.setWagonNumber(edtWagonNumberValue);
		data.setTrain(edtTrainValue);
		data.setPlanCount(count_txt_value);
		data.setScanTime(CommandTools.getTime());
		data.setCreateTime(CommandTools.getTime());
		data.setScaned("1");
		data.setUploadStatus("0");
		data.setTaskId(taskId);

		List<ScanData> scanDataList = new ArrayList<ScanData>();
		scanDataList.add(data);

		requestUpload(scanDataList, taskId);
	}
}
