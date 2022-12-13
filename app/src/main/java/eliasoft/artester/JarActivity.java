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
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.tools.*;
import eliasoft.common.*;

public class JarActivity extends BaseActivity implements Runnable, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SearchView.OnQueryTextListener, View.OnClickListener{
	private LinearLayout mainUI;
	private SearchView searchBar;
	private HashMap<String, File> jars;
	private File openedJarFile;
	private File openedJarPropertiesFile;
	private ArrayList<? extends ZipEntry> openedJarEntries;
	private String openedJarLastDirPath;
	private ArrayList<String> openClassHistory;
	private ArrayList<LinearLayout> openClassBackStack;
	private SpannableStringBuilder openedClassProcessedText;
	private final int typeColor=0xFF7FDBFF, keywordColor=0xFFFFDC00;

	@Override protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mainUI=new LinearLayout(this,null,0, R.style.ll);mainUI.setBackgroundColor(0xFF000000);
		Button b=new Button(this,null, 0, R.style.btn);b.setText("Open class file from arbitrary path");b.setOnClickListener(this);mainUI.addView(b, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		searchBar=new SearchView(this);searchBar.setVisibility(View.GONE);searchBar.setIconifiedByDefault(false);searchBar.setOnQueryTextListener(this);
		searchBar.setInputType(searchBar.getInputType()|InputType.TYPE_TEXT_VARIATION_URI);searchBar.setImeOptions(searchBar.getImeOptions()|EditorInfo.IME_FLAG_FORCE_ASCII|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING|EditorInfo.IME_FLAG_NO_FULLSCREEN);
		mainUI.addView(searchBar, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		ListView lv=new ListView(this);lv.setOnItemClickListener(this);lv.setOnItemLongClickListener(this);lv.setFocusable(true);lv.setFocusableInTouchMode(true);mainUI.addView(lv, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		setContentView(mainUI);
		((App)App.ref).initProjectsDirectory();
		openClassHistory=new ArrayList<>();openClassBackStack=new ArrayList<>();
		jars=new HashMap<>();
		for(File cpf: App.classpathsDirectory.listFiles()){
			if(cpf.getName().endsWith(".jar"))jars.put(cpf.getPath().substring(cpf.getPath().lastIndexOf(File.separator,(cpf.getPath().length()-cpf.getName().length())-2)+1), cpf);
		}
		HashSet<String> projects=new HashSet<String>(App.prefs().getStringSet("projects", new HashSet<String>()));
		for(String p: projects){
			try{
				BufferedReader projectReader=new BufferedReader(new FileReader(p));
				String l; File sourceDir; while((l=projectReader.readLine())!=null){
					if(l.length()==0||l.charAt(0)=='#'||l.charAt(0)=='"'||l.contains(" "))continue;
					else if(l.charAt(0)==File.separatorChar)sourceDir=new File(l);
					else{sourceDir=new File(new File(p).getParentFile(), l);try{sourceDir=new File(sourceDir.getCanonicalPath());}catch(IOException ioe){sourceDir=null;App.l("Error: sourceDir.getCanonicalPath() "+l);}}
					if(sourceDir==null ||!sourceDir.canRead())continue;
					DirectoryTraverser.forEachFile(sourceDir, (file, loop)->{
						if(file.canRead()&&file.getName().endsWith(".jar"))
						jars.put(file.getPath().substring(file.getPath().lastIndexOf(File.separator,(file.getPath().length()-file.getName().length())-2)+1), file);
					});
				} projectReader.close();
			}catch(Exception ioe){}
		}
		JarActivity.MyArrayAdapter ad=new JarActivity.MyArrayAdapter(new ArrayList<String>(jars.keySet()));
		ad.sort(String::compareToIgnoreCase);lv.setAdapter(ad);
	}
	@Override public void onBackPressed(){
		if(openClassBackStack!=null&&!openClassBackStack.isEmpty()&&openClassBackStack.remove(0)!=null&&!openClassBackStack.isEmpty()){
			Spinner3D.show(this, this);return;
		}
		if(openedClassProcessedText!=null){openedClassProcessedText=null;setContentView(mainUI);((ListView)mainUI.getChildAt(2)).requestFocus();return;}
		if(openedJarLastDirPath==null ||openedJarLastDirPath.length()==0){super.onBackPressed();return;}
		openedJarLastDirPath=openedJarLastDirPath.substring(0,openedJarLastDirPath.lastIndexOf(File.separator, openedJarLastDirPath.length()-2)+1);
		openLastDirPath();
	}
	@Override public void run(){// used from onBackPressed() line 2.
		openClassHistory.add(openClassBackStack.size(), openClassHistory.remove(0));
		LinearLayout ll=openClassBackStack.get(0);
		setContentView(ll);
		ll.getChildAt(0).requestFocus();
		ll.post(()->Spinner3D.hide(null));
	}

	private void openLastDirPath(){
		String entryPath, entryName;// "s" is for prevent duplicates.
		ArrayList<String> arr=new ArrayList<>();
		for(ZipEntry ze: openedJarEntries){// this loop only gets directory entries.
			entryPath=ze.getName();
			if(!entryPath.startsWith(openedJarLastDirPath) ||entryPath.indexOf(File.separator, openedJarLastDirPath.length())<1)continue;// Skip entries that are not inside the target directory || Skip entry corresponding to the target directory.
			entryName=entryPath.substring(openedJarLastDirPath.length(), entryPath.indexOf(File.separator, openedJarLastDirPath.length()));
			if(!arr.contains(entryName+File.separator))arr.add(entryName+File.separator);
		}
		arr.sort(String::compareToIgnoreCase);
		JarActivity.MyArrayAdapter ad=new JarActivity.MyArrayAdapter(arr);
		arr=new ArrayList<>();
		for(ZipEntry ze: openedJarEntries){// this loop only gets .class files entries.
			entryPath=ze.getName();
			if(!ze.isDirectory() &&entryPath.startsWith(openedJarLastDirPath) &&entryPath.indexOf(File.separator, openedJarLastDirPath.length())<0 &&entryPath.endsWith(".class"))
			arr.add(entryPath.substring(openedJarLastDirPath.length()).replace(".class","").replace("$","."));
		}
		arr.sort(String::compareToIgnoreCase);
		ad.addAll(arr);((ListView)mainUI.getChildAt(2)).setAdapter(ad);
	}
	@Override public void onItemClick(AdapterView<?> av, View v, int pos, long id){
		String item=((TextView)v).getText().toString();
		if(openedJarEntries==null){try{
			((Button)mainUI.getChildAt(0)).setText("History of viewed class files");((Button)mainUI.getChildAt(0)).setTextColor(typeColor);searchBar.setVisibility(View.VISIBLE);
			ZipFile zf=new ZipFile((openedJarFile=jars.get(item)));
			openedJarEntries=Collections.list(zf.entries());
			zf.close(); openedJarLastDirPath=item="";
			openedJarPropertiesFile=new File(openedJarFile.getPath()+".properties");if(!openedJarPropertiesFile.canRead())openedJarPropertiesFile=null;
		}catch(IOException ie){ie.printStackTrace();}
		}else if(!item.endsWith(File.separator)){
			String s=item.contains("/")?/*onQueryTextSubmit()*/item: openedJarLastDirPath+item;
			openClass(s);return;
		}else openedJarLastDirPath+=item;
		openLastDirPath();
	}
	@Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id){
		return openedJarPropertiesFile!=null&&goToOnlineJavadoc(openedJarLastDirPath+((TextView)v).getText().toString());
	}
	private boolean goToOnlineJavadoc(String s){
		Properties p=new Properties();try{p.load(new FileInputStream(openedJarPropertiesFile));}catch(IOException ioe){}
		if(s.endsWith(File.separator))s+="package-summary";
		startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(p.getProperty("link_prefix")+s+".html")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	return true;}

	private String previousQuery="";// this field is used to detect when the user paste text in the search bar. In such case, calls onQueryTextSubmit() inmediately.
	@Override public boolean onQueryTextChange(String query){
		String s="";
		if(query.length()==0)openLastDirPath();else{
			if(!query.matches("[a-zA-Z0-9./]+"))searchBar.setQuery(query.replaceAll("[^a-zA-Z0-9./]",""),false);
			else if(query.contains(".")){
				for(char ch: query.toCharArray()){
					if(Character.isUpperCase(ch))break;
					else s+=ch=='.'?"/":ch;
				}
				s=s+query.substring(s.length());
				if(!s.equals(query))searchBar.setQuery(s, false);
			}
		}
		if(previousQuery.length()<(s=searchBar.getQuery().toString()).length()-10)onQueryTextSubmit(s);
		previousQuery=s;
	return true;}
	@Override public boolean onQueryTextSubmit(String query){
		searchBar.clearFocus();
		query=query.toLowerCase().replace(".","$");
		ArrayList<String> arr=new ArrayList<>();String entryPath;
		for(ZipEntry ze: openedJarEntries){
			entryPath=ze.getName();
			if(!ze.isDirectory() &&entryPath.toLowerCase().contains(query) &&!entryPath.toLowerCase().endsWith(File.separator+query+".class"))
			arr.add(entryPath.replace(".class","").replace("$","."));
		}
		arr.sort(String::compareToIgnoreCase);
		for(ZipEntry ze: openedJarEntries){
			entryPath=ze.getName();
			if(!ze.isDirectory() &&entryPath.toLowerCase().endsWith(File.separator+query+".class"))
			arr.add(0, entryPath.replace(".class","").replace("$","."));
		}
		((ListView)mainUI.getChildAt(2)).setAdapter(new JarActivity.MyArrayAdapter(arr));
		return true;
	}

	@Override public void onClick(View v){// from the button located at the top left on the main UI.
		android.app.AlertDialog.Builder db=new android.app.AlertDialog.Builder(this);
		if(openedJarEntries==null){
			EditText et=new EditText(this);et.setHint("E.g.: /storage/emulated/0/example/a.class");
			db.setPositiveButton(android.R.string.ok, (DialogInterface di, int wich)->{
				String s=et.getText().toString();if(s.endsWith(".class")){openClass(s);di.dismiss();}
			}).setNegativeButton(android.R.string.cancel,null).setTitle("enter file path").setView(et);
		}else db.setTitle(openClassHistory.isEmpty()?"The list is empty!":null).setItems(openClassHistory.toArray(new String[openClassHistory.size()]), (di, item)->openClass(openClassHistory.get(item)));
		db.show();
	}

	private class MyArrayAdapter extends ArrayAdapter<String>{
		MyArrayAdapter(List<String> l){super(JarActivity.this,0,l);}
		@Override public View getView(int position, View convertView, ViewGroup parent){
			TextView tv;
			if(convertView==null){
				convertView=tv=new TextView(getContext());
				int i=ConsoleActivity.getFontSize();
				tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,  i);
				tv.setPadding(0, i/=2, 0, i);
			}else tv=(TextView)convertView;
			tv.setText(getItem(position));
			return tv;
		}
	}

	private void openClass(String s){
		Spinner3D.show(this, null);
		if(openClassHistory.contains(s))openClassHistory.remove(s);openClassHistory.add(0,s);
		new JarActivity.OpenClassTask().execute(s);
	}
	@SuppressWarnings("deprecation")
	private class OpenClassTask extends AsyncTask<String, Void, Void> implements View.OnClickListener{
		@Override protected Void doInBackground(String... s){
			String clazz=s[0].endsWith(".class")?s[0]: s[0].replace(".","$")+".class";
			String jar=s[0].endsWith(".class")?"": "jar:file://"+openedJarFile.getPath()+"!/";
			try{parse(jar+clazz, clazz);}catch(Exception ex){ex.printStackTrace();}
		return null;}
		@Override protected void onPostExecute(Void result){
			LinearLayout ll=new LinearLayout(JarActivity.this,null,0, R.style.ll);ll.setBackgroundColor(0xFF000000);
			if(openedJarPropertiesFile!=null){Button b=new Button(JarActivity.this,null, 0, R.style.btn);b.setText("Go to online javadoc for this class");b.setTextColor(keywordColor);b.setOnClickListener(this);ll.addView(b, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);}
			ScrollView sv=new ScrollView(JarActivity.this,null,0, R.style.match_parent);ll.addView(sv);
			TextView tv=new TextView(JarActivity.this){
				@Override protected void onSelectionChanged(int selStart, int selEnd){
					if(selStart==selEnd)return;
					CharacterStyle[] spans=((SpannableString)getText()).getSpans(selStart,selEnd, CharacterStyle.class);
					if(spans.length==1&&spans[0] instanceof ClickableSpan){((ClickableSpan)spans[0]).onClick(this);Selection.removeSelection((SpannableString)getText());}
				}
			};tv.setTextIsSelectable(true);tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, ConsoleActivity.getFontSize());tv.setText(openedClassProcessedText);
			HorizontalScrollView hsv=new HorizontalScrollView(JarActivity.this);hsv.addView(tv, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);sv.addView(hsv, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			setContentView(ll);
			if(openClassBackStack!=null)openClassBackStack.add(0,ll);// openClassBackStack is null if you are opening class file from arbitrary path.
			tv.post(()->Spinner3D.hide(null));
		}
		@Override public void onClick(View v){// from the button defined inside onPostExecute()
			goToOnlineJavadoc(openClassHistory.get(0));
		}
	}

	private void parse(String classInJar, String clazz) throws Exception{
		DiagnosticCollector<JavaFileObject> dc=new DiagnosticCollector<>();
		StandardJavaFileManager sfm=com.sun.tools.javac.api.JavacTool.create().getStandardFileManager(dc, null, null);
		ArrayList<String> options=new ArrayList<>();options.add("-constants");options.add("-protected");
		StringWriter sw=new StringWriter();
		com.sun.tools.javap.JavapTask jt=new com.sun.tools.javap.JavapTask(sw, sfm, dc, options, Collections.singletonList(classInJar));
		java.lang.reflect.Field f=jt.getClass().getDeclaredField("options");f.setAccessible(true);com.sun.tools.javap.Options o=(com.sun.tools.javap.Options)f.get(jt);o.showAllAttrs=true;
		jt.call();sfm.close();String sws=sw.toString();
		if(sws.length()<5/*ERROR*/)throw new Error(dc.getDiagnostics().toString());
		clazz=clazz.substring(0,clazz.indexOf(".")).replace(File.separator,".");
		String s=clazz;try{s=clazz.substring(clazz.lastIndexOf(".")+1);}catch(Exception ex){}
		openedClassProcessedText=new SpannableStringBuilder();
		boolean b=false;
		String[] sp=sws.replaceAll(clazz.replace("$","[$]")+"(?!\\$)", s.replace(/*avoid a rare behavior*/"$",".")).replaceAll("java.lang.(?=([A-Z](?!\\w+[\\$])))","").replace("$",".").split("\n");
		String ss=sp[1];int j=ss.indexOf(" extends ");if(j<0)j=ss.indexOf(" implements ");if(j<0)j=ss.length()-2;
		String[] sj=new String[2];sj[1]=ss.substring(j+1).replaceAll(",(?! )",", ");s=ss.substring(0,j);
		sj[0]=s.substring((j=s.contains("<")?s.lastIndexOf(" ",s.indexOf("<")):s.lastIndexOf(" ")+1));
		if(openClassBackStack!=null){append(1,"package ");append(0,clazz.substring(0,clazz.lastIndexOf("."))+";\n");}
		append(1,s.substring(0,j));append(2,sj[0]+" ");
		sj=sj[1].split(" ");for(int i=0;i<sj.length;i++){
			if(i==sj.length-1)append(0,"{\n\n");
			else{append(sj[i].matches("extends|implements")?1:2, sj[i]);append(0, " ");}
		}
		for(int i=2;i<sp.length-1;i++){
			sj=null;
			if(sp[i].length()==0){append(0,"\n");continue;}
			else if(sp[i].charAt(2)==' ')continue;
			ss=sp[i].trim();boolean isMethod=ss.contains("(");
			j=isMethod?ss.indexOf("("):(ss.contains("=")?ss.indexOf(" ="):ss.indexOf(";"));
			sj=new String[3];sj[2]=isMethod?ss.substring(j+1, ss.indexOf(")")):ss.substring(j);
			s=ss.substring(0,j);sj[1]=s.substring((j=s.lastIndexOf(" "))+1);s=s.substring(0,j);
			sj[0]=s.substring((j=s.contains("<")?s.lastIndexOf(" ",s.indexOf("<")):s.lastIndexOf(" ")+1));s=s.substring(0,j);
			for(int k=i+1;k<sp.length;k++){
				if(sp[k].length()==0)break;else if(!sp[k].matches("    Runtime(Inv|V)isibleAnnotations:"))continue;
				ss="";k++;
				while(k<sp.length&&sp[k].length()>5&&sp[k].charAt(5)==' '){if(sp[k].charAt(7)==' ')ss+=sp[k].substring(8, sp[k].length());else ss+="\n  @";k++;}
				append(3, ss.substring(1)+"\n");break;
			}
			append(1,"  "+s);
			if(!isMethod){
				append(2,sj[0]);append(0,"\n  "+sj[1]);append(0,sj[2]);
				if(sj[0].equals("int")){try{ss=Integer.toHexString(Integer.parseInt(sj[2].substring(sj[2].lastIndexOf(" ")+1,sj[2].length()-1))).toUpperCase();while(ss.length()<8){ss="0"+ss;}append(3,"//0x"+ss);}catch(NumberFormatException nfe){}}
				else if(sj[0].equals("long")){try{ss=Long.toHexString(Long.parseLong(sj[2].substring(sj[2].lastIndexOf(" ")+1,sj[2].length()-1))).toUpperCase();while(ss.length()<16){ss="0"+ss;}append(3,"//0x"+ss);}catch(NumberFormatException nfe){}}
				append(0,"\n");
			continue;}
			else{if(s.length()==0)append(1,sj[0]+" ");else{append(2,sj[0]);append(0,"\n  ");}append(s.length()==0?2:0,sj[1]);append(0," (");}
			ArrayList<String> lvt=new ArrayList<>();
			b=false;ss="";j=0;for(int k=0;k<sj[2].length();k++){
				char ch=sj[2].charAt(k);ss+=ch;
				if(ch=='<'){j++;b=true;}else if(ch=='>'){j--;if(j==0)b=false;}
				if(!b&&(ch==','||k==sj[2].length()-1)){lvt.add(ss);ss="";k++;continue;}
			}
			sj=lvt.toArray(new String[lvt.size()]);
			b=false;lvt=new ArrayList<>(sj.length);
			if(sj.length>0){for(int k=i+1;k<sp.length;k++){
				if(sp[k].length()==0)break;
				else if(b){if(sp[k].length()>7&&sp[k].charAt(7)==' ')lvt.add(sp[k]);else break;}
				else if(sp[k].equals("      LocalVariableTable:")){k+=s.contains("static")?1:2;b=true;}
			} b=false;}
			String[] rpa=new String[sj.length];
			if(sj.length>0){for(int k=i+1;k<sp.length;k++){
				if(sp[k].length()==0)break;else if(b){
					if(sp[k].length()>6&&sp[k].charAt(6)=='p'){// 'p' means "parameter"
						j=Integer.parseInt(sp[k].substring(16, sp[k].length()-1));k++;
						s="";while(k<sp.length&&sp[k].length()>7&&sp[k].charAt(7)==' '){s+="@"+sp[k+=1].substring(10, sp[k].length())+" ";k++;}
						k--;rpa[j]=rpa[j]==null?s:s+rpa[j];
					}else break;
				}else if(sp[k].matches("    Runtime(Inv|V)isibleParameterAnnotations:"))b=true;
			} b=false;}
			if(sj.length==0)append(0,")");
			else{for(int k=0; k<sj.length;k++){
				if(rpa[k]!=null)append(3, rpa[k]);
				append(2, sj[k].replace(",",""));
				if(lvt.isEmpty())append(0," arg"+(k+1));
				else{s=lvt.get(k);
					j=0;while(!Character.isLetter(s.charAt(++j)));
					append(0," "+s.substring(j,s.indexOf(" ",j)));
				} append(0,k<sj.length-1?", ":")");
			}}
			for(int k=i+1;k<sp.length;k++){
				if(sp[k].length()==0)break;else if(!sp[k].matches("    Exceptions:"))continue;
				k+=1;append(1," throws ");
				s="";while(k<sp.length&&sp[k].length()>5&&sp[k].charAt(5)==' '){if(sp[k].contains(":"))break;s+=sp[k].substring(13, sp[k].length())+", ";k++;}
				append(2,s.substring(0,s.length()-2));break;
			}
			append(0," {}\n");
		}
	}
	private void append(int color, String s){
		if(color==2){
			String ss="";for(int i=0;i<s.length();i++){char ch=s.charAt(i);
				if(String.valueOf(ch).matches("\\w")||(s.charAt(i)=='.'&&i+1<s.length()&&s.charAt(i+1)!='.')){ss+=ch;if(i<s.length()-1)continue;}
				final String type=ss; openedClassProcessedText.append(ss, openedJarEntries==null ||ss.matches("(\\w+)|([A-Z].+)")?new ForegroundColorSpan(ss.matches("extends|super")?keywordColor: typeColor): new ClickableSpan(){
					@Override public void onClick(View v){
						String ss="";for(int i=0;i<type.length();i++){
							if(type.charAt(i)=='.'){ss+="/";if(Character.isUpperCase(type.charAt(i+1))){ss+=type.substring(i+1,type.length());break;}}else ss+=type.charAt(i);
						} openClass(ss);
					}
					@Override public void updateDrawState(TextPaint tp){
						super.updateDrawState(tp);
						tp.setColor(typeColor);
					}
				}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ss="";if(i<s.length()-1||!String.valueOf(ch).matches("\\w"))append(0,String.valueOf(ch));
			} return;
		}else if(color==0)color=0xFFFFFFFF;else if(color==1)color=keywordColor;else if(color==3)color=0xFFAAECB2;
		openedClassProcessedText.append(s, new ForegroundColorSpan(color), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}