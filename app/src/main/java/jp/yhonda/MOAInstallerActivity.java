/*
    Copyright 2012, 2013 Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package jp.yhonda;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.widget.TextView;

public final class MOAInstallerActivity extends AppCompatActivity {
	File installedDir;
	File internalDir;
	TextView msg;
	long intStorageAvail;
	Activity me;
	public Activity parent;

	private long internalFlashAvail() {
		StatFs fs = new StatFs(internalDir.getAbsolutePath());
		return (fs.getAvailableBytes()/(1024*1024));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.moainstallerview);
		me = this;
		internalDir = this.getFilesDir();
		msg = (TextView) findViewById(R.id.checkedTextView1);

		if (!removeMaximaFiles()) {
			msg.setText(R.string.deletion_of_existing_files__failed);
			install(10);
			return;
		}

		intStorageAvail = Math.abs(internalFlashAvail() - 5);

		long minimumStorageSize = 85L;
		if (intStorageAvail < minimumStorageSize) {
			msg.setText(R.string.storage_insufficient_for_maxima_data);
			install(10);
			return;
		}
		installedDir = internalDir;
		install(0); // at the UnzipAsyncTask, install(1) and install(2)
		// will be called.
	}


	public void install(int stage) {
		// Where to Install
		// maxima, gnuplot, qepcad : nativeLibraryDir (extracted by the package installer)
		// additions, init.lisp : internalDir
		// maxima-5.X.0 : installedDir
		Intent data = null;
		Intent origIntent = this.getIntent();
		String vers = origIntent.getStringExtra("version");
		try {
			switch (stage) {
			case 0: {
				UnzipAsyncTask uzt = new UnzipAsyncTask(this);
				uzt.setParams(this.getAssets().open("additions.zip"),
						internalDir.getAbsolutePath(), getString(R.string.install_additions),
						"Additions installed");
				uzt.execute(0);
				break;
			}
			case 1: {
				if (!NativeBinaries.get(this, NativeBinaries.MAXIMA).exists()) {
					Log.v("MoA","maxima binary not found in nativeLibraryDir.");
					install(10);
					return;
				}
				String initlispPath = internalDir.getAbsolutePath()
						+ "/init.lisp";
				String firstLine = "(setq *maxima-dir* \""
						+ installedDir.getAbsolutePath() + "/maxima-" + vers
						+ "\")\n"
						+ "(defvar *moa-files-dir* \""
						+ internalDir.getAbsolutePath() + "/\")\n";
				copyFileFromAssetsToLocal("init.lisp", initlispPath, firstLine);
				UnzipAsyncTask uzt = new UnzipAsyncTask(this);
				uzt.setParams(this.getAssets().open("maxima-" + vers + ".zip"),
						installedDir.getAbsolutePath(), getString(R.string.install_maxima_data),
						"maxima data installed");
				uzt.execute(1);
				break;
			}
			case 2: {
				data = new Intent();
				data.putExtra("sender", "MOAInstallerActivity");
				setResult(RESULT_OK, data);

				me.finish();
				break;
			}
			case 10: {// Error indicated
				data = new Intent();
				data.putExtra("sender", "MOAInstallerActivity");
				setResult(RESULT_CANCELED, data);

				me.finish();
				break;
			}
			default:
				break;
			}
		} catch (IOException e1) {
			Log.d("MoA", "exception8");
			e1.printStackTrace();
			me.finish();
		} catch (Exception e) {
			Log.d("MoA", "exception9");
			e.printStackTrace();
			me.finish();
		}
	}

	private void copyFileFromAssetsToLocal(String src, String dest, String line)
			throws Exception {
		InputStream fileInputStream = getApplicationContext().getAssets().open(
				src);
		BufferedOutputStream buf = new BufferedOutputStream(
				new FileOutputStream(dest));
		int read;
		byte[] buffer = new byte[4096 * 128];
		buf.write(line.getBytes());
		while ((read = fileInputStream.read(buffer)) > 0) {
			buf.write(buffer, 0, read);
		}
		buf.close();
		fileInputStream.close();
	}

	private boolean removeMaximaFiles() {
		MaximaVersion prevVers = new MaximaVersion();
		prevVers.loadVersFromSharedPrefs(this);
		String maximaDirName = "/maxima-" + prevVers.versionString();
		String maximaDirPath = null;
		if ((new File(internalDir.getAbsolutePath() + maximaDirName)).exists()) {
			maximaDirPath = internalDir.getAbsolutePath() + maximaDirName;
		} else {
			maximaDirPath = null;
		}
		String filelist[] = { internalDir.getAbsolutePath() + "/init.lisp",
				internalDir.getAbsolutePath() + "/x86",
				internalDir.getAbsolutePath() + "/maxima",
				internalDir.getAbsolutePath() + "/maxima.x86",
				internalDir.getAbsolutePath() + "/maxima.pie",
				internalDir.getAbsolutePath() + "/maxima.x86.pie",
				internalDir.getAbsolutePath() + "/additions",
				internalDir.getAbsolutePath() + "/maxima-userdir", maximaDirPath };
		for (int i = 0; i < filelist.length; i++) {
			if ((filelist[i] != null) && (new File(filelist[i])).exists()) {
				boolean res=recursiveRemoveFileDirectory(filelist[i]);
				if (res=false) return false;
			}
		}
		return true;
	}

	private boolean recursiveRemoveFileDirectory(String filename) {
		File file=new File(filename);
		if (!file.exists()) {
			return true;
		}
		if (file.isDirectory()) {
			for (File node: file.listFiles()) {
				boolean res=recursiveRemoveFileDirectory(node.getAbsolutePath());
				if (res=false) return false;
			}
		}
		return file.delete();
	}

}
