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
import android.net.Uri;
import android.os.Bundle;

public class TestProcessDataProvider extends android.content.ContentProvider{
	@Override public Bundle call(String method, String arg, Bundle extras){
		Bundle b=new Bundle();
		if(arg.equals("pid"))b.putInt("pid",android.os.Process.myPid());
		else if(arg.equals("logs"))b.putStringArrayList(arg,App.logs);
		return b;
	}
	@Override public boolean onCreate(){return true;}
	@Override public android.database.Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){return null;}
	@Override public String getType(Uri uri){return null;}
	@Override public int delete(Uri uri, String selection, String[] selectionArgs){return 0;}
	@Override public Uri insert(Uri uri, android.content.ContentValues values){return null;}
	@Override public int update(Uri uri, android.content.ContentValues values, String selection, String[] selectionArgs){return 0;}
}
