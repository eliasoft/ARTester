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

public class BaseActivity extends android.app.Activity{

	@Override protected void onCreate(android.os.Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(!BaseApplication.ref.loggerIsEnabled&&!BaseApplication.prefs().contains("do not alert when logger is disabled"))java.util.concurrent.Executors.newSingleThreadExecutor().execute(()->{
			try{Thread.sleep(500);
				if(!BaseApplication.ref.loggerIsEnabled)BaseApplication.showDialog(this, new android.app.AlertDialog.Builder(this).setTitle("ERROR!").setCancelable(false).setPositiveButton("KILL APP", (a, b)->System.exit(0)).setNegativeButton("disable this alert", (a, b)->{
					a.dismiss();android.content.SharedPreferences.Editor spe=BaseApplication.prefs().edit();spe.putBoolean("do not alert when logger is disabled", true);spe.apply();
				}).setMessage("This app cannot work because you have disabled logcat on this device.\n To solve this. Go to: Developer options > Logger Buffer Size, and set it to 64K.\n If you have done the above, you should tap on \"KILL APP\" and then open this app again. But if you can't get this alert to stop appearing, tap the \"disable this alert\" button."));
			}catch(InterruptedException ie){}
		});
	}
	@Override protected void onResume(){
		super.onResume();
		// if the user pauses the Activity while the Spinner3D is showing, and later the user resumes the Activity, the position offset of the Spinner3D will be moved incorrectly to an inapropiated position in the screen. The following line is intended to fix such behavior.
		if(Spinner3D.dialogInstance.get()!=null)Spinner3D.show(this,null);
	}
	@Override public void onConfigurationChanged(android.content.res.Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		// if the Spinner3D is showing, and the user rotates the device, the Spinner3D will be hidden automatically. The following line is intended to fix such behavior.
		if(Spinner3D.dialogInstance.get()!=null)Spinner3D.show(this,null);
	}
	@Override protected void onDestroy(){
		super.onDestroy();
		Spinner3D.hide(null);
	}
}