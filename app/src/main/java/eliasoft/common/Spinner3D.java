/*
* Copyright (C) 2022 ELIASoft <eliasoft.developer@gmail.com>.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package eliasoft.common;
import androidx.annotation.Nullable;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.RelativeLayout;
import java.lang.ref.WeakReference;

/*
* The "Spinner3D" is a dialog wich renders a personalized 3D version of the indeterminate progress-bar animation provided by Android sdk.
* If you plan to use this utility in your project, you should copy the lines of code that are inside the methods of the BaseActivity class, in order to decrease the chance of unwanted behavior.
* Below is a basic example using the Spinner3D class.
*
* package com.example.myapp;
* import eliasoft.common.Spinner3D;
* public class MainActivity extends android.app.Activity{
* 	@Override protected void onCreate(android.os.Bundle savedInstanceState){
* 		super.onCreate(savedInstanceState);
* 		Spinner3D.colorResource=R.color.myColor;
* 		setContentView(blablabla);
* 		Spinner3D.show(this, null);
* 		// running background task.
*	 	new Thread(()->{
* 			while(running)){
* 				// code...
* 			}
* 			Spinner3D.hide(null);
* 		}).start();
* 	}
* }
* If you think some lines of code contained in this file should be replaced with something better and more efficient, please consider letting me know. any suggestion will be highly appreciated.
* If possible, send me a copy of this file that has been modified by you. whether you change some parts or add new content.
*/
@SuppressWarnings("deprecation")
public class Spinner3D extends Thread implements SurfaceHolder.Callback
{

	// you must set the value of "colorResource" field, in order to use a color based on the app theme for the Spinner3D.
	public static int colorResource;

	public static void show(Activity aty, @Nullable Runnable toDoWhenShowing){
		// The Runnable passed as the param @toDoWhenShowing, executes the run method as soon as the Spinner3D is visible to the user. That helps to avoid blocking the UI-thread before the Spinner3D is shown.
		if(colorResource==0)throw new RuntimeException("you haven't set a color yet");
		hide(null); showTask=new AsyncTask<Void, Void, Void>(){
			@Override public Void doInBackground(Void... v){while(surfaceView!=null);/*see the hide() method*/return null;}
			@Override public void onPostExecute(Void result){
				int size=BaseApplication.convertDipToPx(160.0F);
				surfaceView=new SurfaceView(aty); surfaceView.setZOrderOnTop(true); surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT); surfaceView.getHolder().addCallback(new Spinner3D(size));
				Dialog d=new Dialog(aty, themeResource); dialogInstance=new WeakReference<>(d);
				RelativeLayout rl=new RelativeLayout(aty);
				RelativeLayout.LayoutParams rllp=new RelativeLayout.LayoutParams(size, size);rllp.addRule(RelativeLayout.CENTER_IN_PARENT);
				rl.addView(surfaceView, rllp);
				d.setContentView(rl, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); d.setCancelable(false);
				d.show(); if(toDoWhenShowing!=null)surfaceView.post(toDoWhenShowing);
			showTask=null;}
		};showTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	public static void hide(@Nullable Runnable toDoAfterHide){
		// The Runnable passed as the param @toDoAfterHide, executes the run method as soon as the Spinner3D is not visible to the user. That helps to avoid strange behavior in some situations.
		// Fo example: you must not call startActivity(intent) inmediately after calling Spinner3D.hide(). instead, you should to write Spinner3D.hide(()->startActivity(intent));
		if(showTask!=null){showTask.cancel(true);showTask=null;}
		if(surfaceView==null){if(toDoAfterHide!=null)toDoAfterHide.run();return;}
		surfaceView.post(()->{
			if(dialogInstance.get()!=null&&dialogInstance.get().isShowing())dialogInstance.get().dismiss();
			dialogInstance.clear();surfaceView=null;if(toDoAfterHide!=null)toDoAfterHide.run();
		});surfaceView.getHolder().getSurface().release();surfaceObject=null;
	}// The following provides a global instance to the Spinner3D dialog, wich you can use to detect when that is dismissed at abnormal situations such as those explained in the BaseActivity class.
	public static volatile WeakReference<Dialog> dialogInstance=new WeakReference<>(null);

	public static int themeResource;// this field will be removed in the future.
	private static volatile AsyncTask<Void, Void, Void> showTask;// see the show() method.
	private static volatile SurfaceView surfaceView;
	private static volatile Surface surfaceObject;
	private final boolean drawCanvasToPngFile=false;/* you can set this field to true. If true, means you want to draw in a canvas that will not be rendered, but will be stored as a sequence of PNG files. and then be embedded into a animated GIF file.*/ int pngFrameIndex;
	private int viewSize;// determined from the showSpinner3d() method, the default value is the result of converting 160dp to the equivalent in pixels.
	private float spinnerCenterPoint;// viewSize/2
	private boolean firstDrawn;// only relevant the first time that the run() method is executed.
	private int[] rate;// used for Thread.sleep(long, int)
	private long emulationStabilizationTime;// see the instructions within the if() condition in the r() method.
	private final float targetDensity=drawCanvasToPngFile?1.0F:BaseApplication.ref.getResources().getDisplayMetrics().density;
	private float[] angles;// used to define the angle related parameters passed to the Canvas.drawArc() method each time the canvas are drawn.
	private int rotationAngle=120;// the rotation angle is changed each time that r() method is called (each two seconds).
	private float angleStepF;// this defines the number of angles that should be increased or decreased each time the arcs angle are changed after drawing the arcs.
	private boolean alternate;// when false, the arc will be drawn from the starting angle until it reaches 360 degrees. otherwise (when true), the arc will be drawn alternately, that is: from the end angle (360 degrees) to the start angle.
	private int[] angleOffsets;// This array consists of two items, which define a offset effect with the angles during a short time each time the value of the "alternate" field is changed. Note: This comment needs to be reformulated.
	private int[] i;// I really don't know what explanation to provide for this field.
	private RectF spinnerBounds; private Paint spinnerPaint, clearPaint;

	private Spinner3D(int viewSize){
		if(drawCanvasToPngFile)viewSize=160;this.viewSize=viewSize;
		spinnerCenterPoint=(float)viewSize/2.0F;
		float fps=drawCanvasToPngFile?12.5F:((WindowManager)BaseApplication.ref.getSystemService(android.content.Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate();
		float t=(1000.0F/fps)%1000000.0F;// gets the most apropiated duration in milliseconds and nanoseconds for each @frame. E.q.: 16.666666 for 60fps.
		rate=new int[]{(int)t, Integer.parseInt(String.valueOf(t).substring(String.valueOf(t).indexOf(".")+1)), 0,0}; rate[2]=rate[0];rate[3]=rate[1];
		angles=new float[]{0.0F, 18.0F/*minimum arc end-angle*/};
		angleStepF=342.0F/(fps/1.25F);
		angleOffsets=new int[]{(int)(fps/5.0F), Math.round(fps/5.0F)};
		i=new int[]{0, (120+18/*18 is the minimum arc end-angle*/)/angleOffsets[1]};
		spinnerBounds=newRectF(20); int color=Build.VERSION.SDK_INT>22?BaseApplication.ref.getColor(colorResource):BaseApplication.ref.getResources().getColor(colorResource);
		Bitmap pattern=Bitmap.createBitmap(viewSize, viewSize, Bitmap.Config.ARGB_8888); Canvas c=new Canvas(pattern);
		/*draw colorResource arc*/c.drawArc(spinnerBounds,0,360,false,newPaint(40, color));
		/*draw darken arcs*/c.drawArc(newRectF(0),0,360,false,newPaint(2,0xE0000000));c.drawArc(newRectF(40),0,360,false,newPaint(2,0xE0000000)); c.drawArc(newRectF(2),0,360,false,newPaint(2,0xC0000000));c.drawArc(newRectF(38),0,360,false,newPaint(2,0xC0000000)); c.drawArc(newRectF(4),0,360,false,newPaint(2,0xA0000000));c.drawArc(newRectF(36),0,360,false,newPaint(2,0xA0000000)); c.drawArc(newRectF(6),0,360,false,newPaint(2,0x80000000));c.drawArc(newRectF(34),0,360,false,newPaint(2,0x80000000)); c.drawArc(newRectF(8),0,360,false,newPaint(2,0x60000000));c.drawArc(newRectF(32),0,360,false,newPaint(2,0x60000000)); c.drawArc(newRectF(10),0,360,false,newPaint(2,0x40000000));c.drawArc(newRectF(30),0,360,false,newPaint(2,0x40000000)); c.drawArc(newRectF(12),0,360,false,newPaint(2,0x20000000));c.drawArc(newRectF(28),0,360,false,newPaint(2,0x20000000));
		/*draw lighten arcs*/c.drawArc(spinnerBounds,0,360,false,newPaint(2,0xC0FFFFFF)); c.drawArc(newRectF(18),0,360,false,newPaint(2,0x40FFFFFF));c.drawArc(newRectF(22),0,360,false,newPaint(2,0x40FFFFFF));
		spinnerPaint=newPaint(40, color);spinnerPaint.setShader(new BitmapShader(pattern, Shader.TileMode.CLAMP,Shader.TileMode.CLAMP)); clearPaint=new Paint();clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		pattern=null;
	}
	private RectF newRectF(int dimen){return new RectF(targetDensity*dimen,targetDensity*dimen,(viewSize-(targetDensity*dimen)),(viewSize-(targetDensity*dimen)));}
	private Paint newPaint(int width, int color){Paint p=new Paint();p.setAntiAlias(true);p.setDither(true);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(targetDensity*width);p.setColor(color);return p;}
	@Override public void surfaceCreated(SurfaceHolder sh){surfaceObject=sh.getSurface(); start();}
	@Override public void surfaceDestroyed(SurfaceHolder sh){surfaceObject=null;}
	@Override public void surfaceChanged(SurfaceHolder sh, int frmt, int w, int h){}
	@Override public void run(){
		while(surfaceObject!=null){try{
			if(emulationStabilizationTime==0)emulationStabilizationTime=SystemClock.elapsedRealtimeNanos();
			Bitmap bm=drawCanvasToPngFile?Bitmap.createBitmap(viewSize, viewSize, Bitmap.Config.ARGB_8888):null;
			Canvas c=drawCanvasToPngFile?new Canvas(bm): (Build.VERSION.SDK_INT>22?surfaceObject.lockHardwareCanvas():surfaceObject.lockCanvas(null));
			if(c==null){bm=null;break;}
			if(!firstDrawn){r();firstDrawn=true;} c.rotate((float)rotationAngle, spinnerCenterPoint,spinnerCenterPoint);
			c.drawPaint(clearPaint); c.drawArc(spinnerBounds,angles[0],angles[1],false,spinnerPaint);
			if(!drawCanvasToPngFile)surfaceObject.unlockCanvasAndPost(c);
			if(drawCanvasToPngFile&&pngFrameIndex<25){try{pngFrameIndex++;java.io.FileOutputStream fos=new java.io.FileOutputStream("/sdcard/Download/canvas_png/"+pngFrameIndex+".png");bm.compress(Bitmap.CompressFormat.PNG,0,fos);fos.close();fos=null;}catch(java.io.IOException ioe){ioe.printStackTrace();}}bm=null;
			if(i[0]>0){
				if(alternate)angles[0]=drawCanvasToPngFile?0.0F:angles[0]+i[1];
				if(i[0]++==angleOffsets[alternate?1:0]){
					i[0]=0;angles[0]=0.0F;
					if(alternate)r(); alternate=!alternate;
				}
			}else if(alternate){
				angles[0]=Math.min(angles[0]+angleStepF, 342.0F);
				angles[1]=Math.max(angles[1]-angleStepF, 18.0F);
				if(angles[0]>=342.0F)i[0]++;
			}else{
				angles[1]=Math.min(angles[1]+angleStepF, 360.0F);
				if(angles[1]>=360.0F)i[0]++;
			} Thread.sleep(rate[0], rate[1]);
		}catch(Exception ex){break;}}
		surfaceObject=null; i=rate=null; angles=null; spinnerBounds=null; spinnerPaint=clearPaint=null;
	}
	private void r(){// invoked each two seconds.
		rotationAngle=(rotationAngle+120)%360;
		if(Build.VERSION.SDK_INT<=22&&rate[2]>30){// emulate hardware_acceleration for SurfaceView (API <23).
			emulationStabilizationTime=Math.max(Long.parseLong(String.valueOf(rate[2])+String.valueOf(rate[3]))-(SystemClock.elapsedRealtimeNanos()-emulationStabilizationTime), 0);
			rate[0]=(int)(emulationStabilizationTime/1000000); rate[1]=(int)(emulationStabilizationTime%1000000); emulationStabilizationTime=0;
		}
	}
}