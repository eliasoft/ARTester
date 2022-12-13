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
import java.io.File;

/*
* This is a perfect solution to get and manipulate all files inside a given directory, this also includes files inside subdirectories. Also, this solution provides an special feature that is not provided in other similar solutions.
* This solution can be used as a replacement for the strategy of filling in an ArrayList<File> before starting to process the files. so memory usage will decrease considerably, in case you want to traverse all storage volumes.
* This solution can also be used as a replacement for using the walk( ... ) method of the java.nio.file.Files class, since that is not supported on devices running API level lower than 26.
* I am aware that there are some external libraries that offer a similar solution, but such libraries are full of other things that my app doesn't need, and using them would mean increasing the build times for my project.
*
* Three code examples are provided below to help understand this solution and ilustrate the special feature.
*
File internalStorageDirectory=new File("/storage/emulated/0");

EXAMPLE 1: Process all files:
	DirectoryTraverser.forEachFile(internalStorageDirectory, (file, loop)->{
		// do something with the file object.
	});

EXAMPLE 2: Process all files and terminate the traversing if a particular file is found:
	File commandFile=null;
	DirectoryTraverser.forEachFile(internalStorageDirectory, (file, loop)->{
		if(file.getName().equals("command.txt")){
			commandFile=file;
			loop.terminate();
		}
	});
	if(commandFile!=null)doSomething();

EXAMPLE 3: Add all photos to a list, excluding files within folders containing a ".nomedia" file:
	ArrayList<File> photos=new ArrayList<>();
	DirectoryTraverser.forEachFile(internalStorageDirectory, (file, loop)->{
		if(file.getName().equals(".nomedia"))loop.goBackAndContinue();
		else if(file.getName().endsWith(".jpg"))photos.add(file);
	});

*/
@FunctionalInterface public interface DirectoryTraverser{
	
	public abstract void accept(File file, DirectoryTraverser.Loop loop) throws Exception;

	public static void forEachFile(File parent, DirectoryTraverser dt) throws Exception{
		forEachFileInternal(parent, dt, new DirectoryTraverser.Loop());
	}

	static void forEachFileInternal(File parent, DirectoryTraverser dt, DirectoryTraverser.Loop l) throws Exception{
		for(File f: parent.listFiles()){
			if(!f.canRead())continue;
			if(f.isDirectory())forEachFileInternal(f, dt, l);
			else dt.accept(f, l);
			if(l.terminated)return;
			else if(l.goBack){l.goBack=false;break;}
		}
	}
	public static class Loop{
		boolean terminated;
		boolean goBack;
		public void terminate(){terminated=true;}
		public void goBackAndContinue(){goBack=true;}
	}
}