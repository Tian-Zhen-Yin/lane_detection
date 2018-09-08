package com.example.cameratest3;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.example.cameratest3.R;

public class MainActivity extends ActionBarActivity implements
		CvCameraViewListener2 {
	private static final String TAG = "CameraTest3::MainActivity";
	private CameraBridgeViewBase mCVCamera; // openCV������ӿ�
	private Mat mRgba, mTmp; // Mat����������Ϊʵʱ֡ͼ��Ļ������,�������ÿ֡���������
	// private LaneDetectionView mView;
	private LaneDetection lanedt;

	// ͨ��OpenCV����Android�����첽��ʼ��OpenCV

	BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV loaded successfully");

				// Load native library after(!) OpenCV initialization
				System.loadLibrary("opencv_java3");
				System.loadLibrary("OpenCV_Test");

				mCVCamera.enableView();
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// ʵ�ְ󶨺�����¼�����
		mCVCamera = (CameraBridgeViewBase) findViewById(R.id.main_activity_surface_view);
		mCVCamera.setCvCameraViewListener(this);
	}

	@Override
	protected void onResume() {
		// ÿ�ε�ǰActivity�������ô˷��������Կ����ڴ˴����OpenCV�Ŀ��ļ��Ƿ�������
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			Log.d(TAG, "OpenCV library not found!");
		} else {
			Log.d(TAG, "OpenCV library found inside package. Using it!");
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

	@Override
	protected void onDestroy() {
		if (mCVCamera != null) {
			mCVCamera.disableView();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// ����ʵ�����Լ��������Ե����ã����������ȡ���Ⱥ�ͼ�����ͱ�־

	@Override
	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mTmp = new Mat(height, width, CvType.CV_8UC4);

	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
		mTmp.release();

	}

	// ����ͼ����

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// by JNI 4.25 (update 5.6 : native build an ROI)

		mRgba = inputFrame.rgba();
		lanedt = new LaneDetection();
		lanedt.colorFrameLaneLine(mRgba);
		lanedt.setLaneSlope(lanedt.getLeftLaneSlope(), lanedt.getRightLaneSlope());

		return mRgba;
		// ------------------------------------------------
		// by java 4.17
		// mRgba = inputFrame.rgba();
		// Mat linesHoughP = new Mat();
		// Lane lane = new Lane();
		// mTmp = mRgba.clone();
		// Mat blur = new Mat();
		//
		// Imgproc.GaussianBlur(inputFrame.gray(), blur, new Size(17, 17), 0);
		// Imgproc.Canny(blur, mTmp, 50, 80);
		//
		// //set region of interest
		// int r = mTmp.rows();
		// Range roiRowRange = new Range(r / 2, r);
		// Mat imgRoi = new Mat(mTmp, roiRowRange);
		//
		// Imgproc.HoughLinesP(imgRoi, linesHoughP, 2, Math.PI / 180, 1, 50, 5);
		//
		// // Drawing lines on the image
		// for (int i = 0; i < linesHoughP.rows(); i++) {
		// double[] points = linesHoughP.get(i, 0);
		// lane.x1 = points[0];
		// lane.y1 = points[1] + r / 2;
		// lane.x2 = points[2];
		// lane.y2 = points[3] + r / 2;
		// if (lane.slope >= 0.55 || lane.slope <= 1.73) {
		// lane.draw(mRgba, new Scalar(0, 0, 255), 3);
		// }
		// }
		// ���ر�ע�����ߵ�ͼ��
		// return mRgba;
	}
}
