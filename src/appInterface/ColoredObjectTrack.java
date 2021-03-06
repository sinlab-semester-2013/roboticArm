package appInterface;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvFlip;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvInRangeS;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_MEDIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvEqualizeHist;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetCentralMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetSpatialMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMoments;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import geometric.RelativePoint;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JPanel;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;

import constants.Constants;

public class ColoredObjectTrack implements Runnable {
    final int INTERVAL = 1000;// 1sec
    final int CAMERA_NUM = 0; // Default camera for this time

    /**
     * Correct the color range- it depends upon the object, camera quality,
     * environment.
     */
    static CvScalar rgba_min = cvScalar(0, 0, 130, 0);  // RED wide dabur birko
    static CvScalar rgba_max = cvScalar(80, 80, 255, 0);

    IplImage image;
    CanvasFrame canvas = new CanvasFrame("Web Cam Live");
    CanvasFrame path = new CanvasFrame("Detection");
    
    int ii = 0;
    JPanel jp; 
    
    ArrayList<RelativePoint> points = new ArrayList<RelativePoint>();
    boolean paused = false;

    public ColoredObjectTrack(JPanel jp) {
    	path.dispose();
    	canvas.setLocation(700, 100);
//        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
//        path.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    	this.jp = jp;
        path.setContentPane(jp);
        this.jp.setSize(canvas.getSize().width, canvas.getSize().height);	// ??????
        path.setSize(canvas.getSize().width, canvas.getSize().height);

    }

    @Override
    public void run() {
        FrameGrabber grabber = new OpenCVFrameGrabber(CAMERA_NUM);
        try {
            grabber.start();
            IplImage img;
            int posX = 0;
            int posY = 0;
            while (true) {
                img = grabber.grab();
                if (img != null) {
                    // show image on window
                    cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise
                    canvas.showImage(img);
                    IplImage detectThrs = getThresholdImage(img);

                    CvMoments moments = new CvMoments();
                    cvMoments(detectThrs, moments, 1);
                    double mom10 = cvGetSpatialMoment(moments, 1, 0);
                    double mom01 = cvGetSpatialMoment(moments, 0, 1);
                    double area = cvGetCentralMoment(moments, 0, 0);
                    posX = (int) (mom10 / area);
                    posY = (int) (mom01 / area);
                    // only if its a valid position
                    if (posX > 0 && posY > 0 && !paused){
                        paint(img, posX, posY);
                    } 
                    
                    repaintPoints();	// Repaints the points already saved
                }
                // Thread.sleep(INTERVAL);
            }
        } catch (Exception e) {
        }
    }
    
    private void paintMouse(Graphics g, IplImage img, int posX, int posY) {
         g.clearRect(0, 0, img.width(), img.height());	// Mouse mode
         g.setColor(Color.BLACK);
         g.fillOval(posX, posY, 20, 20);
         g.drawOval(posX, posY, 20, 20);
    }

    private void paint(IplImage img, int posX, int posY) {
        Graphics g = jp.getGraphics();
        g.setColor(Color.RED);
    	RelativePoint point = new RelativePoint(posX, posY, -50, 0, Constants.DEFAULT_FLOW1, 0); // Strawberry points
        points.add(point);
        g.fillOval(posX, posY, 20, 20);
        g.drawOval(posX, posY, 20, 20);
        
        System.out.println(posX + " , " + posY);

    }

    private void repaintPoints() {
        Graphics g = jp.getGraphics();

        g.setColor(Color.RED);
    	for (int i = 0; i < points.size(); i++){
    		RelativePoint point = points.get(i);
    		
            g.fillOval((int)point.getX(), (int)point.getY(), 20, 20);
            g.drawOval((int)point.getX(), (int)point.getY(), 20, 20);
    	}
    }
    
    private IplImage getThresholdImage(IplImage orgImg) {
        IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
        //
        cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red

        cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 15);
        //cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
        return imgThreshold;
    }

//    public static void main(String[] args) {
//        ColoredObjectTrack cot = new ColoredObjectTrack();
//        Thread th = new Thread(cot);
//        th.start();
//    }

    public IplImage Equalize(BufferedImage bufferedimg) {
        IplImage iploriginal = IplImage.createFrom(bufferedimg);
        IplImage srcimg = IplImage.create(iploriginal.width(), iploriginal.height(), IPL_DEPTH_8U, 1);
        IplImage destimg = IplImage.create(iploriginal.width(), iploriginal.height(), IPL_DEPTH_8U, 1);
        cvCvtColor(iploriginal, srcimg, CV_BGR2GRAY);
        cvEqualizeHist(srcimg, destimg);
        return destimg;
    }
    
    public ArrayList<RelativePoint> getPoints() {
    	return points;
    }
    
    public void setPauseMode(boolean pause) {
    	this.paused = pause;
    }
}
