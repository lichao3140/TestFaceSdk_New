package com.testfacesdk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

// 引入人脸算法类
import com.idfacesdk.*;

public class MainActivity extends AppCompatActivity {

    private boolean bSdkInit = false; // SDK是否成功初始化
    private String strCacheDir = ""; // 本APP的cache目录
    private int VID1 = 0x096E, PID1 = 0x0304; // 白色USB加密狗
    private int VID2 = 0x3689, PID2 = 0x8762; // 红色或灰色加密狗

    // USB设备及权限类对象
    private UsbManager mUsbManager;
    protected PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION =
            "com.example.rockeyarm.USB_PERMISSION";

    private Button btn1, btn2, btn3, btn4, btn5;
    private TextView txt1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取本APP的cache目录
        Context cont = this.getApplicationContext();
        strCacheDir = cont.getCacheDir().getAbsolutePath();

        btn1 = (Button) findViewById(R.id.button1);
        btn2 = (Button) findViewById(R.id.button2);
        btn3 = (Button) findViewById(R.id.button3);
        btn4 = (Button) findViewById(R.id.button4);
        btn5 = (Button) findViewById(R.id.button5);
        txt1 = (TextView) findViewById(R.id.text1);

        Log.i("MainActivity","OnCreate OK.");

        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                ConnectUsbDog();
			}
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                StartSdk();
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                Compare11();
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                Compare1N();
            }
        });

        btn5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                LiveDetect();
            }
        });
    }

	protected void ConnectUsbDog()
	{
		String str;
		// 如果已经初台化，则先反初始化
        if(bSdkInit) {
            bSdkInit = false;
            IdFaceSdk.IdFaceSdkUninit();
        }
        // 查找加密狗并请求权限
        if (FindUsbDog() != true) {
			str = "请连接加密狗";
            txt1.setText(str);
            Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
        } else {
			str = "加密狗已连接";
            Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
        }
		
		Log.i("ConnectUsbDog", str);
	}
	
	protected void StartSdk()
	{
        String str;
		if(bSdkInit == false) {
            // 初始化人脸算法（反初始化后也必须先请求USB加密狗权限才能再次成功调用初始化）
            long tStart = System.nanoTime() / 1000000;
            int ret = IdFaceSdk.IdFaceSdkInit(strCacheDir);
            long tEnd = System.nanoTime() / 1000000;

            if(ret == 0) {
                bSdkInit = true;
                str = "SDK启动成功, 用时 " + (tEnd - tStart) + " 毫秒";
            } else if(ret == -1) str = "加密狗不工作";
            else str = "SDK启动失败";
            txt1.setText(str);
        } else str = "SDK已启动";
        Log.i("StartSDK", str);
        Toast.makeText(MainActivity.this, str, 3).show();
    }
	
	protected void Compare11()
	{
		String str;
		if(bSdkInit) {
            int nRoot = strCacheDir.indexOf("/cache");
			if(nRoot == -1) str = "Cache directory invalid.";
			else {
                String strImgRoot = strCacheDir.substring(0, nRoot) + "/lib/";

                int nFileNum = 3;
                String fileNames[] = new String[nFileNum];
                fileNames[0] = strImgRoot + "libTest_jpgfile_t1.so";
                fileNames[1] = strImgRoot + "libTest_jpgfile_u1.so";
                fileNames[2] = strImgRoot + "libTest_jpgfile_u2.so";

                int nBufSize = 1024 * 768 * 3;
                byte[] buffer = new byte[nBufSize];
                int[] nWidth = new int[1];
                int[] nHeight = new int[1];

                int i, j, ret;
                long tStart, tEnd;
                FACE_DETECT_RESULT faceResult = new FACE_DETECT_RESULT();
                byte[][] features = new byte[nFileNum][3000];

                String strResult = "";
                for(i = 0; i < nFileNum; i++) {
                    nWidth[0] = nHeight[0] = 0;
                    ret = IdFaceSdk.ReadImageFile(fileNames[i], buffer, nBufSize, nWidth, nHeight, 24);
                    if(ret < 0) {
                        strResult = "解码JPEG文件" + (i + 1) + " 返回" + ret;
                        break;
                    } else {
                        tStart = System.nanoTime() / 1000000;
                        ret = IdFaceSdk.IdFaceSdkDetectFace(buffer, nWidth[0], nHeight[0], faceResult);
                        tEnd = System.nanoTime() / 1000000;
                        if(ret <= 0) {
                            strResult = "JPEG文件" + (i + 1) +  " 检测人脸返回 " + ret;
                            break;
                        } else {
                            strResult += "JPEG文件" + (i + 1) + ": LeftEye(" + faceResult.nLeftEyeX + ", " + faceResult.nLeftEyeY + "), RightEye(" + faceResult.nRightEyeX + ", " + faceResult.nRightEyeY + "), 检测人脸用时 " + (tEnd - tStart) +  " 毫秒";

                            tStart = System.nanoTime() / 1000000;
                            ret = IdFaceSdk.IdFaceSdkFeatureGet(buffer, nWidth[0], nHeight[0], faceResult, features[i]);
                            tEnd = System.nanoTime() / 1000000;
                            if(ret != 0) {
                                strResult = "JPEG文件" + (i + 1) + " 提取特征返回错误 " + ret;
                                break;
                            } else strResult += "，提特征用时 " + (tEnd - tStart) + " 毫秒\n";
                        }
                    }
                }
                if(i == nFileNum) {
                    // 一对一比对
                    for(j = 1; j < nFileNum; j++) {
                        for (i = j; i < nFileNum; i++) {
                            tStart = System.nanoTime() / 1000000;
                            ret = IdFaceSdk.IdFaceSdkFeatureCompare(features[j - 1], features[i]);
                            tEnd = System.nanoTime() / 1000000;
                            strResult += "\n文件" + j + "与文件" + (i + 1) + " 比对分值: " + ret + ", 用时 " + (tEnd - tStart) + " 毫秒";
                        }
                    }
                }

                str = strResult;

                txt1.setText(str);
            }
        } else {
            str = "请先启动SDK";
            Toast.makeText(MainActivity.this, str, 1).show();
        }
        Log.i("Compare 1 to 1", str);
    }

    protected void Compare1N()
	{
		String str;
		if(bSdkInit) {
            int nRoot = strCacheDir.indexOf("/cache");
			if(nRoot == -1) str = "Cache directory invalid.";
			else {
                int i, j, nModNum = 8, nUserNum = 3, ret;
                long tStart, tEnd;
                String strImgRoot = strCacheDir.substring(0, nRoot) + "/lib/";
                String fileNameMods[] = new String[nModNum];
                String fileNameUsers[] = new String[nUserNum];
                for(i = 0; i < nModNum; i++) fileNameMods[i] = strImgRoot + "libTest_jpgfile_t" + (i + 1) + ".so";
                for(i = 0; i < nUserNum; i++) fileNameUsers[i] = strImgRoot + "libTest_jpgfile_u" + (i + 1) + ".so";

                String strResult = "";
                int hList  = IdFaceSdk.IdFaceSdkListCreate(nModNum);
                if(hList == 0) strResult = "创建链表失败";
                else {
                    int nBufSize = 1024 * 768 * 3;
                    byte[] buffer = new byte[nBufSize];
                    int[] nWidth = new int[1];
                    int[] nHeight = new int[1];

                    FACE_DETECT_RESULT faceResult = new FACE_DETECT_RESULT();
                    byte[] feature = new byte[3000];
                    byte[] nScores = new byte[nModNum];
                    int[] nPos = new int[1];

                    for(i = 0; i < nModNum; i++) {
                        nWidth[0] = nHeight[0] = 0;
                        ret = IdFaceSdk.ReadImageFile(fileNameMods[i], buffer, nBufSize, nWidth, nHeight, 24);
                        if(ret < 0) {
                            strResult = "解码库文件" + (i + 1) + " 返回" + ret;
                            break;
                        } else {
                            ret = IdFaceSdk.IdFaceSdkDetectFace(buffer, nWidth[0], nHeight[0], faceResult);
                            if(ret <= 0) {
                                strResult = "库文件" + (i + 1) +  " 检测人脸返回 " + ret;
                                break;
                            } else {
                                ret = IdFaceSdk.IdFaceSdkFeatureGet(buffer, nWidth[0], nHeight[0], faceResult, feature);
                                if(ret != 0) {
                                    strResult = "库文件" + (i + 1) + " 提取特征返回错误 " + ret;
                                    break;
                                }

                                nPos[0] = -1;
                                ret = IdFaceSdk.IdFaceSdkListInsert(hList, nPos, feature, 1);
                                if(ret != (i + 1)) {
                                    strResult = "特征" + (i + 1) + "入库失败";
                                    break;
                                }
                            }
                        }
                    }
                    if(i == nModNum) { // 入库成功，开始比对
                        for(i = 0; i < nUserNum; i++) {
                            nWidth[0] = nHeight[0] = 0;
                            ret = IdFaceSdk.ReadImageFile(fileNameUsers[i], buffer, nBufSize, nWidth, nHeight, 24);
                            if(ret < 0) {
                                strResult = "解码用户文件" + (i + 1) + " 返回" + ret;
                                break;
                            } else {
                                ret = IdFaceSdk.IdFaceSdkDetectFace(buffer, nWidth[0], nHeight[0], faceResult);
                                if(ret <= 0) {
                                    strResult = "用户文件" + (i + 1) +  " 检测人脸返回 " + ret;
                                    break;
                                } else {
                                    ret = IdFaceSdk.IdFaceSdkFeatureGet(buffer, nWidth[0], nHeight[0], faceResult, feature);
                                    if(ret != 0) {
                                        strResult = "用户文件" + (i + 1) + " 提取特征返回错误 " + ret;
                                        break;
                                    }

                                    for(j = 0; j < nModNum; j++) nScores[j] = 0;
                                    tStart = System.nanoTime() / 1000000;
                                    ret = IdFaceSdk.IdFaceSdkListCompare(hList, feature, 0, -1, nScores);
                                    tEnd = System.nanoTime() / 1000000;
                                    if(ret != nModNum) {
                                        strResult = "用户" + (i + 1) + " 比对失败，返回 " + ret;
                                        break;
                                    } else {
                                        int nMaxScore = -1, nMatchIndex = -1;
                                        strResult += "用户" + (i + 1) + "分数列表:";
                                        for(j = 0; j < nModNum; j++) {
                                            if(nScores[j] > nMaxScore) {
                                                nMaxScore = nScores[j];
                                                nMatchIndex = j;
                                            }
                                            if(j < 10) strResult += " " + nScores[j];
                                            else if(j == 10) strResult += " ...";
                                        }
                                        strResult += "\n比中库中第" + (nMatchIndex + 1) + "人, 分数" + nMaxScore + ", 用时 " + (tEnd - tStart) + " 毫秒\n\n";
                                    }
                                }
                            }
                        }
                    }
                    IdFaceSdk.IdFaceSdkListDestroy(hList);
                }

                str = strResult;

                txt1.setText(str);
            }
        } else {
            str = "请先启动SDK";
            Toast.makeText(MainActivity.this, str, 1).show();
        }
        Log.i("Compare 1 to N", str);
    }
	
	protected void LiveDetect()
    {
        String str;
        if(bSdkInit) {
            int nRoot = strCacheDir.indexOf("/cache");
            if(nRoot == -1) str = "Cache directory invalid.";
            else {
                String strImgRoot = strCacheDir.substring(0, nRoot) + "/lib/";

                String[] fileNames = new String[2];
                fileNames[0] = strImgRoot + "libTest_jpgfile_Color.so";
                fileNames[1] = strImgRoot + "libTest_jpgfile_BW.so";

                int nBufSize = 1024 * 768 * 3;
                byte[][] rgbBuffers = new byte[2][nBufSize];
                int[] nWidth = new int[1];
                int[] nHeight = new int[1];

                int i, ret;
                long tStart, tEnd;
                FACE_DETECT_RESULT[] faceResults = new FACE_DETECT_RESULT[2];
                faceResults[0] = new FACE_DETECT_RESULT();
                faceResults[1] = new FACE_DETECT_RESULT();

                String strResult = "";
                for(i = 0; i < 2; i++) {
                    nWidth[0] = nHeight[0] = 0;
                    ret = IdFaceSdk.ReadImageFile(fileNames[i], rgbBuffers[i], nBufSize, nWidth, nHeight, 24);
                    if (ret < 0) {
                        strResult = "解码JPEG文件" + (i + 1) + " 返回" + ret;
                        break;
                    }
                }
                if(i == 2) {
                    // 直接传图象快速活体检测
                    tStart = System.nanoTime() / 1000000;
                    ret =IdFaceSdk.IdFaceSdkQuickLiveDetectImg(nWidth[0], nHeight[0], rgbBuffers[1]);
                    tEnd = System.nanoTime() / 1000000;
                    if(ret == 1) strResult += "图象快速检测确认为活体 !";
                    else if(ret == 0) strResult += "图象快速检测为非活体 !!!";
                    else strResult += "图象快速活体检测失败，返回错误 " + ret + ".";
                    strResult += " 用时 " + (tEnd - tStart) + " 毫秒\n";

                    for(i = 0; i < 2; i++) {
                        tStart = System.nanoTime() / 1000000;
                        ret = IdFaceSdk.IdFaceSdkDetectFace(rgbBuffers[i], nWidth[0], nHeight[0], faceResults[i]);
                        tEnd = System.nanoTime() / 1000000;
                        if (ret <= 0) {
                            strResult += "JPEG文件" + (i + 1) + " 检测人脸返回 " + ret;
                            break;
                        } else strResult += "JPEG文件" + (i + 1) + " 检测人脸成功，用时 " + (tEnd - tStart) + " 毫秒\n";
                    }
                }
                if(i == 2) {
                    // 第三方检测到的人眼坐标快速活体检测
                    tStart = System.nanoTime() / 1000000;
                    ret =IdFaceSdk.IdFaceSdkQuickLiveDetectEyes(nWidth[0], nHeight[0], rgbBuffers[1], faceResults[1].nLeftEyeX, faceResults[1].nLeftEyeY, faceResults[1].nRightEyeX, faceResults[1].nRightEyeY);
                    tEnd = System.nanoTime() / 1000000;
                    if(ret == 1) strResult += "\n第三方人脸坐标快速检测确认为活体 !";
                    else if(ret == 0) strResult += "\n第三方人脸坐标快速检测为非活体 !";
                    else strResult += "\n第三方人脸坐标快速活体检测失败，返回错误 " + ret + ".";
                    strResult += " 用时 " + (tEnd - tStart) + " 毫秒";

                    // 快速活体检测
                    tStart = System.nanoTime() / 1000000;
                    ret =IdFaceSdk.IdFaceSdkQuickLiveDetect(nWidth[0], nHeight[0], rgbBuffers[1], faceResults[1]);
                    tEnd = System.nanoTime() / 1000000;
                    if(ret == 1) strResult += "\n快速检测确认为活体 !";
                    else if(ret == 0) strResult += "\n快速检测为非活体 !";
                    else strResult += "\n快速活体检测失败，返回错误 " + ret + ".";
                    strResult += " 用时 " + (tEnd - tStart) + " 毫秒";

                    // 标准活体检测
                    tStart = System.nanoTime() / 1000000;
                    ret =IdFaceSdk.IdFaceSdkLiveDetect(nWidth[0], nHeight[0], rgbBuffers[0], faceResults[0], rgbBuffers[1], faceResults[1]);
                    tEnd = System.nanoTime() / 1000000;
                    if(ret == 1) strResult += "\n标准检测确认为活体 !";
                    else if(ret == 0) strResult += "\n标准检测为非活体 !";
                    else strResult += "\n标准活体检测失败，返回错误 " + ret + ".";
                    strResult += " 用时 " + (tEnd - tStart) + " 毫秒";
                } else strResult += "\n非活体 !";

                str = strResult;

                txt1.setText(str);
            }
        } else {
            str = "请先启动SDK";
            Toast.makeText(MainActivity.this, str, 1).show();
        }
        Log.i("LiveDetect", str);
    }

    // 查找USB加密狗并请求访问权限
    public boolean FindUsbDog(){
        // TODO Auto-generated method stub
        mUsbManager = (UsbManager) getSystemService( Context.USB_SERVICE );
        if(mUsbManager==null) {
            Toast.makeText(MainActivity.this, "创建 UsbManager 失败 !", Toast.LENGTH_SHORT).show();
            return false;
        }

        HashMap< String, UsbDevice > stringDeviceMap = mUsbManager.getDeviceList();
        if(stringDeviceMap.isEmpty()){
            Toast.makeText(MainActivity.this, "请连接加密狗 !", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Collection< UsbDevice > usbDevices = stringDeviceMap.values();

            mPermissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent( ACTION_USB_PERMISSION ), 0 );
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver( mUsbReceiver, filter );

            Iterator< UsbDevice > usbDeviceIter = usbDevices.iterator();
            while( usbDeviceIter.hasNext() ) {
                UsbDevice usbDevice = usbDeviceIter.next();
                if((usbDevice.getVendorId() == VID1 && usbDevice.getProductId() == PID1) || (usbDevice.getVendorId() == VID2 && usbDevice.getProductId() == PID2)) {
                    // Request permission to access the device.
                    mUsbManager.requestPermission( usbDevice, mPermissionIntent );
                    return true;
                }
            }
            return false;
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)arg1.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (arg1.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null) {
                            UsbDeviceConnection connection = mUsbManager.openDevice( device );
                            int fd = connection.getFileDescriptor(), nAuthType = 1;
                            if(device.getVendorId() == VID2 && device.getProductId() == PID2) nAuthType = 0;

                            // 设置加密方式及加密狗句柄
                            IdFaceSdk.IdFaceSdkSetAuth(nAuthType, fd);

                            txt1.setText("加密狗连接成功");
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "未授权给设备 " + device, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };
}
