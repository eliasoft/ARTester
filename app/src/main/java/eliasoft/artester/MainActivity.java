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
import android.app.AlertDialog;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.util.ArraySet;
import java.io.*;

public class MainActivity extends BaseActivity implements Runnable, View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, CompoundButton.OnCheckedChangeListener{
	private ArraySet<String> projects;

	@Override protected void onCreate(android.os.Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);return;}
		if(!App.prefs().contains("projects directory")){requestToSettingProjectsDirectory();return;}
		((App)App.ref).initProjectsDirectory();
		java.util.concurrent.Executors.newSingleThreadExecutor().execute(this);
		projects=new ArraySet<String>(App.prefs().getStringSet("projects", new ArraySet<String>()));
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		refreshUI(); onNewIntent(getIntent());
	}
	@Override public void run(){// from onCreate(Bundle)
		try{for(String s: new String[]{"eliasoft.artester.jar", "android.jar", "android.jar.properties", "androidx.annotation-1.5.0.jar", "annotation-1.5.0.jar.properties", "core-lambda-stubs.jar", "core-lambda-stubs.jar.properties"}){
			File dest=new File(App.classpathsDirectory, s);
			InputStream is=getAssets().open(s);
			if(dest.canRead()){
				if(dest.length()==is.available()){is.close();continue;}
				if(s.equals("android.jar")&&!App.prefs().getBoolean("overwrite android.jar when if needed",true)){is.close();continue;}
			}
			FileOutputStream fos=new FileOutputStream(dest);
			byte[] buf=new byte[4096];
			int len=0;while((len=is.read(buf))>=0){fos.write(buf, 0, len);}
			is.close();fos.close();
		}}catch(IOException ioe){ioe.printStackTrace();}
	}

	private void refreshUI(){
		LinearLayout ll=new LinearLayout(this, null,0, R.style.ll);
		ll.setPadding(ll.getPaddingLeft(), App.convertDipToPx(24.0F), ll.getPaddingRight(), ll.getPaddingBottom());
		TextView v=new TextView(this);ll.addView(v);
		TextView firstNote=v;
		if(projects.isEmpty())firstNote.setText("If this is the first time you use this app, please read the documentation in the repository.");
		LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);llp.topMargin=llp.bottomMargin=App.convertDipToPx(8.0F);
		ListView projectsListView=new ListView(this);projectsListView.setOnItemClickListener(this);projectsListView.setOnItemLongClickListener(this);ll.addView(projectsListView, llp);
		addButton(ll, 1, "Make offline javadoc for classes in JAR files");
		addButton(ll, 2, "Add existing projects");
		addButton(ll, 3, "Create new example project");
		addButton(ll, 4, "Add an existing project from another location");
		addButton(ll, 5, "Record logcat");
		addButton(ll, 6, "Throw an error in order to get the general app diagnostic logs");
		v=new TextView(this);v.setText("\n\nSETTINGS:\n");v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);ll.addView(v, new LinearLayout.LayoutParams(llp));
		v=new Switch(this);/*here is the reloadOverlay related Switch.*/v.setText("Put an screen overlay so that from another app you can reload the project that is running, without having to go to the test activity. Enable this option to get more details.");((Switch)v).setChecked((TestActivity.canPutReloadOverlay=App.prefs().getBoolean("put reload overlay",false)));((Switch)v).setOnCheckedChangeListener(this);ll.addView(v, new LinearLayout.LayoutParams(llp));
		addButton(ll, 7, "Set the font size that will be applied to TextView's containing dynamic generated contents");
		addButton(ll, 8, "Set the java version used to compile java sources for all projects");
		v=new Switch(this);v.setId(9);v.setText("Overwrite the android.jar file in the ARTester_classpaths folder, when said file is different from the one inside the assets folder in the ARTester APK file.");v.setHint("overwrite android.jar when if needed");((Switch)v).setChecked(App.prefs().getBoolean((String)v.getHint(),true));v.setOnClickListener(this);ll.addView(v, new LinearLayout.LayoutParams(llp));
		addButton(ll, 10, "Change the projects default directory");
		ScrollView sv=new ScrollView(this);sv.addView(ll);setContentView(sv);

		ArrayAdapter ad=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		java.util.Iterator<String> it=projects.iterator();
		while(it.hasNext()){
			String p=it.next();try{
				BufferedReader projectReader=new BufferedReader(new FileReader(p));
				String l; while((l=projectReader.readLine())!=null){
					if(l.length()==0||l.charAt(0)!='"')continue;
					ad.add((l=l.trim()).substring(1,l.length()-1));l=null;break;
				} projectReader.close();
				if(l!=null)ad.add(p.substring(0,p.lastIndexOf(File.separator)));
			}catch(IOException ioe){it.remove();}
		} projectsListView.setAdapter(ad);
		if(!projects.isEmpty())firstNote.setText("You can do: Click on an item in the list to run the code, or long click to remove such item from the list.");
		App.stretchListView(projectsListView);
	}
	private void addButton(LinearLayout ll, int id, String s){
		Button b=new Button(this, null, 0, R.style.btn);b.setText(s);b.setId(id);b.setOnClickListener(this);ll.addView(b, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	}

	@Override protected void onNewIntent(Intent i){
		setIntent(i);// this line is needed to get the updated Intent when getIntent() is called from onActivityResult()
		File project=(File)i.getSerializableExtra("load project");if(project!=null&&projects.contains(project.getPath()))loadProject(project);
	}
	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==2){onCheckedChanged(null, android.provider.Settings.canDrawOverlays(this));refreshUI();}
		else if(getIntent().getBooleanExtra("called from reloadOverlay",false))moveTaskToBack(true);
	}
	@Override public void onBackPressed(){((App)App.ref).killTestProcess();((App)App.ref).finishAppTasks();}
	@Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){if(grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)recreate();else onBackPressed();}

	private void loadProject(File project){
		sendBroadcast(new Intent(TestActivity.FinishTaskCommandReceiver.class.getName()));
		if(!project.exists())App.notice(this, true, "The file no longer exists");
		else if(!project.getParentFile().canWrite())App.notice(this, true, "The parent directory does not have write permissions");
		else{App.currentProject=project;
			eliasoft.common.Spinner3D.show(this, null);new CompileTask(this).execute();
		}
	}
	@Override public void onItemClick(AdapterView<?> av, View v, int pos, long id){loadProject(new File(projects.valueAt(pos)));}
	@Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id){
		projects.removeAt(pos);
		SharedPreferences.Editor spe=App.prefs().edit();spe.putStringSet("projects",projects);spe.apply();
		refreshUI();return true;
	}
	@Override public void onClick(View v){
		EditText et; int id=v.getId();
		switch(id){
			case 1: startActivity(new Intent(this, JarActivity.class));break;
			case 2:
				String s="";
				for(File f: App.projectsDirectory.listFiles()){s+=f.getPath()+File.pathSeparator;}
				save(4, s.endsWith(File.pathSeparator)?s.substring(0,s.length()-1):s);
			break;case 3: try{
				File dest=new File(App.projectsDirectory, "ARTester_Example");
				if(dest.exists()){App.notice(this, true, "A directory named \"ARTester_Example\" already exists, to create a new example project, you must rename said directory using a file manager installed on this device, and then click the \"Add existing projects\" button, so that the directory reappears that I was renamed.\n After you have done the above, it is recommended that you edit the \"config.txt\" file (in the recently renamed directory), and modify the line where the project name is specified.");return;}
				else new AlertDialog.Builder(this).setPositiveButton(android.R.string.ok, null).setCancelable(false).setTitle("Please read carefully.").setMessage(App.fromHtml("To edit the source files of the project you just created, you will have to use one of the following two alternatives, since ARTester does not integrate a file explorer or a text editor.<br/><br/><b>Alternative 1</b>. "
					+(android.os.Build.VERSION.SDK_INT>29?"Using the built-in text editor in CodeAssist.<br/>Unfortunately this alternative is not available on devices running Android 11 or higher, because the CodeAssist developer has not yet considered building and publishing on Github an alternative APK file, which is intended to disable the storage restrictions that were added in Android 11.<br/>":"If you prefer to use the built-in text editor in CodeAssist, follow these steps:<ul><li>Open CodeAssist</li><li>Open the project \"ARTester_Example\"</li><li>Open side menu</li><li>Open the \"ARTester\" directory. You should not open the \"app\" directory, as java files that are intended to be compiled by ARTester will not be able to be compiled by CodeAssist.</li><li>Open the \"java\" directory and focus on the \"MainTest.java\" file, as this will be the equivalent of \"MainActivity.java\" (but for ARTester).</li></ul>")
				+"<b>Alternative 2</b>. If you prefer to use another text editor, follow the steps below:<ul><li>Open your text editor</li><li>Go to the directory that you established to store your projects</li><li>Go to the directory: ARTester_Example > ARTester > java, and focus on the \"MainTest.java\" file, as this will be the equivalent of \"MainActivity.java\" (but for ARTester).</li></ul><br/><b>Finally you will want to run the \"MainTest.java\" file</b>. To achieve this, all you have to do is open ARTester app and then click on the \"Example project\" item.")).show();
				java.util.zip.ZipInputStream zis=new java.util.zip.ZipInputStream(getAssets().open("ARTester_Example.zip"));
				java.util.zip.ZipEntry ze;while((ze=zis.getNextEntry())!=null){
					File f=new File(App.projectsDirectory, ze.getName());
					if(ze.isDirectory())f.mkdirs();else{
						FileOutputStream fos=new FileOutputStream(f);
						byte[] buf=new byte[1024];int i=0;while((i=zis.read(buf))>=0){fos.write(buf, 0, i);}
					fos.close();}
				} zis.close();save(4, dest.getPath());
			}catch(IOException ioe){ioe.printStackTrace();}break;
			case 4: et=new EditText(this);et.setHint("E.g.: /storage/emulated/0/AndroidProjects/MyApp/ARTester/config.txt\n NOTE: you can insert more than one file path, in such cases you must separate them with a \":\" character.");inputText(id, et, "enter file path");break;
			case 5:
				v.setEnabled(false);v.setOnClickListener(null);
				App.notice(this, false, "A process has been started that will write each logcat entry to a file, whose name will be \"logcat.txt\" and that file will be stored in the root of the AndroidProjects directory.\nIf you press the Back button, that process will be killed along with the process of this app.");
				java.util.concurrent.Executors.newSingleThreadExecutor().execute(()->{try{
					Runtime.getRuntime().exec("logcat -f "+new File(App.projectsDirectory, "/logcat.txt"));
				}catch(Exception ex){ex.printStackTrace();}});
			break;case 6: v.setEnabled(false);System.err.println("intentional error!");break;
			case 7:case 8: et=new EditText(this);et.setText(id==7?String.valueOf(ConsoleActivity.getFontSize()): App.prefs().getString("java version","1.8"));et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);inputText(id, et, "");break;
			case 9: SharedPreferences.Editor spe=App.prefs().edit();spe.putBoolean((String)((Switch)v).getHint(), ((Switch)v).isChecked());spe.apply();break;
			case 10: requestToSettingProjectsDirectory();break;
		}
	}
	private void inputText(int id, EditText et, String dialogTitle){
		new AlertDialog.Builder(this).setPositiveButton(android.R.string.ok, (DialogInterface di, int wich)->{
			save(id, et.getText().toString()); di.dismiss();
		}).setNegativeButton(android.R.string.cancel,null).setTitle(dialogTitle).setView(et).show();
	}
	private void save(int id, String userInput){
		SharedPreferences.Editor spe=App.prefs().edit();
		switch(id){
			case 4:
				boolean multiPaths=userInput.contains(File.pathSeparator);
				for(String path: userInput.split(File.pathSeparator)){
					if(new File(path).isDirectory()&&!new File((path=new File(path, "ARTester/config.txt").getPath())).exists()){if(multiPaths)continue;App.notice(this, true, "The specified directory does not contains a relative file as ARTester/config.txt");break;}
					else if(new File(path).isFile()&&!path.endsWith("/config.txt")){if(multiPaths)continue;App.notice(this, true, "The file is not named as config.txt");break;}
					else if(path.contains(" ")){App.notice(this, true, "Some of the directory names contain whitespace, which is not supported by ARTester. To fix this, you will need to rename such directories to remove whitespaces.");break;}
					else{projects.add(path);userInput=null;}
				}if(userInput==null){spe.putStringSet("projects",projects);refreshUI();}
			break;case 7: spe.putString("font size",userInput);((App)App.ref).killTestProcess();break;
			case 8: spe.putString("java version",userInput);break;
		} spe.apply();
	}

	@Override public void onCheckedChanged(CompoundButton cb, boolean isChecked){// used by the reloadOverlay related Switch from refreshUI()
		if(isChecked){if(cb!=null&&!android.provider.Settings.canDrawOverlays(this)){startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:"+getPackageName())),2);cb.setChecked(false);return;}}
		if(isChecked)App.notice(this,false, "From now, the ARTester icon will be displayed as a 48x48 (\"dp\" unit size) image floating (screen overlay) at the top left of the screen, above other apps, while ARTester is in the background. You can tap it to reload the last project you've run. Or you can also tap and hold when you want to kill the ARTester processes.");
		SharedPreferences.Editor spe=App.prefs().edit();spe.putBoolean("put reload overlay", (TestActivity.canPutReloadOverlay=isChecked));spe.apply();
	}

	private void requestToSettingProjectsDirectory(){
		LinearLayout ll=new LinearLayout(this,null,0, R.style.ll);
		TextView tv=new TextView(this);tv.setText("Enter the directory path where you want to store the projects you will run using ARTester."+(android.os.Build.VERSION.SDK_INT>29?"\n":" In case you want to be able to edit the source files of these projects using CodeAssist's built-in editor, you will need to enter the same directory path here that you set for your CodeAssist projects.\n"));ll.addView(tv);
		EditText et=new EditText(this);et.setText(App.projectsDirectory!=null?App.projectsDirectory.getPath(): new File(getExternalFilesDir(null).getParentFile().getParentFile().getParentFile().getParentFile(), "AndroidProjects").getPath()); ll.addView(et);
		new AlertDialog.Builder(this).setPositiveButton(android.R.string.ok, (DialogInterface di, int wich)->{
			SharedPreferences.Editor spe=App.prefs().edit();spe.putString("projects directory", et.getText().toString());spe.apply();
			di.dismiss();recreate();
		}).setCancelable(false).setTitle("Set projects directory").setView(ll).show();
	}
}