/**************************************************************************************************
  Filename:       DeviceView.java
  Revised:        $Date: 2013-08-30 12:02:37 +0200 (fr, 30 aug 2013) $
  Revision:       $Revision: 27470 $

  Copyright 2013 Texas Instruments Incorporated. All rights reserved.
 
  IMPORTANT: Your use of this Software is limited to those specific rights
  granted under the terms of a software license agreement between the user
  who downloaded the software, his/her employer (which must be your employer)
  and Texas Instruments Incorporated (the "License").  You may not use this
  Software unless you agree to abide by the terms of the License. 
  The License limits your use, and you acknowledge, that the Software may not be 
  modified, copied or distributed unless used solely and exclusively in conjunction 
  with a Texas Instruments Bluetooth device. Other than for the foregoing purpose, 
  you may not use, reproduce, copy, prepare derivative works of, modify, distribute, 
  perform, display or sell this Software and/or its documentation for any purpose.
 
  YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
  PROVIDED “AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
  INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
  NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
  TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
  NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
  LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
  INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
  OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
  OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
  (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 
  Should you have any questions regarding your right to use this Software,
  contact Texas Instruments Incorporated at www.TI.com

 **************************************************************************************************/
package ti.android.ble.sensortag;

import static ti.android.ble.sensortag.SensorTag.*;
import static ti.android.ble.sensortag.R.drawable.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ti.android.util.Point3D;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

// Fragment for Device View
public class DeviceView extends Fragment {
	
	private static final String TAG = "DeviceView";
	private Point3D prevV;
	
  // Sensor table; the iD corresponds to row number
	private static final int ID_OFFSET = 0;
  private static final int ID_KEY = 0;
  private static final int ID_ACC = 1;
  private static final int ID_MAG = 2;
  private static final int ID_GYR = 3;
  private static final int ID_ORI = 4;
  private static final int ID_OBJ = 5;
  private static final int ID_AMB = 6;
  private static final int ID_HUM = 7;
  private static final int ID_BAR = 8;
  private static final int ID_SON = 9;
  

	public static DeviceView mInstance = null;

	// GUI
  private TableLayout table;
	private TextView mAccValue;
	private TextView mMagValue;
	private TextView mGyrValue;
	private TextView mObjValue;
	private TextView mAmbValue;
	private TextView mHumValue;
	private TextView mBarValue;
	private TextView mOriValue;
	private TextView mSonValue;
	private ImageView mButton;
	private TextView mStatus;
	private TableRow mMagPanel;
	private TableRow mOriPanel;
	private TableRow mBarPanel;
	private ScrollView sc;
	private File file;
  // House-keeping
  private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");
  private DeviceActivity mActivity;
  private static final double PA_PER_METER = 12.0;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.i(TAG, "onCreateView");
    mInstance = this;
    mActivity = (DeviceActivity) getActivity();
    
    // The last two arguments ensure LayoutParams are inflated properly.
    View view = inflater.inflate(R.layout.services_browser, container, false);
    sc = (ScrollView) view.findViewById(R.id.scrollview);
    // Hide all Sensors initially (but show the last line for status)
    table = (TableLayout) view.findViewById(R.id.services_browser_layout);

    // UI widgets
    mAccValue = (TextView) view.findViewById(R.id.accelerometerTxt);
    mMagValue = (TextView) view.findViewById(R.id.magnetometerTxt);
  	mGyrValue = (TextView) view.findViewById(R.id.gyroscopeTxt);
  	mObjValue = (TextView) view.findViewById(R.id.objTemperatureText);
  	mAmbValue = (TextView) view.findViewById(R.id.ambientTemperatureTxt);
  	mHumValue = (TextView) view.findViewById(R.id.humidityTxt);
  	mBarValue = (TextView) view.findViewById(R.id.barometerTxt);
  	mOriValue = (TextView) view.findViewById(R.id.orientationTxt);
  	mSonValue = (TextView) view.findViewById(R.id.sonarTxt);
  	mButton = (ImageView) view.findViewById(R.id.buttons);
  	mStatus = (TextView) view.findViewById(R.id.status);
  	
	File root = android.os.Environment.getExternalStorageDirectory(); 
    File dir = new File (root.getAbsolutePath() + "/asd");
    dir.mkdirs();
    DateFormat s = SimpleDateFormat.getDateTimeInstance();
    file = new File(dir,  s.format(new Date()) +".csv");	
  	
  	
  	// Support for calibration
  	mMagPanel = (TableRow) view.findViewById(R.id.magPanel);
  	mBarPanel = (TableRow) view.findViewById(R.id.barPanel);
  	mOriPanel = (TableRow) view.findViewById(R.id.oriPanel);
  	OnClickListener cl = new OnClickListener() {
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.magPanel:
					mActivity.calibrateMagnetometer();
					break;
				case R.id.barPanel:
					mActivity.calibrateHeight();
					break;
				case R.id.oriPanel:
					mActivity.calibrateOrientation();
					break;
				default:
				}
			}
		};
		
		mMagPanel.setOnClickListener(cl);
		mBarPanel.setOnClickListener(cl);
		mOriPanel.setOnClickListener(cl);
  	
    // Notify activity that UI has been inflated
    mActivity.onViewInflated(view);

    return view;
  }


  @Override
  public void onResume() {
    super.onResume();
    updateVisibility();
  }


	@Override
  public void onPause() {
    super.onPause();
  }

  /**
   * Handle changes in sensor values
   * */
  public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
		Point3D v;
		String msg;


  	if (uuidStr.equals(UUID_ACC_DATA.toString())) {

  		v = Sensor.ACCELEROMETER.convert(rawValue);
  		if(prevV == null)
  			prevV = v;
  		double deger = (prevV.y - v.y);
  		if( deger > 1.25)
  			sc.setBackgroundColor(Color.RED);
  		else if(deger >0.75)
  			sc.setBackgroundColor(Color.GREEN);
  		else if(deger >0.25)
  			sc.setBackgroundColor(Color.YELLOW);
  		else if (deger >-0.25)
  			sc.setBackgroundColor(Color.WHITE);
  		else if (deger >-0.75)
  			sc.setBackgroundColor(Color.DKGRAY);
  		else if (deger > -1.25)
  			sc.setBackgroundColor(Color.BLUE);
  		else 
  			sc.setBackgroundColor(Color.BLACK);
  		prevV = v;	
  		msg = decimal.format(v.x) + "\n" + decimal.format(v.y) + "\n" + decimal.format(v.z) + "\n";
  		writeToSDFile(uuidStr + "," + Long.toString(System.currentTimeMillis())+ ","+ Double.toString(v.x) + ","+ Double.toString(v.y) + ","+ Double.toString(v.z));
  		mAccValue.setText(msg);
  	} 
  
  	if (uuidStr.equals(UUID_MAG_DATA.toString())) {
  		v = Sensor.MAGNETOMETER.convert(rawValue);
  		msg = decimal.format(v.x) + "\n" + decimal.format(v.y) + "\n" + decimal.format(v.z) + "\n";
  		writeToSDFile(uuidStr + "," + Long.toString(System.currentTimeMillis())+ ","+ Double.toString(v.x) + ","+ Double.toString(v.y) + ","+ Double.toString(v.z));
  		mMagValue.setText(msg);
  	} 

  	if (uuidStr.equals(UUID_GYR_DATA.toString())) {
  		v = Sensor.GYROSCOPE.convert(rawValue);
  		msg = decimal.format(v.x) + "\n" + decimal.format(v.y) + "\n" + decimal.format(v.z) + "\n";
  		writeToSDFile(uuidStr + "," + Long.toString(System.currentTimeMillis())+ ","+ Double.toString(v.x) + ","+ Double.toString(v.y) + ","+ Double.toString(v.z));
  		mGyrValue.setText(msg);
  	} 
  	
  	if (uuidStr.equals(UUID_ORI_DATA.toString())) {
  		v = Sensor.ORIENTATION.convert(rawValue);
  		msg = decimal.format(v.x) + "\n" + decimal.format(v.y) + "\n" + decimal.format(v.z) + "\n";
  		writeToSDFile(uuidStr + "," + Long.toString(System.currentTimeMillis())+ ","+ Double.toString(v.x) + ","+ Double.toString(v.y) + ","+ Double.toString(v.z));
  		mOriValue.setText(msg);
  	} 
  	if (uuidStr.equals(UUID_SON_DATA.toString())) {
  		v = Sensor.SONAR.convert(rawValue);
  		if(v.x < 766)
  		{
	  		msg = decimal.format(v.x) + "\n" ;
	  		writeToSDFile(uuidStr + "," + Long.toString(System.currentTimeMillis())+ ","+ Double.toString(v.x) + ","+ Double.toString(v.y) + ","+ Double.toString(v.z));
	  		mSonValue.setText(msg);
  		}
  	} 

  	if (uuidStr.equals(UUID_IRT_DATA.toString())) {
  		v = Sensor.IR_TEMPERATURE.convert(rawValue);
  		msg = decimal.format(v.x) + "\n";
  		mAmbValue.setText(msg);
  		msg = decimal.format(v.y) + "\n";
  		mObjValue.setText(msg);
  	}
  	
  	if (uuidStr.equals(UUID_HUM_DATA.toString())) {
  		v = Sensor.HUMIDITY.convert(rawValue);
  		msg = decimal.format(v.x) + "\n";
  		mHumValue.setText(msg);
  	}

  	if (uuidStr.equals(UUID_BAR_DATA.toString())) {
  		v = Sensor.BAROMETER.convert(rawValue);
  		double h = (v.x - BarometerCalibrationCoefficients.INSTANCE.heightCalibration) / PA_PER_METER;
  		h = (double)Math.round(-h * 10.0) / 10.0;
  		msg = decimal.format(v.x/100) + "\n" + h;
  		mBarValue.setText(msg);
  	}

  	if (uuidStr.equals(UUID_KEY_DATA.toString())) {
  		SimpleKeysStatus s;
  		final int img;
  		s = Sensor.SIMPLE_KEYS.convertKeys(rawValue);
  		
  		switch (s) {
  		case OFF_OFF:
  			img = buttonsoffoff;
  			break;
  		case OFF_ON:
  			img = buttonsoffon;
  			break;
  		case ON_OFF:
  			img = buttonsonoff;
  			break;
  		case ON_ON:
  			img = buttonsonon;
  			break;
  		default:
  			throw new UnsupportedOperationException();
  		}

  		mButton.setImageResource(img);
  	}
  }
  
  void updateVisibility() {
  	showItem(ID_KEY,mActivity.isEnabledByPrefs(Sensor.SIMPLE_KEYS));
  	showItem(ID_ACC,mActivity.isEnabledByPrefs(Sensor.ACCELEROMETER));
  	showItem(ID_MAG,mActivity.isEnabledByPrefs(Sensor.MAGNETOMETER));
  	showItem(ID_GYR,mActivity.isEnabledByPrefs(Sensor.GYROSCOPE));
  	showItem(ID_ORI,mActivity.isEnabledByPrefs(Sensor.ORIENTATION));
  	showItem(ID_SON,mActivity.isEnabledByPrefs(Sensor.SONAR));
  	showItem(ID_OBJ,mActivity.isEnabledByPrefs(Sensor.IR_TEMPERATURE));
  	showItem(ID_AMB,mActivity.isEnabledByPrefs(Sensor.IR_TEMPERATURE));
  	showItem(ID_HUM,mActivity.isEnabledByPrefs(Sensor.HUMIDITY));
  	showItem(ID_BAR,mActivity.isEnabledByPrefs(Sensor.BAROMETER));
  }


  private void showItem(int id, boolean visible) {
  	View hdr = table.getChildAt(id*2 + ID_OFFSET);
  	View txt = table.getChildAt(id*2 + ID_OFFSET + 1);
  	int vc = visible ? View.VISIBLE : View.GONE;
  	hdr.setVisibility(vc);    
  	txt.setVisibility(vc);    
  }

  void setStatus(String txt) {
  	mStatus.setText(txt);
  	mStatus.setTextAppearance(mActivity, R.style.statusStyle_Success);
  }

  void setError(String txt) {
  	mStatus.setText(txt);
  	mStatus.setTextAppearance(mActivity, R.style.statusStyle_Failure);
  }

  void setBusy(boolean f) {
  	if (f)
  		mStatus.setTextAppearance(mActivity, R.style.statusStyle_Busy);
  	else
  		mStatus.setTextAppearance(mActivity, R.style.statusStyle);  		
  }
  private void writeToSDFile(String s){




	    try {
	        FileOutputStream f = new FileOutputStream(file,true);
	        PrintWriter pw = new PrintWriter(f,true);
	        pw.println(s);
	        pw.close();
	        f.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	        Log.i(TAG, "******* File not found. Did you" +
	                " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }   

	}
}
