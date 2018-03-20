package com.xulan.client.activity.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.xulan.client.MyApplication;
import com.xulan.client.R;
import com.xulan.client.activity.BaseActivity;
import com.xulan.client.adapter.CommonAdapter;
import com.xulan.client.adapter.ViewHolder;
import com.xulan.client.camera.CaptureActivity;
import com.xulan.client.data.LinkInfo;
import com.xulan.client.data.ScanData;
import com.xulan.client.db.dao.ScanDataDao;
import com.xulan.client.net.AsyncNetTask;
import com.xulan.client.net.AsyncNetTask.OnPostExecuteListener;
import com.xulan.client.net.LoadTextNetTask;
import com.xulan.client.net.LoadTextResult;
import com.xulan.client.net.NetTaskResult;
import com.xulan.client.service.UserService;
import com.xulan.client.takephoto.Bimp;
import com.xulan.client.takephoto.PhotoMenuActivity;
import com.xulan.client.util.CommandTools;
import com.xulan.client.util.Constant;
import com.xulan.client.util.FileUtils;
import com.xulan.client.util.ImageTool;
import com.xulan.client.util.Logs;
import com.xulan.client.util.PostTools;
import com.xulan.client.util.RequestUtil;
import com.xulan.client.util.Res;
import com.xulan.client.util.RequestUtil.RequestCallback;
import com.xulan.client.util.VoiceHint;
import com.xulan.client.view.CustomProgress;
import com.xulan.client.view.SingleItemDialog;
import com.xulan.client.view.SingleItemDialog.SingleItemCallBack;

/** 
 * 拍照
 * 
 * @author yxx
 *
 * @date 2016-12-20 下午4:00:43
 * 
 */
public class ActionPhotoActivity extends BaseActivity implements OnClickListener {

	@ViewInject(R.id.lv_public) ListView mListView;
	@ViewInject(R.id.action_photo_tv_1) EditText edtLink;
	@ViewInject(R.id.action_photo_tv_2) EditText edtBillcode;
	@ViewInject(R.id.action_photo_tv_3) EditText edtNumber;
	@ViewInject(R.id.action_photo_tv_4) EditText edtCount;
	
	@ViewInject(R.id.action_photo_tv1) TextView tvItemBarcode;
	@ViewInject(R.id.action_photo_tv2) TextView tvItemBarno;
	
	@ViewInject(R.id.photo_tv_barcode) TextView tvBarcode;
	@ViewInject(R.id.photo_tv_barno) TextView tvBarno;

	private LinearLayout photo_layout;
	private RelativeLayout billCodeImg;

	private GridView gridView;
	private GVAdapter adapter;
	private static final String IMG_ADD_TAG = "a";
	private List<String> photoList = new ArrayList<String>();
	private CommonAdapter<ScanData> commonAdapter;
	private List<ScanData> dataList = new ArrayList<ScanData>();

	final List<LinkInfo> linkList = new ArrayList<LinkInfo>();

	private static final int TAKE_PICTURE = 0x000001;
	private static final int TAKE_FOLDER = 0x000002;
	
	private ScanDataDao mScanDataDao = new ScanDataDao();

	@Override
	protected void onBaseCreate(Bundle savedInstanceState) {
		setContentViewId(R.layout.activity_action_photo, this);
		ViewUtils.inject(this);

	}

	@Override
	public void initView() {
		setTitle(getResources().getString(R.string.photo));
		setRightTitle(getResources().getString(R.string.submit));
		photo_layout = (LinearLayout) findViewById(R.id.photo_layout);
		gridView = (GridView) findViewById(R.id.gridview);
		
		billCodeImg = (RelativeLayout) findViewById(R.id.bill_code_img);
		
		dataList = mScanDataDao.getNotUploadDataList(MyApplication.m_scan_type, MyApplication.m_link_num + "", MyApplication.m_nodeId);
		
		mListView.setAdapter(commonAdapter = new CommonAdapter<ScanData>(mContext, dataList, R.layout.action_photo_item) {
			@Override
			public void convert(ViewHolder helper, ScanData item) {

				helper.setText(R.id.action_photo_tv0, commonAdapter.getIndex());
				helper.setText(R.id.action_photo_tv1, item.getPackBarcode());
				helper.setText(R.id.action_photo_tv2, item.getPackNumber());
				helper.setText(R.id.action_photo_tv3, item.getOperationLink());
				helper.setText(R.id.action_photo_tv4, item.getPicture().size() + "");//图片数量
				helper.setText(R.id.action_photo_tv5, item.getScanUser());
				helper.setText(R.id.action_photo_tv6, item.getScanTime());
				
			}
		});

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {


			}
		});

		billCodeImg.setOnClickListener(this);
	}

	@Override
	public void initData() {
		photoList.add(IMG_ADD_TAG);
		
		//包装显示的是商品条码、商品号码
		if(MyApplication.m_scan_type.equals(Constant.SCAN_TYPE_OFFLINE) || MyApplication.m_scan_type.equals(Constant.SCAN_TYPE_INSTALL)) {
			tvBarcode.setText(Res.getString(R.string.goods_barcode));
			tvBarno.setText(Res.getString(R.string.goods_no));
			
			tvItemBarcode.setText(Res.getString(R.string.goods_barcode));
			tvItemBarno.setText(Res.getString(R.string.goods_no));
		}
		
		adapter = new GVAdapter();
		gridView.setAdapter(adapter);
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (photoList.get(position).equals(IMG_ADD_TAG)) {
					if (photoList.size() <= Constant.MAX_PHOTO_COUNT ) {
						new PopupWindows(mContext, photo_layout);
					} else
						Toast.makeText(ActionPhotoActivity.this, "最多只能选择" + Constant.MAX_PHOTO_COUNT + "张照片！", Toast.LENGTH_SHORT).show();
				}
			}
		});
		refreshAdapter();

		PostTools.getLink(mContext, linkList);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bill_code_img:
			// 扫描
			Intent openCameraIntent = new Intent(ActionPhotoActivity.this, CaptureActivity.class);
			startActivityForResult(openCameraIntent, Constant.CAPTURE_BILLCODE);
			break;

		default:
			break;
		}
	}

	@Override
	public void onEventMainThread(Message msg) {

		if(msg.what == Constant.SCAN_DATA){

			String strBillcode = (String) msg.obj;
			edtBillcode.setText(strBillcode);
			addData(null);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String path = "";
		if (requestCode == TAKE_FOLDER) {
			if (data == null) {
				return;
			}
			
			final Uri uri = data.getData();
			path = ImageTool.getImageAbsolutePath(this, uri);
			
			if (photoList.size() == Constant.MAX_PHOTO_COUNT + 1) {
				removeItem();
				refreshAdapter();
				return;
			}
			removeItem();
			photoList.add(path);
			photoList.add(IMG_ADD_TAG);
			refreshAdapter();

		} else if (requestCode == TAKE_PICTURE) {
			if (resultCode == RESULT_OK) {
				path = strFolder;
			} else if (resultCode == RESULT_CANCELED) {
				return;
			}
			
			if (photoList.size() == Constant.MAX_PHOTO_COUNT + 1) {
				removeItem();
				refreshAdapter();
				return;
			}
			removeItem();
			photoList.add(path);
			photoList.add(IMG_ADD_TAG);
			refreshAdapter();
		} else if (requestCode == Constant.CAPTURE_BILLCODE) {
			if (data == null) {
				return;
			}
			
			Bundle bundle = data.getExtras();
			String strBillcode = bundle.getString("result");

			edtBillcode.setText(strBillcode);
			addData(null);
			
//			Bundle bundle = data.getExtras();
//			String strBillcode = bundle.getString("result");
//			
//			ScanData scanData = DataUtilTools.checkScanData(strBillcode, dataList);
//			if (scanData != null) {
//
//				edtBillcode.setText(scanData.getPackBarcode());
//				edtNumber.setText(scanData.getPackNumber());
//				addData(null);
//			} else {
//				VoiceHint.playErrorSounds();
//				CommandTools.showToast(getResources().getString(R.string.barcode_does_not_exist));
//			}
			return;
		}
	}

	private void removeItem() {
		if (photoList.size() != Constant.MAX_PHOTO_COUNT + 1) {
			if (photoList.size() != 0) {
				photoList.remove(photoList.size() - 1);
			}
		}
	}
	
	private int link_num = -1;

	/**
	 * @param v
	 */
	public void chooseNode(View v) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < linkList.size(); i++) {
			list.add(linkList.get(i).getLinkName());
		}
		
		SingleItemDialog.showDialog(mContext, getResources().getString(R.string.mode_step), false, list, new SingleItemCallBack() {
			@Override
			public void callback(int pos) {
				link_num = linkList.get(pos).getLinkNum();
				edtLink.setText(linkList.get(pos).getLinkName());
			}
		});

	}

	/**
	 * 添加
	 * @param v
	 */

	public void addData(View v) {
		final String pack_barcode = edtBillcode.getText().toString(); 
		if(TextUtils.isEmpty(pack_barcode)){
			CommandTools.showToast(getResources().getString(R.string.code_not_null));
			return;
		}

		final String edtLinkValue = edtLink.getText().toString();
		if(TextUtils.isEmpty(edtLinkValue)){
			CommandTools.showToast(getResources().getString(R.string.select_mode_step));
			return;
		}

		if (photoList.size() <= 1) {
			CommandTools.showToast(getResources().getString(R.string.img_not_null));
			return;
		}

		for (int i = 0; i < dataList.size(); i++) {
			ScanData scanData = dataList.get(i);
			if (scanData.getPackBarcode().equals(pack_barcode)) {
				VoiceHint.playErrorSounds();
				CommandTools.showToast("不能重复录入");
				return;
			}
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("project_id", MyApplication.m_projectId);
			jsonObject.put("pack_barcode", pack_barcode);
			jsonObject.put("type", MyApplication.m_scan_type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		CustomProgress.showDialog(mContext, "数据获取中", true, null);
		String strUrl = "action/takephoto/checkbarcode";
		RequestUtil.postDataIfToken(mContext, strUrl, jsonObject, false, new RequestCallback() {

			@Override
			public void callback(int res, String remark, JSONObject jsonObject){
				if(res != 0){
					CommandTools.showToast(remark);
					return;
				}

				String goods_id = jsonObject.optJSONObject("data").optString("id");
				String pack_no = jsonObject.optJSONObject("data").optString("pack_no");

				ScanData scanData = new ScanData();
				scanData.setCacheId(CommandTools.getUUID());
				scanData.setPackBarcode(pack_barcode);
				scanData.setPackNumber(pack_no);
				scanData.setLink(MyApplication.m_link_num + "");
				scanData.setScanType(MyApplication.m_scan_type);
				scanData.setScanUser(MyApplication.m_userName);
				scanData.setScanTime(CommandTools.getTime());
				scanData.setOperationLink(edtLinkValue);
				scanData.setNode_id(MyApplication.m_nodeId);
				scanData.setMainGoodsId(goods_id);
				scanData.setScaned("1");
				scanData.setUploadStatus("0");
				
				List<String> list = new ArrayList<String>();
				for (int i = 0; i < photoList.size(); i++) {
					String str = photoList.get(i);

					if (!str.equals(IMG_ADD_TAG)) {
						list.add(str);
					}
				}
				scanData.setPicture(list);

				dataList.add(scanData);
				commonAdapter.notifyDataSetChanged();
				
				mScanDataDao.addData(scanData);
				mScanDataDao.addPicData(scanData);

				edtCount.setText(dataList.size() + "");

				edtBillcode.setText("");
				edtNumber.setText("");
				photoList.clear();
				photoList.add(IMG_ADD_TAG);
				refreshAdapter();
			}
		});

	}

	/**
	 * 拍照
	 * @param v
	 */
	public void takePhoto(View v){

		Intent intent = new Intent(this, PhotoMenuActivity.class);
		//		intent.putExtra("picName", dataList.get(0).getScanHawb());
		startActivity(intent);
	}

	public class PopupWindows extends PopupWindow {
		public PopupWindows(Context mContext, View parent) {
			super(mContext);

			View view = View.inflate(mContext, R.layout.item_popupwindows, null);
			LinearLayout ll_popup = (LinearLayout) view.findViewById(R.id.ll_popup);
			getBackground().setAlpha(80);
			setAnimationStyle(R.style.AnimationFade);
			setWidth(LayoutParams.MATCH_PARENT);
			setHeight(LayoutParams.MATCH_PARENT);
			setFocusable(true);
			setOutsideTouchable(false);
			setContentView(view);
			showAtLocation(parent, Gravity.BOTTOM, 0, 0);
			update();

			RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.layout_pop_window);
			Button bt1 = (Button) view.findViewById(R.id.item_popupwindows_camera);
			Button bt2 = (Button) view.findViewById(R.id.item_popupwindows_Photo);
			Button bt3 = (Button) view.findViewById(R.id.item_popupwindows_cancel);

			layout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					dismiss();
				}
			});

			bt1.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {

					getFromCamera();
					dismiss();
				}
			});

			bt2.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {

					getFromFolder();
					dismiss();
				}
			});

			bt3.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dismiss();
				}
			});

		}
	}
	
	String strFolder;

	public void getFromCamera() {
		if(FileUtils.existSDCard() == false){
			VoiceHint.playErrorSounds();
			CommandTools.showToast("没有SD卡");
			return;
		}

		if(Bimp.tempSelectBitmapList.size() >= Constant.MAX_PHOTO_COUNT){
			VoiceHint.playErrorSounds();
			CommandTools.showDialog(mContext, "最多拍" + Constant.MAX_PHOTO_COUNT + "张照片");
			return;
		}
		
		String fileName = CommandTools.getTimes();
		strFolder = Environment.getExternalStorageDirectory().getPath() + "/" +  fileName + ".JPEG";
		Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(strFolder)));
		startActivityForResult(openCameraIntent, TAKE_PICTURE);
	}

	public void getFromFolder() {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, TAKE_FOLDER);
	}

	/* 提交
	 * (non-Javadoc)
	 * @see com.xulan.client.BaseActivity#clickRight(android.view.View)
	 */
	public void clickRight(View v){
		List<ScanData> list = mScanDataDao.getNotUploadDataList2(MyApplication.m_scan_type, MyApplication.m_link_num + "", MyApplication.m_nodeId);
		if (list.size() <= 0) {
			CommandTools.showToast(getResources().getString(R.string.scan_records));
		} else {
			requestUpload(list);
		}
	}

	/**
	 * 上传数据
	 */
	protected LoadTextNetTask requestUpload(final List<ScanData> list) {

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
							mScanDataDao.updateUploadState(list);
							
							List<ScanData> list = mScanDataDao.getNotUploadDataList2(MyApplication.m_scan_type, MyApplication.m_link_num + "", MyApplication.m_nodeId);
							if (list.size() <= 0) {
								finish();
								commonAdapter.notifyDataSetChanged();
							} else {
								requestUpload(list);
							}
						}
					} catch (JSONException e) {
						Toast.makeText(ActionPhotoActivity.this, "解析错误", 1).show();
						e.printStackTrace();
					}
				} else {
				}
			}
		};

		CustomProgress.showDialog(ActionPhotoActivity.this, getResources().getString(R.string.searching), false, null);
		LoadTextNetTask task = UserService.takephoto(list, link_num, listener, null);
		return task;
	}

	private class GVAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return photoList.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				convertView = LayoutInflater.from(getApplication()).inflate(R.layout.activity_add_photo_gv_items, parent, false);
				holder = new ViewHolder();
				holder.imageView = (ImageView) convertView.findViewById(R.id.main_gridView_item_photo);
				holder.checkBox = (ImageView) convertView.findViewById(R.id.main_gridView_item_cb);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			String s = photoList.get(position);
			if (!s.equals(IMG_ADD_TAG)) {
				holder.checkBox.setVisibility(View.VISIBLE);
				holder.imageView.setImageBitmap(ImageTool.createImageThumbnail(s));
			} else {
				holder.checkBox.setVisibility(View.GONE);
				holder.imageView.setImageResource(R.drawable.phonet);
			}
			holder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					photoList.remove(position);
					refreshAdapter();
				}
			});
			return convertView;
		}

		private class ViewHolder {
			ImageView imageView;
			ImageView checkBox;
		}

	}

	private void refreshAdapter() {
		if (photoList == null) {
			photoList = new ArrayList<String>();
		}
		if (adapter == null) {
			adapter = new GVAdapter();
		}
		adapter.notifyDataSetChanged();
	}

}
