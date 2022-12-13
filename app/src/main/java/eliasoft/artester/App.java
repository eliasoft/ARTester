/*
* Copyright (C) 2022 ELIASoft <eliasoft.developer@gmail.com>.
*
* This file is part of ARTester.
*
* ARTester is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package eliasoft.artester;
import android.content.*;
import android.os.*;
import android.view.*;
import java.io.File;

public class App extends eliasoft.common.BaseApplication{

	public static void notice(android.app.Activity aty, boolean isAnErrorMessage, CharSequence msg){
		showDialog(aty, new android.app.AlertDialog.Builder(aty).setTitle(isAnErrorMessage?"ERROR!":null).setMessage(msg).setCancelable(true).setPositiveButton(android.R.string.ok, null));
	}

	public static File projectsDirectory;// see the initProjectsDirectory() method.
	public static File classpathsDirectory;// see the initProjectsDirectory() method.
	public static File currentProject;// will be used to store global reference to the "config.txt" file of the last project clicked from the list of projects displayed in the main activity.

	void initProjectsDirectory(){// this method is called from rhe onCreate() method inside both MainActivity and JarActivity.
		projectsDirectory=new File(prefs().getString("projects directory",null));projectsDirectory.mkdir();
		File nomediaFile=new File(projectsDirectory, ".nomedia");try{if(!nomediaFile.exists())nomediaFile.createNewFile();}catch(Exception e){e.printStackTrace();}
		classpathsDirectory=new File(projectsDirectory, "ARTester_classpaths");classpathsDirectory.mkdir();
	}

	@Override public void onCreate(){
		super.onCreate();
		// Logger.initialize(this);// injected by CodeAssist. but i not want that, because this app provides the ability to record logcat.
		eliasoft.common.Spinner3D.colorResource=R.color.green;
		eliasoft.common.Spinner3D.themeResource=R.style.Theme_Spinner3D;
		setTheme(R.style.Theme_App);// forces the framework to use the specified theme when creating/inflating views using non activity context.
		if(isCurrentlyMainProcess())registerReceiver(new App.ToggleReloadOverlay(), new IntentFilter(App.ToggleReloadOverlay.class.getName()), Manifest.permission.RECEIVE_PRIVATE_BROADCASTS, null);
	}
	// you should read the AndroidManifest.xml file to find out which components run in a separate process.
	private boolean isCurrentlyMainProcess(){
		if(Build.VERSION.SDK_INT>27)return getProcessName().equals(getPackageName());
		try{
			return ((String)Class.forName("android.app.ActivityThread").getDeclaredMethod("currentProcessName").invoke(null)).equals(getPackageName());
		}catch(Exception ex){}return true;
	}
	// the following two methods are called when the user press the back button from MainActivity, or when the user make a long click over the reload overlay (see ToggleReloadOverlay at the end of this file).
	void killTestProcess(){android.os.Process.killProcess(getContentResolver().call(android.net.Uri.parse("content://"+getPackageName()), "r", "pid",null).getInt("pid"));}
	void finishAppTasks(){
		for(android.app.ActivityManager.AppTask at: ((android.app.ActivityManager)getSystemService(Context.ACTIVITY_SERVICE)).getAppTasks()){at.finishAndRemoveTask();}
	System.exit(0);}

	@Override protected void manageCrashReport(String crashData, String extraData){
		java.util.ArrayList<String> al=new java.util.ArrayList<>();al.add("ERROR:\n"+crashData);
		if(isCurrentlyMainProcess())al.addAll(getContentResolver().call(android.net.Uri.parse("content://"+getPackageName()), "r", "logs",null).getStringArrayList("logs"));
		al.add(extraData);
		startActivity(new Intent(App.ref, App.CrashReportActivity.class).putStringArrayListExtra("diagnostics",al).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	public static class CrashReportActivity extends ConsoleActivity{
		@Override protected void onCreate(Bundle savedInstanceState){
			super.onCreate(savedInstanceState);
			((ViewGroup)findViewById(R.id.firstButtons)).setVisibility(View.GONE);
			((ViewGroup)((ViewGroup)consoleTextView.getParent().getParent().getParent()).getChildAt(0)).removeViews(0, 2);
		}
		@Override public void onBackPressed(){startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK));}
	}

	// to understand the following lines, you should read the "Put an screen overlay" setting inside main activity.
	class ToggleReloadOverlay extends BroadcastReceiver implements View.OnClickListener, View.OnLongClickListener{
		public void onReceive(Context ctx, Intent itn){
			if(itn.hasExtra("show")){
				if(!TestActivity.canPutReloadOverlay ||TestActivity.reloadOverlay!=null ||App.currentProject==null)return;
				TestActivity.reloadOverlay=new android.widget.ImageView(ref);TestActivity.reloadOverlay.setImageResource(R.mipmap.ic_launcher);
				TestActivity.reloadOverlay.setOnClickListener(this);
				TestActivity.reloadOverlay.setOnLongClickListener(this);
				int size=convertDipToPx(48.0F);
				WindowManager.LayoutParams wlp=new WindowManager.LayoutParams(size, size, Build.VERSION.SDK_INT>25?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT);
				wlp.gravity=Gravity.LEFT|Gravity.TOP;
				new Handler(getMainLooper()).postDelayed(()->{// postDelayed is used here to prevent unwanted blinking the TestActivity.reloadOverlay when moving from one activity to another.
					try{if(TestActivity.reloadOverlay!=null)((WindowManager)getSystemService(Context.WINDOW_SERVICE)).addView(TestActivity.reloadOverlay, wlp);}catch(RuntimeException re){/*already attached to window*/}
				}, 500);
			}else if(TestActivity.reloadOverlay!=null){
				try{((WindowManager)getSystemService(Context.WINDOW_SERVICE)).removeView(TestActivity.reloadOverlay);}catch(IllegalArgumentException iae){}
				TestActivity.reloadOverlay=null;
			}
		}
		@Override public void onClick(View v){
			TestActivity.reloadProject(App.ref, true);
			sendBroadcast(new Intent(TestActivity.FinishTaskCommandReceiver.class.getName()));
		}
		@Override public boolean onLongClick(View view){
			Vibrator v=(Vibrator)App.ref.getSystemService(Context.VIBRATOR_SERVICE);
			if(Build.VERSION.SDK_INT>25)v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(100);
			try{Thread.sleep(200);/*ensure vibration executes before killing the process*/}catch(InterruptedException ie){}
			killTestProcess();finishAppTasks();return true;
		}
	}
}
