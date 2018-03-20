package com.xulan.client.service;

import java.util.ArrayList;

import org.apache.http.NameValuePair;

import com.xulan.client.net.AsyncNetTask;
import com.xulan.client.net.AsyncNetTask.OnPostExecuteListener;
import com.xulan.client.net.AsyncTaskManager;
import com.xulan.client.net.HttpSendType;
import com.xulan.client.net.LoadPicNetTask;
import com.xulan.client.net.LoadPicParams;
import com.xulan.client.net.LoadTextNetTask;
import com.xulan.client.net.LoadTextParams;
import com.xulan.client.net.NetTaskParamsMaker;
import com.xulan.client.util.Logs;

/**
 * 
 * @author HeXiuHui
 * 
 */
public class CommonService {

	// 从图片服务器获取图片
	public static LoadPicNetTask getPicture(OnPostExecuteListener listener, Object tag, String strPicUrl) {
		LoadPicParams params = NetTaskParamsMaker.makeLoadPictureParams(strPicUrl);

		LoadPicNetTask task = (LoadPicNetTask) AsyncTaskManager.createAsyncTask(AsyncNetTask.TaskType.GET_PIC, params);
		task.addOnPostExecuteListener(listener, tag);
		task.executeMe();

		return task;
	}
	
	/** 获取服务器端APK版本号 **/
	public static LoadTextNetTask getAppVersion(OnPostExecuteListener listener, String strUrl, Object tag) {
		final ArrayList<NameValuePair> listArg = new ArrayList<NameValuePair>();
		
		Logs.v("checkupdate", "APP更新检查: " + strUrl);
		LoadTextParams params = NetTaskParamsMaker.makeLoadTextParams(strUrl,
				listArg, HttpSendType.HTTP_GET);
		LoadTextNetTask task = (LoadTextNetTask) AsyncTaskManager
				.createAsyncTask(AsyncNetTask.TaskType.GET_TEXT, params);

		task.addOnPostExecuteListener(listener, tag);
		task.executeMe();
		
		return task;
	}
	
}
