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
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package jp.yhonda;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Installs the data the native programs need into the files directory:
 * additions.zip (gnuplot JavaScript, QEPCAD data, qepmax), init.lisp and
 * maxima-<version>.zip. The programs themselves are installed by the package
 * installer into nativeLibraryDir.
 */
public final class MOAInstallerActivity extends AppCompatActivity {
	private static final String TAG = "MoA";
	private static final long REQUIRED_MB = 85;

	/** Files and directories left by earlier versions (maxima-x.y.z dirs are found separately). */
	private static final String[] OLD_FILES = { "init.lisp", "additions", "maxima-userdir",
			"x86", "maxima", "maxima.x86", "maxima.pie", "maxima.x86.pie" };

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private TextView msg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SystemBars.setUp(this);
		// Leaving half-way would leave Maxima unusable; installing takes seconds.
		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
			}
		});
		setContentView(R.layout.moainstallerview);
		msg = findViewById(R.id.installStatus);
		final String version = getIntent().getStringExtra("version");
		executor.execute(() -> {
            final boolean ok = install(version);
            runOnUiThread(() -> {
                Intent data = new Intent();
                data.putExtra("sender", "MOAInstallerActivity");
                setResult(ok ? RESULT_OK : RESULT_CANCELED, data);
                finish();
            });
        });
	}

	@Override
	protected void onDestroy() {
		executor.shutdown();
		super.onDestroy();
	}

	/** Runs on the background thread. */
	private boolean install(String version) {
		File filesDir = getFilesDir();
		if (!removeOldInstall(filesDir)) {
			showStatus(R.string.deletion_of_existing_files__failed);
			return false;
		}
		if (new StatFs(filesDir.getAbsolutePath()).getAvailableBytes() / (1024 * 1024) < REQUIRED_MB) {
			showStatus(R.string.storage_insufficient_for_maxima_data);
			return false;
		}
		if (!NativeBinaries.get(this, NativeBinaries.MAXIMA).exists()) {
			Log.e(TAG, "maxima binary not found in nativeLibraryDir");
			return false;
		}
		try {
			showStatus(R.string.install_additions);
			ZipExtractor.extract(getAssets().open("additions.zip"), filesDir);
			writeInitLisp(filesDir, version);
			showStatus(R.string.install_maxima_data);
			ZipExtractor.extract(getAssets().open("maxima-" + version + ".zip"), filesDir);
			return true;
		} catch (IOException e) {
			Log.e(TAG, "installation failed", e);
			return false;
		}
	}

	private void showStatus(final int resId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				msg.setText(resId);
			}
		});
	}

	/** init.lisp from the assets, preceded by the paths it needs. */
	private void writeInitLisp(File filesDir, String version) throws IOException {
		String header = "(setq *maxima-dir* \"" + filesDir.getAbsolutePath() + "/maxima-" + version + "\")\n"
				+ "(defvar *moa-files-dir* \"" + filesDir.getAbsolutePath() + "/\")\n";
		try (InputStream in = getAssets().open("init.lisp");
				OutputStream out = new FileOutputStream(new File(filesDir, "init.lisp"))) {
			out.write(header.getBytes(StandardCharsets.UTF_8));
			byte[] buffer = new byte[64 * 1024];
			int n;
			while ((n = in.read(buffer)) != -1) {
				out.write(buffer, 0, n);
			}
		}
	}

	private static boolean removeOldInstall(File filesDir) {
		for (String name : OLD_FILES) {
			if (!ZipExtractor.deleteRecursively(new File(filesDir, name))) {
				return false;
			}
		}
		File[] entries = filesDir.listFiles();
		if (entries != null) {
			for (File f : entries) {
				if (f.isDirectory() && f.getName().matches("maxima-\\d+\\.\\d+\\.\\d+")
						&& !ZipExtractor.deleteRecursively(f)) {
					return false;
				}
			}
		}
		return true;
	}
}
