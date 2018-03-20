﻿package com.xulan.client.net;


/**
 * @author HeXiuHui
 *
 */
public class LoadPicParams extends NetTaskParams
{
	public boolean m_bLoadBitmap = false;
	public boolean m_bWriteToFile = true;
	public String m_strFileName;

	@Override
	public String getTargetFileName()
	{
		return NetUtil.getFileNameFromUrl(m_strURL);
	}
}
