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

public class BaseActivity extends eliasoft.common.BaseActivity{
	@Override protected void onResume(){
		super.onResume();
		sendBroadcast(new android.content.Intent(App.ToggleReloadOverlay.class.getName()));
	}
	@Override protected void onPause(){
		super.onPause();
		sendBroadcast(new android.content.Intent(App.ToggleReloadOverlay.class.getName()).putExtra("show",true));
	}
}