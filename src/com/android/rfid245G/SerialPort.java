/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.android.rfid245G;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class SerialPort {

	private static final String TAG = "SerialPort";

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;
	private boolean trig_on=false;
	
	public SerialPort(int port, int baudrate, int flags) throws SecurityException, IOException {
	
		mFd = open(port, baudrate);
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);


	}

	// Getters and setters
	public InputStream getInputStream() {
		return mFileInputStream;
	}

	public OutputStream getOutputStream() {
		return mFileOutputStream;
	}
	public void printer_poweron() {

	}
	public void printer_poweroff() {

	}
	public void psam_poweron() {
		psamPowerOn();
	}
	public void psam_poweroff() {
		psamPoweroff();
		//scaner_trigoff();
	}
	public void rfid_poweron() {
		rfidPoweron();
	}
	public void rfid_poweroff() {
		rfidPoweroff();
		//scaner_trigoff();
	}
	
	// JNI
	private native static FileDescriptor open(int port, int baudrate);
	public native void close(int port);
	public native void rfidPoweron();
	public native void rfidPoweroff();
	public native void psamPowerOn();
	public native void psamPoweroff();
	static {
		System.loadLibrary("devapi");
		System.loadLibrary("SerialPort");
	}
	
}
