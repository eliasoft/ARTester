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
import androidx.annotation.Nullable;
import android.content.*;
import android.view.View;
import android.widget.*;
import java.io.*;
import eliasoft.common.Spinner3D;

public class TestActivity extends ConsoleActivity implements 
		Runnable, CompoundButton.OnCheckedChangeListener{

	public void setMyView(@Nullable View v){
		if(testModesContainer.getChildCount()>1)testModesContainer.removeViewAt(1);
		if(v==null)myView=null;else testModesContainer.addView((myView=v));
		testModeSwitcher.setChecked(v==null);
	}
	public void startTest(Intent i){
		startActivity(i.putExtra("class",i.getComponent().getClassName()).setComponent(new ComponentName(this, TestActivity.SecondaryTestsStarterActivity.class)));
	}
	public TextView getConsoleTextView(){return consoleTextView;}

	// The reloadOverlay represents the View that is explained in the lines mentioning "reloadOverlay" (inside MainActivity).
	static boolean canPutReloadOverlay;static ImageView reloadOverlay;
	private boolean startedFromAppCode;// by default the value of this field will be true, unless the startTest(intent) method is called from external java code.
	private boolean finishTaskCommandReceiverHasRegistered;
	class FinishTaskCommandReceiver extends BroadcastReceiver{public void onReceive(Context ctx, Intent itn){nullifyVariables(true);}}
	private static dalvik.system.DexClassLoader dexClassLoader;
	private Object instanceToCurrentTestingClass;
	private LinearLayout testModesContainer;// test modes can be one of "console view" or "my view".
	private Switch testModeSwitcher;
	private HorizontalScrollView consoleTextViewParent;// see the toggleTextWrap() method.
	private View myView;// see both setMyView() and onCheckedChanged() methods.

	@Override protected void onCreate(android.os.Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		consoleTextViewParent=(HorizontalScrollView)consoleTextView.getParent();
		if((startedFromAppCode=getIntent().hasExtra("currentProject")))App.currentProject=(File)getIntent().getSerializableExtra("currentProject");
		if(!finishTaskCommandReceiverHasRegistered){finishTaskCommandReceiverHasRegistered=true;registerReceiver(new TestActivity.FinishTaskCommandReceiver(), new IntentFilter(TestActivity.FinishTaskCommandReceiver.class.getName()), Manifest.permission.RECEIVE_PRIVATE_BROADCASTS, null);}
		if(getIntent().getStringExtra("class")!=null){
			testModeSwitcher=(Switch)((LinearLayout)findViewById(R.id.firstButtons)).getChildAt(1);
			testModeSwitcher.setOnCheckedChangeListener(this);
			testModesContainer=(LinearLayout)findViewById(R.id.test_modes_container);
			System.setErr(new PrintStream(new TestActivity.SystemErrToCrashReportStream()));
			Spinner3D.show(this, this);
		}else{
			((LinearLayout)findViewById(R.id.firstButtons)).removeViewAt(1);// remove the testModeSwitcher
			((LinearLayout)((LinearLayout)consoleTextView.getParent().getParent().getParent()).getChildAt(0)).removeViewAt(0);// remove the Auto scroll Switch
		}
	}
	@Override public void run(){// ↑
		if(startedFromAppCode)dexClassLoader=new dalvik.system.DexClassLoader(getIntent().getStringExtra("dexPaths"), getCodeCacheDir().getPath(), null, getClass().getClassLoader());
		Spinner3D.hide(()->{try{
			instanceToCurrentTestingClass=dexClassLoader.loadClass(getIntent().getStringExtra("class")).getConstructor(TestActivity.class).newInstance(this);
		}catch(Throwable th){th.printStackTrace();}});
	}

	@Override public void onBackPressed(){
		if(instanceToCurrentTestingClass!=null){
			try{if(!((boolean)instanceToCurrentTestingClass.getClass().getMethod("onBackPressed").invoke(instanceToCurrentTestingClass)))return;}catch(ReflectiveOperationException roe){}
		}
		if(instanceToCurrentTestingClass!=null&&!startedFromAppCode){super.onBackPressed();nullifyVariables(false);}else{setResult(RESULT_OK, null);nullifyVariables(true);}
	}
	private void nullifyVariables(boolean finishTask){
		dexClassLoader=null;instanceToCurrentTestingClass=null; testModesContainer=null;testModeSwitcher=null;consoleTextView=null;consoleTextViewParent=null;myView=null;
		if(finishTask)finishAndRemoveTask();
	}

	static void reloadProject(Context ctx, boolean calledFromReloadOverlay){
		// this method is also called from App.ToggleReloadOverlay.onReceive() > lambda OnClickListener
		ctx.startActivity(new Intent(ctx, MainActivity.class).putExtra("load project", App.currentProject).putExtra("called from reloadOverlay",calledFromReloadOverlay).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
	}
	public void reloadProject(View firstButtonInLayout){reloadProject(this, false);}

	@Override public void onCheckedChanged(CompoundButton cb, boolean isChecked){
		// this method used by the testModeSwitcher
		if(isChecked){
			testModesContainer.getChildAt(0).setVisibility(View.VISIBLE);
			if(myView!=null)myView.setVisibility(View.GONE);
		}else if(myView!=null){
			testModesContainer.getChildAt(0).setVisibility(View.GONE);
			myView.setVisibility(View.VISIBLE);
		}else cb.setChecked(true);
	}
	public void toggleAutoScroll(View v){
		consoleTextView.setMovementMethod(((Switch)v).isChecked()?new android.text.method.ScrollingMovementMethod():null);
		if(((Switch)v).isChecked())((ScrollView)consoleTextView.getParent().getParent()).setScrollY(consoleTextView.getHeight());
	}
	public void toggleTextWrap(View v){
		if(((Switch)v).isChecked()){
			ScrollView sv=(ScrollView)consoleTextViewParent.getParent();
			consoleTextViewParent.removeView(consoleTextView); sv.removeView(consoleTextViewParent); sv.addView(consoleTextView);
		}else{
			ScrollView sv=(ScrollView)consoleTextView.getParent();
			sv.removeView(consoleTextView); sv.addView(consoleTextViewParent); consoleTextViewParent.addView(consoleTextView);
		}
	}

	private class SystemErrToCrashReportStream extends OutputStream{
		@Override public void write(int i) throws IOException{append(String.valueOf(i));}
		@Override public void write(byte[] b) throws IOException{append(new String(b));}
		@Override public void write(byte[] b, int off, int len) throws IOException{append(new String(b, off, len));}
		private StringBuilder report;
		private Thread pauseDetector;
		private void append(String s){
			if(pauseDetector!=null)pauseDetector.interrupt();
			else report=new StringBuilder();
			report.append(s);
			pauseDetector=new Thread(()->{
				try{Thread.sleep(250);
					java.util.ArrayList<String> al=new java.util.ArrayList<>();al.add("ERROR:\n"+report.toString());al.addAll(App.logs);
					startActivity(new Intent(App.ref, TestActivity.class).putExtra("currentProject",App.currentProject).putStringArrayListExtra("diagnostics",al).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
				System.exit(0);}catch(InterruptedException ie){}
			});pauseDetector.start();
		}
	}

	public static class SecondaryTestsStarterActivity extends android.app.Activity{
		@Override protected void onCreate(android.os.Bundle savedInstanceState){
			super.onCreate(savedInstanceState);
			startActivity(getIntent().setComponent(new ComponentName(this, TestActivity.class)).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
		finish();}
	}
}