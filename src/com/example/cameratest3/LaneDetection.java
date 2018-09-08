package com.example.cameratest3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class LaneDetection {

	private static final String TAG = "CameraTest3::LaneDetection";

	private ArrayList<Lane> detectedLinesList;
	private ArrayList<Lane> candidateLinesList;
	private ArrayList<Lane> posLines;
	private ArrayList<Lane> negLines;
	private Lane leftLane;
	private Lane rightLane;
	private LaneDetectionView mView;// ���ڵ���JNI

	static private double oldLeftLaneSlope = 0;// slope of leftlane in last
												// frame
	static private double oldRightLaneSlope = 0;// slope of rightlane in last
												// frame
	static private boolean flag = true;// �ж��Ƿ��ǵ�һ֡

	// Booleans storing if right or left lane detected
	// private Boolean rightLaneDetected;
	// private Boolean leftLaneDetected;

	public LaneDetection() {
		mView = new LaneDetectionView();// ��ʼ��LaneDetectionView�Ķ���
		posLines = new ArrayList<Lane>();
		negLines = new ArrayList<Lane>();
		leftLane = new Lane();
		rightLane = new Lane();

		detectedLinesList = new ArrayList<Lane>();
		candidateLinesList = new ArrayList<Lane>();

		// rightLaneDetected = false;
		// leftLaneDetected = false;

	}

	/**
	 * ������Ƴ���λ�õ�������
	 * 
	 * @param lineCandidates
	 *            ArrayList<Lane> ����hough�任����
	 * @param imageRows
	 *            int height of image which hough transform was applied
	 */

	public void computeLaneFromCandidates(int imageRows) {

		Log.i(TAG, "LaneDetection method : computeLaneFromCandidates enter");
		// separate candidate lines according to their slope
		// posLines = new ArrayList<Lane>();
		// negLines = new ArrayList<Lane>();
		//
		// leftLane = new Lane();
		// rightLane = new Lane();
		double negBias = 0;
		double negSlope = 0;
		double posBias = 0;
		double posSlope = 0;

		Log.i(TAG, "classify lines start");
		Iterator<Lane> it1 = candidateLinesList.iterator();
		while (it1.hasNext()) {
			// Log.i(TAG, "candidateLinesList lines:" + it1.next());
			Lane candidateLane = it1.next();
			if (candidateLane.computeSlope() > 0) {
				posLines.add(candidateLane);
				Log.i(TAG, "add line to posLines:" + candidateLane.toString());
			} else if (candidateLane.computeSlope() < 0) {
				negLines.add(candidateLane);
				Log.i(TAG, "add line to negLines:" + candidateLane.toString());
			}
		}
		Log.i(TAG, "classify lines stop");

		try {
			// Log.i(TAG, "compute leftLanes' median start");
			// �����������೵�����ߵķ��� ƫ���б����λ�������������쳣ֵ
			negBias = computeMedian(negLines, "bias");
			negSlope = computeMedian(negLines, "slope");
			leftLane.x1 = 0;
			leftLane.y1 = negBias;
			leftLane.x2 = -Math.round(negBias / negSlope);
			leftLane.y2 = 0;
			// Log.i(TAG, "compute leftLanes' median stop: leftLane.bias:"
			// + negBias + " leftlane.slope:" + negSlope);

			// Log.i(TAG, "compute rightLines' median start");
			// ����������Ҳ೵�����ߵķ��� ƫ���б����λ�������������쳣ֵ
			posBias = computeMedian(posLines, "bias");
			posSlope = computeMedian(posLines, "slope");
			rightLane.x1 = 0;
			rightLane.y1 = posBias;
			rightLane.x2 = Math.round((imageRows - posBias) / posSlope);
			rightLane.y2 = imageRows;
			// Log.i(TAG, "compute rightLines' median stop: rightLane.bias:"
			// + posBias + " rightLane.slope:" + posSlope);

		} catch (Exception e) {
			// TODO: handle exception
			Log.i(TAG,
					"Unknown exception in LaneDetection method : computeLaneFromCandidates : "
							+ e.getLocalizedMessage());
		}

		Log.i(TAG, "LaneDetection method : computeLaneFromCandidates exit");
	}

	/**
	 * ����ƫ���б�ʵ���λ��
	 * 
	 * @param tmpLines
	 *            ArrayList
	 * @param str
	 *            String �� "bias" or "slope"
	 * @return median
	 */
	public double computeMedian(ArrayList<Lane> tmpLines, String str) {
		Log.i(TAG, "LaneDetection method : computeMedian enter");
		int i = 0;
		double[] arr = new double[tmpLines.size()];
		double median = 0;

		try {
			if (str == "slope") {
				for (Lane l : tmpLines) {
					arr[i] = l.computeSlope();
					i++;
				}
			} else if (str == "bias") {
				for (Lane l : tmpLines) {
					arr[i] = l.computeBias();
					i++;
				}
			}
			Arrays.sort(arr);
			int midLength = arr.length / 2;
			median = arr[midLength];
			return median;
		} catch (Exception e) {
			// TODO: handle exception
			Log.i(TAG,
					"Unknown exception in LaneDetection method : computeMedian :"
							+ e.toString() + e.getLocalizedMessage()
							+ e.getMessage());
		}
		Log.i(TAG, "LaneDetection method : computeMedian exit");
		return median;
	}

	/**
	 * find lines with slope between 30 and 60 degrees
	 * 
	 * @param colorImage
	 */

	public void getLaneLines(Mat colorImage) {
		Log.i(TAG, "LaneDetection method : getLaneLines enter");
		Mat imgROI = new Mat();
		Mat imgGray = new Mat();
		Mat imgBlur = new Mat();
		Mat imgEdge = new Mat();
		Mat detectedLines = new Mat();

		// detectedLinesList = new ArrayList<Lane>();
		// candidateLinesList = new ArrayList<Lane>();

		// JNI native code :set ROI
		// mView = new LaneDetectionView();
		mView.getROI(colorImage, imgROI);
		Imgproc.cvtColor(imgROI, imgGray, Imgproc.COLOR_BGR2GRAY);
		// Imgproc.GaussianBlur(imgGray, imgBlur, new Size(17, 17), 0);
		Imgproc.medianBlur(imgGray, imgBlur, 7);
		Imgproc.Canny(imgBlur, imgEdge, 50, 80);
		// Imgproc.HoughLinesP(imgEdge, detectedLines, 2, Math.PI / 180, 1, 15,
		// 5);
		Imgproc.HoughLinesP(imgEdge, detectedLines, 2, Math.PI / 180, 10, 15, 5);

		// convert (x1, y1, x2, y2) into Lanes
		for (int i = 0; i < detectedLines.rows(); i++) {
			double[] points = detectedLines.get(i, 0);
			Lane lane = new Lane();
			lane.x1 = points[0];
			lane.y1 = points[1];
			lane.x2 = points[2];
			lane.y2 = points[3];

			detectedLinesList.add(lane);
		}

		// consider only lines with slope between 30 and 60 degrees
		for (Lane lane : detectedLinesList) {
			double slope = Math.abs(lane.computeSlope());
			// if (slope >= 0.55 && slope <= 1.73) {
			if (slope >= 0.5 && slope <= 2) {
				candidateLinesList.add(lane);
				// for debug
				// lane.draw(colorImage, new Scalar(0, 0, 255), 3);
				// Log.i(TAG, "lane.slope:" + lane.computeSlope());
			}
		}
		// for debug
		// Iterator it1 = candidateLinesList.iterator();
		// while (it1.hasNext()) {
		// Log.i(TAG, "candidateLinesList lines:" + it1.next());
		// }

		computeLaneFromCandidates(imgGray.rows());
		Log.i(TAG, "LaneDetection method : getLaneLines exit");
	}

	/**
	 * ���㱾֡ʶ������󳵵��ߵ�б��
	 * 
	 * @param leftLane
	 *            ʶ������󳵵���
	 * @return �󳵵��ߵ�б��
	 */
	public double getLeftLaneSlope() {
		return leftLane.computeSlope();
	}

	/**
	 * ���㱾֡ʶ������ҳ����ߵ�б��
	 * 
	 * @param rightLane
	 *            ʶ������ҳ�����
	 * @return �ҳ����ߵ�б��
	 */
	public double getRightLaneSlope() {
		return rightLane.computeSlope();
	}

	/**
	 * �����һ֡ʶ����ĳ����ߵ�б��
	 * 
	 * @param oldLeftSlope
	 *            ʶ������󳵵��ߵ�б��
	 * @param oldRightSlope
	 *            ʶ������ҳ����ߵ�б��
	 */
	public void setLaneSlope(double oldLeftSlope, double oldRightSlope) {
		oldLeftLaneSlope = oldLeftSlope;
		oldRightLaneSlope = oldRightSlope;
	}

	/**
	 * ����ƫ��ʶ����Ԥ��
	 * 
	 * @param imagew
	 *            ͼ��Ŀ�
	 * @param imageh
	 *            ͼ��ĸ�
	 * @return �Ƿ�ƫ��
	 */
	public void checkDeparture(Mat inputRgba) {
		// TODO
		Point pt1Warning = new Point(10, 10);
		Point pt2Warning = new Point(60, 60);
		Point pt1WarningRight = new Point(inputRgba.cols() - 60, 10);
		Point pt2WarningRight = new Point(inputRgba.cols() - 10, 60);

		// threshold1���󳵵���ǰ����֮֡���б�ʱ仯��С
		double threshold1 = Math
				.abs(oldLeftLaneSlope - leftLane.computeSlope());
		// threshold2���ҳ�����ǰ����֮֡���б�ʱ仯��С
		double threshold2 = Math.abs(oldRightLaneSlope
				- rightLane.computeSlope());
		// threshold3�����ҳ�����б�ʲ�ֵ�Ĵ�С
		double threshold3 = Math.abs(Math.abs(leftLane.computeSlope())
				- Math.abs(rightLane.computeSlope()));

		// �ж��Ƿ��Ǵ����ĵ�һ֡ͼ�����ǵ�һ֡�򲻽�����ǰһ֡ͼ��ԱȵĲ�����
		if (flag) {
			flag = false;
		} else {

			if (threshold1 >= 0.5 && threshold3 >= 1.0) {// �������ƫ
				Imgproc.rectangle(inputRgba, pt1Warning, pt2Warning,
						new Scalar(0, 255, 0), -1, 8, 0);// green
				Imgproc.rectangle(inputRgba, pt1WarningRight, pt2WarningRight,
						new Scalar(255, 0, 0), -1, 8, 0);// red

			} else if (threshold2 > 0.5 && threshold3 >= 1.0) { // �������ƫ
				Imgproc.rectangle(inputRgba, pt1Warning, pt2Warning,
						new Scalar(255, 0, 0), -1, 8, 0);// red
				Imgproc.rectangle(inputRgba, pt1WarningRight, pt2WarningRight,
						new Scalar(0, 255, 0), -1, 8, 0);// green
			} else {
				Imgproc.rectangle(inputRgba, pt1Warning, pt2Warning,
						new Scalar(0, 255, 0), -1, 8, 0);// green
				Imgproc.rectangle(inputRgba, pt1WarningRight, pt2WarningRight,
						new Scalar(0, 255, 0), -1, 8, 0);// green }
			}
		}
	}

	/**
	 * ʶ���ɫͼ�еĳ�����
	 * 
	 * @param inputRgba
	 */
	public void colorFrameLaneLine(Mat inputRgba) {
		Log.i(TAG, "LaneDetection method : colorFrameLaneLine enter");
		int imgh = inputRgba.rows();
		int imgw = inputRgba.cols();

		getLaneLines(inputRgba);

		// prepare empty mask on which lines are drawn�� draw lanes on mask
		Mat lineImg = Mat.zeros(imgh, imgw, CvType.CV_8UC4);
		leftLane.draw(lineImg, new Scalar(0, 255, 255), 10);
		rightLane.draw(lineImg, new Scalar(0, 255, 255), 10);

		// JNI: by native code��from LaneDetectionView methods
		// ֻ����ʶ���������ֱ�ߵ��°벿�֡�make blend on color image

		// mView = new LaneDetectionView();
		mView.detect(inputRgba, lineImg);
		checkDeparture(inputRgba);
		Log.i(TAG, "LaneDetection method : colorFrameLaneLine exit");
	}
}
