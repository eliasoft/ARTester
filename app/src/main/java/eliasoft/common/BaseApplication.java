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
import android.app.*;
import android.content.res.Configuration;
import android.os.Build;
import android.provider.Settings;
import android.text.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Executors;

public abstract class BaseApplication extends Application implements
																Thread.UncaughtExceptionHandler{

	public static BaseApplication ref;// apart from providing a static reference to this Application subclass. It is also used to provide a Context instance when required within an arbitrary class which itself does not have a way to get a Context reference.
	public static String APP_LABEL_AND_VERSION;// "MyApp vX.X(X)?"

	public final static int convertDipToPx(float dipData){
		return Math.round(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, dipData, ref.getResources().getDisplayMetrics()));
	}
	public final static Spanned fromHtml(String str){
		return Build.VERSION.SDK_INT>23?Html.fromHtml(str, Html.FROM_HTML_OPTION_USE_CSS_COLORS) :Html.fromHtml(str);
	}

	public final static android.content.SharedPreferences prefs(){
		return android.preference.PreferenceManager.getDefaultSharedPreferences(ref);
	}

	public final static void showDialog(Activity aty, AlertDialog.Builder d){
		// this method is used to prevent throwing an exception of type: java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
		aty.runOnUiThread(d::show);
	}

	public static void stretchListView(android.widget.ListView lv){
		/* Sometimes I want to display a ListView that will live with other views (all inside a LinearLayout). In such cases I prefer to make said ListView behave as if it were a LinearLayout, so that when I scroll through the list, the entire content of the UI is scrolled instead of scrolling the ListView itself.
		* This method will cause the ListView to stretch vertically to the height that will be obtained by suming the height of all its childrens.
		*/
		android.widget.ListAdapter adp=lv.getAdapter();int lh=0, divH=lv.getDividerHeight();
		for(int i=0;i<adp.getCount();i++){android.view.View v=adp.getView(i, null, lv);v.measure(0,0);lh+=divH+v.getMeasuredHeight();}
		android.view.ViewGroup.LayoutParams lp=lv.getLayoutParams();lp.height=lh;lv.setLayoutParams(lp);lv.requestLayout();
	}

	// Personally. I prefer to avoid using the "android.util.Log" class methods. Instead I prefer to write my records to a list which will be readed when the app crashes.
	public final static ArrayList<String> logs=new ArrayList<>(1000);
	public final static void l(String msg){
		// this method is my solution to replace the use of android.util.Log class methods.
		if(logs.size()==1000)logs.remove(logs.size()-1);
		logs.add(0, msg.replace("\n","\\n"));
	}

	@Override public void onCreate(){
		super.onCreate();ref=this;
		try{APP_LABEL_AND_VERSION=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getPackageName(),0))+" v"+getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException nnfe){}
		Thread.setDefaultUncaughtExceptionHandler(this);
		readLogcat("logcat System.err:W *:S");readLogcat("logcat -b crash");
		Executors.newSingleThreadExecutor().execute(()->{try{
			Thread.sleep(200);System.err.println("check if logger is enabled");
			Thread.sleep(500);if(!loggerIsEnabled)System.setErr(new PrintStream(new OutputStream(){
				@Override public void write(int i) throws IOException{append(String.valueOf(i));}
				@Override public void write(byte[] b) throws IOException{append(new String(b));}
				@Override public void write(byte[] b, int off, int len) throws IOException{append(new String(b, off, len));}
				private StringBuilder crashData;
				private void append(String s){
					if(crashData==null)crashData=new StringBuilder();
					processCrashDataLine(crashData, s);
				}
			}));
		}catch(InterruptedException ie){}});
	}

	@Override public final void uncaughtException(Thread t, Throwable th){
		if(!loggerIsEnabled)th.printStackTrace();
		// the else logic will be managed from the readLogcat() method. see comments at the readLogcat() method.
	}
	protected abstract void manageCrashReport(String crashData, String extraData);
	volatile boolean loggerIsEnabled;// set to true if the value of "logger buffer size" setting within Developer options, it's other than off.
	volatile Thread crashProcessingPauseDetector;

	private void readLogcat(String command){
		/* Some time ago I faced a strange problem that I had a hard time solving, because it was an exception that occurred very infrequently, and which came from the native Android library: "/system/lib/libhwui.so".
		* That exception caused my app to crash without giving me a chance to catch the exception from the java code, because such an exception did not propagated to the java side.
		* So I decided to write my own work-around, in order to catch java exceptions and other exceptions thrown from native Android code, but not propagated to java side.
		* In addition, the following code is also executed if methods System.err.println() and Throwable.printStackTrace() are called. This is my intention, as I prefer to make the app crash if an exception is thrown where the program is not worth continuing to run, and instead that is a problem that needs to be fixed.
		* Below is an example explaining about when the app must be crashed if an exceptions that I would not want to catch is thrown.
		* try{
		* 	File f=new File("somefile");
		* 	if(!f.exists())doSomethingHere();
		* 	else{
		* 		FileInputStream fis=new FileInputStream(f);
		* 		// read the file.
		* 		fis.close();
		* 	}
		* }catch(IOException ioe){
		* 	// this exception would be thrown only in very rare situations. but if it does, the code within the try block, and possibly other parts of the program, will need to be modified to prevent the catch block from occurring.
		* 	ioe.printStackTrace();// cause the app to crash.	
		* }
		*/
		Executors.newSingleThreadExecutor().execute(()->{try{
			StringBuilder crashData=new StringBuilder();
			BufferedReader logcatReader=new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(command).getInputStream()));
			// I tried the logcat -c and -T commands, but they didn't work for me. So I had to write some code to skip the log entries that I already read the last time the app crashed.
			Date now=new Date();now.setYear(70/*means 1970*/); java.text.SimpleDateFormat logcatDateFormatExcludingMilliseconds=new java.text.SimpleDateFormat("MM-dd kk:mm:ss");
			String l;while((l=logcatReader.readLine())!=null){
				if(l.startsWith("---"))continue;
				if(l.contains("W System.err: check if logger is enabled")){loggerIsEnabled=true;continue;}
				if(logcatDateFormatExcludingMilliseconds.parse(l.substring(0,l.indexOf("."))).before(now))continue;
				processCrashDataLine(crashData, l.substring(l.substring(0, l.lastIndexOf(" ", l.indexOf(": "))).length()-1).replaceAll(",? [a-zA-Z]+:?.?[0-9]+","")+"\n");
			}
		}catch(Exception ex){System.exit(1);}});
	}
	void processCrashDataLine(StringBuilder crashData, String s){
		if(crashProcessingPauseDetector!=null)crashProcessingPauseDetector.interrupt();
		else startActivity(new android.content.Intent(ref, BaseApplication.DetectedErrorPrintingActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
		crashData.append(s);
		crashProcessingPauseDetector=new Thread(()->{
			try{Thread.sleep(200);
				if(!logs.isEmpty()){crashData.append("--------- beginning of my custom logs\n");for(String otherLog: logs){crashData.append(otherLog).append("\n");}}
				manageCrashReport(crashData.toString(), getExtraData());
				System.exit(0);// at this point, it is obligatory to kill the process of the app.
			}catch(Exception e){}
		});crashProcessingPauseDetector.start();
	}

	private String getExtraData() throws IllegalAccessException{
		StringBuilder sb=new StringBuilder("{\"").append(APP_LABEL_AND_VERSION).append("\", \"Device info\": {\n");
		sb.append("    \"Build\":{ \"SDK_INT\":").append(Build.VERSION.SDK_INT).append(", \"MANUFACTURER\":\"").append(Build.MANUFACTURER).append("\", \"BRAND\":\"").append(Build.BRAND).append("\", \"MODEL\":\"").append(Build.MODEL).append("\", \"PRODUCT\":\"").append(Build.PRODUCT).append("\", \"VERSION.INCREMENTAL\":\"").append(Build.VERSION.INCREMENTAL).append("\", \"HARDWARE\":\"").append(Build.HARDWARE).append("\" },\n");
		sb.append("    \"default shared preferences\":\""+prefs().getAll().toString().replace("\"","\\\"")+"\" },\n");
		android.content.pm.PackageManager pm=getPackageManager(); ArrayList<String> al=new ArrayList<>(100);
		for(android.content.pm.FeatureInfo fi: pm.getSystemAvailableFeatures()){if(fi.name!=null)al.add("\""+fi.name+"\"");}al.sort(String::compareTo);sb.append("    \"available system features\": ").append(al.toString()).append(",\n");
		String s=null;al=new ArrayList<>(100);
		for(Field f: pm.getClass().getFields()){if(f.getName().startsWith("FEATURE_")&&!pm.hasSystemFeature((s=(String)f.get(null))))al.add("\""+s+"\"");}al.sort(String::compareTo);sb.append("    \"unavailable system features\": ").append(al.toString());
		sb.append(",\n    \"Configuration\":{\n");
		Configuration cfg=getResources().getConfiguration();
		for(Field f: cfg.getClass().getFields()){
			if(Modifier.isStatic(f.getModifiers()))continue;
			sb.append("        \"").append(f.getName()).append("\": \"");
			if(f.getType()==int.class){
				int val=f.getInt(cfg);switch(f.getName()){
					case "colorMode":
						if((val&Configuration.COLOR_MODE_HDR_MASK)!=Configuration.COLOR_MODE_HDR_UNDEFINED)sb.append("hdr=").append((val&Configuration.COLOR_MODE_HDR_MASK)==Configuration.COLOR_MODE_HDR_NO?"no":"yes").append(" ");
						if((val&Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_MASK)!=Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_UNDEFINED)sb.append("wideColorGamut=").append((val&Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_MASK)==Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_NO?"no":"yes");
					break;case "screenLayout":
						if((val&Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)!=Configuration.SCREENLAYOUT_LAYOUTDIR_UNDEFINED)sb.append("layoutDir=").append((val&Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)==Configuration.SCREENLAYOUT_LAYOUTDIR_LTR?"ltr":"rtl").append(" ");
						if((val&Configuration.SCREENLAYOUT_LONG_MASK)!=Configuration.SCREENLAYOUT_LONG_UNDEFINED)sb.append("long=").append((val&Configuration.SCREENLAYOUT_LONG_MASK)==Configuration.SCREENLAYOUT_LONG_NO?"no":"yes").append(" ");
						if((val&Configuration.SCREENLAYOUT_ROUND_MASK)!=Configuration.SCREENLAYOUT_ROUND_UNDEFINED)sb.append("round=").append((val&Configuration.SCREENLAYOUT_ROUND_MASK)==Configuration.SCREENLAYOUT_ROUND_NO?"no":"yes").append(" ");
						if((val&Configuration.SCREENLAYOUT_SIZE_MASK)!=Configuration.SCREENLAYOUT_SIZE_UNDEFINED)sb.append("size=").append(String.valueOf(val&Configuration.SCREENLAYOUT_SIZE_MASK));
					break;case "uiMode":
						if((val&Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_UNDEFINED)sb.append("night=").append((val&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_NO?"no":"yes").append(" ");
						if((val&Configuration.UI_MODE_TYPE_MASK)!=Configuration.UI_MODE_TYPE_UNDEFINED)sb.append("type=").append(String.valueOf(val&Configuration.UI_MODE_TYPE_MASK));
					break;default: sb.append(String.valueOf(val));break;
				}
			}else sb.append(f.get(cfg).toString());
			sb.append("\",\n");
		}
		sb.deleteCharAt(sb.length()-2).append("    },\n    \"Settings.Global\":{\n"); android.content.ContentResolver cr=getContentResolver();
		for(Field f: Settings.Global.class.getFields()){if(f.getName().equals("NAME")){sb.deleteCharAt(sb.length()-2).append("    },\n    \"Settings.Secure\":{\n");break;}if(f.getType()==String.class&&!f.isAnnotationPresent(Deprecated.class)&&(s=Settings.Global.getString(cr, (String)f.get(null)))!=null)sb.append("        \"").append(f.getName()).append("\": \"").append(s).append("\",\n");}
		for(Field f: Settings.Secure.class.getFields()){if(f.getName().equals("NAME")){sb.deleteCharAt(sb.length()-2).append("    },\n    \"Settings.System\":{\n");break;}if(f.getType()==String.class&&!f.isAnnotationPresent(Deprecated.class)&&(s=Settings.Secure.getString(cr, (String)f.get(null)))!=null)sb.append("        \"").append(f.getName()).append("\": \"").append(s).append("\",\n");}
		for(Field f: Settings.System.class.getFields()){if(f.getName().equals("NAME")){sb.deleteCharAt(sb.length()-2).append("    }\n}");break;}if(f.getType()==String.class&&!f.isAnnotationPresent(Deprecated.class)&&(s=Settings.System.getString(cr, (String)f.get(null)))!=null)sb.append("        \"").append(f.getName()).append("\": \"").append(s).append("\",\n");}
		return sb.toString();
	}

	public static class DetectedErrorPrintingActivity extends Activity{
		// This is a transient activity that will start when the logcat reader detects System.err.println() method calls. this will stop users from interacting with the app UI, and wait for the crash report activity to be shown.
		@Override protected void onCreate(android.os.Bundle savedInstanceState){super.onCreate(savedInstanceState);setFinishOnTouchOutside(false);}
		@Override public void onBackPressed(){}
	}
}