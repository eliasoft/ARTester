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
import java.io.*;

public class ConsoleActivity extends BaseActivity{

	static int getFontSize(){return Integer.parseInt(App.prefs().getString("font size"/*setting inside MainActivity*/, "20"));}
	protected android.widget.TextView consoleTextView;

	@SuppressWarnings("deprecation")
	@Override protected void onCreate(android.os.Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		consoleTextView=(android.widget.TextView)findViewById(R.id.consoleTextView);
		consoleTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, getFontSize());
		consoleTextView.setMovementMethod(null);// disable auto scroll, this is important and should called after setContentView()
		System.setOut(new PrintStream(new ConsoleActivity.SystemOutToConsoleStream()));
		try{for(String msg: getIntent().getStringArrayListExtra("diagnostics")){System.out.println(msg);}}catch(NullPointerException npe){}
		java.util.concurrent.atomic.AtomicBoolean uiThreadIsBlocked=new java.util.concurrent.atomic.AtomicBoolean(true);
		consoleTextView.post(()->uiThreadIsBlocked.set(false));
		java.util.concurrent.Executors.newSingleThreadExecutor().execute(()->{
			try{Thread.sleep(3000);
				if(!uiThreadIsBlocked.get())return;
				byte[] bytes=consoleTextView.getText().toString().getBytes();
				if(bytes.length<16/*this could happen if the operating system is overloaded enough to allow 3 seconds to elapse before displaying the UI.*/)return;
				String outPath=new File(App.projectsDirectory, "console-text.txt").getPath();
				FileOutputStream fos=new FileOutputStream(outPath);fos.write(bytes);fos.close();
				System.err.print("For more details, read the content of the file: "+outPath);
			}catch(Exception ex){}
		});
	}

	private class SystemOutToConsoleStream extends OutputStream{
		@Override public void write(int i) throws IOException{print(String.valueOf(i));}
		@Override public void write(byte[] b) throws IOException{print(new String(b));}
		@Override public void write(byte[] b, int off, int len) throws IOException{print(new String(b, off, len));}
		private void print(String s){
			String ss=s.substring(0,s.length()>10?10:s.length()).toLowerCase();
			int color=0xFFFFFFFF;if(ss.startsWith("error"))color=0xFFFFDDDD;else if(ss.startsWith("warning"))color=0xFFFFFFCC;else if(ss.startsWith("note"))color=0xFF7FDBFF;
			consoleTextView.append(new android.text.SpannableStringBuilder().append(s, new android.text.style.ForegroundColorSpan(color), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE));
		}
	}

	public void copyTextToClipboard(android.view.View v){
		((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText(null, consoleTextView.getText().toString()));
		App.notice(this, false,"The text in the console TextView has been copied to the clipboard");
	}
}