/*
    Copyright 2012, 2013, 2014, 2015, 2016, 2017 Yasuaki Honda (yasuaki.honda@gmail.com)
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaximaOnAndroidActivity extends AppCompatActivity implements
		TextView.OnEditorActionListener, OnTouchListener {
	boolean inited=false; /* expSize initialize is done or not */
	String[] mcmdArray = null; /* manual example input will be stored. */
	int mcmdArrayIndex = 0;
	Activity thisActivity = this;
	String maximaURL = null;

	String manjp = "file:///android_asset/maxima-doc/ja/index.html";
	String manen = "file:///android_asset/maxima-doc/en/index.html";
	String manURL;
	boolean manLangChanged = true;
	boolean allExampleFinished = false;
	Semaphore sem = new Semaphore(1);
	MultiAutoCompleteTextView editText;
	Button enterB;
	WebView webview;
	ScrollView scview;
	CommandExec maximaProccess;
	File internalDir;
	MaximaVersion mvers = new MaximaVersion(5, 50, 0);

	private static final int READ_REQUEST_CODE = 42;
	private static final long SCRIPT_FILE_MAX_SIZE = 10000000L;

	/* Settings changed in MoAPreferenceActivity; applied in onResume(). */
	private final Set<String> changedSettings = new HashSet<String>();
	private final SharedPreferences.OnSharedPreferenceChangeListener settingsListener =
			new SharedPreferences.OnSharedPreferenceChangeListener() {
				@Override
				public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
					changedSettings.add(key);
				}
			};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("MoA", "onCreate()");
		super.onCreate(savedInstanceState);
		SystemBars.setUp(this);
		// Back does not leave the console (Maxima would be stopped); quit is in the menu.
		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				Toast.makeText(MaximaOnAndroidActivity.this, getString(R.string.quit_hint),
						Toast.LENGTH_LONG).show();
			}
		});

		maximaURL = "file:///android_asset/maxima_svg.html";

		PreferenceManager.setDefaultValues(this, R.xml.preference, false);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		manURL = migrateManualURL(settings);
		settings.registerOnSharedPreferenceChangeListener(settingsListener);
		deleteTemporaryScripts();

		setContentView(R.layout.main);
		internalDir = this.getFilesDir();

		webview = (WebView) findViewById(R.id.webView1);
		webview.getSettings().setJavaScriptEnabled(true);
		WebView.setWebContentsDebuggingEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				Log.v("MoA", "onPageFinished");
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(thisActivity);
				float sc = settings.getFloat("maxima main scale", 1.5f);
				Log.v("MoA", "sc=" + Float.toString(sc));
				view.setInitialScale((int) (100 * sc));
			}

		});
		float sc = settings.getFloat("maxima main scale", 1.5f);
		Log.v("MoA", "onCreate sc=" + Float.toString(sc));
		webview.setInitialScale((int) (100 * sc));

		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setDisplayZoomControls(false);

		webview.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}
		});
		scview = (ScrollView) findViewById(R.id.scrollView1);

		webview.addJavascriptInterface(this, "MOA");

		editText = (MultiAutoCompleteTextView) findViewById(R.id.editText1);
		editText.setTextSize((float) Integer.parseInt(settings.getString("fontSize1","20")));
		Boolean bflag=settings.getBoolean("auto_completion_check_box_pref", true);
		if (bflag) {
			editText.setThreshold(2);
		} else {
			editText.setThreshold(100);
		}

		editText.setOnEditorActionListener(this);

		ArrayAdapter<String> adapter =
				new ArrayAdapter<String>(this,
						android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.MaximaCompletionList));
		editText.setTokenizer(new MaximaTokenizer());
		editText.setAdapter(adapter);
		webview.setOnTouchListener(this);

		enterB = (Button) findViewById(R.id.enterB);
		enterB.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_ENTER));
				editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
						KeyEvent.KEYCODE_ENTER));
			}
		});

		MaximaVersion prevVers = new MaximaVersion();
		prevVers.loadVersFromSharedPrefs(this);
		long verNo = prevVers.versionInteger();
		long thisVerNo = mvers.versionInteger();

		if ((thisVerNo > verNo)
				|| installedAppVersionCode() != appVersionCode()
				|| !maximaBinaryExists()
				|| !((new File(internalDir + "/additions")).exists())
				|| !((new File(internalDir + "/init.lisp")).exists())
				|| !(new File(internalDir + "/maxima-" + mvers.versionString()))
						.exists()) {
			Intent intent = new Intent(this, MOAInstallerActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.putExtra("version", mvers.versionString());
			this.startActivityForResult(intent, 0);
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					startMaxima();
				}
			}).start();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v("MoA", "onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);
		String sender = (data != null) ? data.getStringExtra("sender") : null;
		Log.v("MoA", "sender = " + sender);
		if (requestCode == READ_REQUEST_CODE) {
			// The user has selected a script file to load.
			if (resultCode == RESULT_OK && data != null && data.getData() != null) {
				copyScriptFileToInputArea(data.getData());
			}
		} else if ("manualActivity".equals(sender)) {
			if (resultCode == RESULT_OK) {
				String mcmd = data.getStringExtra("maxima command");
				if (mcmd != null) {
					copyExample(mcmd);
				}
			}
		} else if ("MOAInstallerActivity".equals(sender)) {
            if (resultCode == RESULT_OK) {
				/* everything is installed properly. */
                mvers.saveVersToSharedPrefs(this);
                saveInstalledAppVersionCode();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startMaxima();
                    }
                }).start();

            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.installer_title)
                        .setMessage(R.string.install_failure)
                        .setPositiveButton(R.string.OK,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        finish();
                                    }
                                }).show();
            }
        }
	}

	@Override
	protected void onResume() {
		Log.d("MoA","onResume");
		super.onResume();
		SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);

		if (changedSettings.contains("auto_completion_check_box_pref")) {
			editText.setThreshold(sharedPrefs.getBoolean("auto_completion_check_box_pref", true) ? 2 : 100);
		}

		if (changedSettings.contains("manURL")) {
			manLangChanged=true;
			manURL=migrateManualURL(sharedPrefs);
		} else {
			manLangChanged=false;
		}

		if (changedSettings.contains("fontSize1")) {
			editText.setTextSize((float) Integer.parseInt(sharedPrefs.getString("fontSize1","20")));
		}

		if (changedSettings.contains("fontSize2")) {
			webview.loadUrl("javascript:window.ChangeExpSize(" + sharedPrefs.getString("fontSize2","20") + ")");
		}

		changedSettings.clear();
	}

	@Override
	protected void onDestroy() {
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(settingsListener);
		// A new activity starts its own Maxima; do not leave this one running.
		if (maximaProccess != null) {
			maximaProccess.destroy();
		}
		super.onDestroy();
	}

	private void startMaxima() {
		Log.d("MoA", "startMaxima()");
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			Log.d("MoA", "exception1");
			e1.printStackTrace();
		}

		runOnUiThread(new Runnable() {@Override public void run() {webview.loadUrl(maximaURL);}});
		if (!(new File(internalDir + "/maxima-" + mvers.versionString()))
				.exists()) {
			this.finish();
		}
		List<String> list = new ArrayList<String>();
		list.add(NativeBinaries.path(this, NativeBinaries.MAXIMA));
		list.add("--init-lisp=" + internalDir + "/init.lisp");
		// Maxima creates its user directory before reading init.lisp, and the
		// app process has no HOME. init.lisp points gnuplot_command at MOA_GNUPLOT.
		Map<String, String> env = new HashMap<String, String>();
		env.put("HOME", internalDir.getAbsolutePath());
		env.put("MAXIMA_USERDIR", internalDir + "/maxima-userdir");
		env.put("MOA_GNUPLOT", NativeBinaries.path(this, NativeBinaries.GNUPLOT));
		env.put("GNUPLOT_JS_DIR", internalDir + "/additions/gnuplot/js");

		maximaProccess = new CommandExec();
		try {
			maximaProccess.execCommand(list, env);
		} catch (Exception e) {
			Log.d("MoA", "exception2");
			exitMOA();
		}
		maximaProccess.clearStringBuilder();
		sem.release();
		Log.v("MoA", "sem released.");
	}

	private void copyExample(String mcmd) {
		Log.d("MoA", "copyExample()");
		mcmdArray = MaximaText.exampleCommands(mcmd);
		mcmdArrayIndex = 0;
		copyExampleInputToInputArea();
	}

	private void copyExampleInputToInputArea() {
		if (mcmdArray == null)
			return;
		Log.d("MoA", "copyExampleInputToInputArea()");
		editText.setText(mcmdArray[mcmdArrayIndex]);
		editText.setSelection(mcmdArray[mcmdArrayIndex].length());
		editText.requestFocus();
		mcmdArrayIndex++;
		if (mcmdArrayIndex == mcmdArray.length) {
			allExampleFinished = true;
			mcmdArrayIndex = 0;
		}
	}

	@JavascriptInterface
	public void reuseByTouch(String maximacmd) {
		class rbttask implements Runnable {
			String text = "";

			@Override
			public void run() {
				editText.setText(text);
			}

			public void settext(String tt) {
				text = tt;
			}
		}
		rbttask viewtask = new rbttask();
		viewtask.settext(substitute(maximacmd, "<br>", ""));
		editText.post(viewtask);
	}

	@JavascriptInterface
	public void reuseOutput(String oNumStr) {
		if (oNumStr.equals("nolabel")) {
			return;
		}
		String olabel="$%"+oNumStr;
		String cmdstr=":lisp ($printf nil \"$$$$$$ R0 ~a $$$$$$\" "+olabel+")";
		try {
			maximaProccess.maximaCmd(cmdstr + "\n");
		} catch (IOException e) {
			Log.d("MoA", "reuseOutput exception1");
			e.printStackTrace();
			exitMOA();
		} catch (Exception e) {
			Log.d("MoA", "reuseOutput exception2");
			e.printStackTrace();
			exitMOA();
		}
		String resString = maximaProccess.getProcessResult();
		maximaProccess.clearStringBuilder();
		Pattern pa= Pattern.compile(".*\\$\\$\\$\\$\\$\\$ R0 (.+) \\$\\$\\$\\$\\$\\$.*");
		Matcher ma=pa.matcher(resString);
		if (ma.find()) {
			final String oText=ma.group(1).replace("\\'","'");
			runOnUiThread(new Runnable() {@Override public void run() { editText.setText(oText); }});
			ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboardManager.setPrimaryClip(ClipData.newPlainText("text", oText));
		}
	}

	@JavascriptInterface
	public void scrollToEnd() {
		// Called from JavaScript (not the UI thread); give MathJax time to render.
		scview.postDelayed(new Runnable() {
			@Override
			public void run() {
				scview.fullScroll(ScrollView.FOCUS_DOWN);
			}
		}, 1000);
	}

	public boolean onEditorAction(TextView testview, int id, KeyEvent keyEvent) {
		try {
			Log.v("MoA", "onEditorAction");
			sem.acquire();
		} catch (InterruptedException e1) {
			Log.d("MoA", "exception3");
			e1.printStackTrace();
			exitMOA();
		}
		sem.release();
		if (!inited) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(thisActivity);
			final String newsize = settings.getString("fontSize2", "");
			if (!newsize.equals("")) {
				runOnUiThread(new Runnable() {@Override public void run() {webview.loadUrl("javascript:window.ChangeExpSize("+newsize+")");}});
			}
			inited=true;
		}

		Log.v("MoA", "sem released");
		String cmdstr = editText.getText().toString();
		if ((keyEvent == null) || (keyEvent.getAction() == KeyEvent.ACTION_UP)) {
			try {
				if (cmdstr.equals("")) {
					return true;
				}
				if (cmdstr.equals("reload;")) {
					webview.loadUrl(maximaURL);
					return true;
				}
				if (cmdstr.equals("sc;")) {
					this.scrollToEnd();
					return true;
				}
				if (cmdstr.equals("quit();"))
					exitMOA();
				if (cmdstr.equals("aboutme;")) {
					showHTML("file:///android_asset/About_MoA/index.html", true);
					return true;
				}
				if (cmdstr.equals("man;")) {
					showHTML(manen, true);
					return true;
				}
				if (cmdstr.equals("manj;")) {
					showHTML(manjp, true);
					return true;
				}

				removeTmpFiles();
				cmdstr = maxima_syntax_check(cmdstr);
				maximaProccess.maximaCmd(cmdstr + "\n");
			} catch (IOException e) {
				Log.d("MoA", "exception4");
				e.printStackTrace();
				exitMOA();
			} catch (Exception e) {
				Log.d("MoA", "exception5");
				e.printStackTrace();
				exitMOA();
			}

			final String cmdstr2 = cmdstr;
			runOnUiThread(new Runnable() {@Override public void run() {webview.loadUrl("javascript:window.UpdateInput('"
					+ escapeChars(cmdstr2) + "<br>" + "')");}});
			String resString = maximaProccess.getProcessResult();
			maximaProccess.clearStringBuilder();
			while (isStartQepcadString(resString)) {
				List<String> list = new ArrayList<String>();
				// qepcad reads its help file from $qe/bin and talks to maxima via files.
				list.add("/system/bin/sh");
				list.add("-c");
				list.add("qe=\"$3\"; export qe; exec \"$0\" < \"$1\" > \"$2\"");
				list.add(NativeBinaries.path(this, NativeBinaries.QEPCAD));
				list.add(internalDir + "/qepcad_input.txt");
				list.add(internalDir + "/qepcad_output.txt");
				list.add(internalDir + "/additions/qepcad");
				CommandExec qepcadcom = new CommandExec();
				try {
					qepcadcom.execCommand(list);
				} catch (Exception e) {
					Log.d("MoA", "exception7");
				}
				try {
					maximaProccess.maximaCmd("qepcad finished\n");
				} catch (Exception e) {
					Log.d("MoA", "exception8");
				}
				resString = maximaProccess.getProcessResult();
				maximaProccess.clearStringBuilder();
			}

			// Maxima runs gnuplot itself; a plot command leaves maxout.html.
			if (new File(internalDir, "maxout.html").exists()) {
				showGraph();
			}

			displayMaximaCmdResults(resString);

		}

		return true;
	}

	private boolean isStartQepcadString(String res) {
		int i = res.indexOf("start qepcad");
		if (i < 0) {
			return false;
		} else if (i == 0) {
			return true;
		} else {
			// i>0
			displayMaximaCmdResults(res.substring(0, i));
			return true;
		}

	}

	private String maxima_syntax_check(String cmd) {
		return MaximaText.terminateCommand(cmd);
	}

	private String escapeChars(String cmd) {
		return MaximaText.escapeQuotes(cmd);
	}

	private void displayMaximaCmdResults(String resString) {
		Log.v("MoA cmd", resString);
		String[] resArray = resString.split("\\$\\$\\$\\$\\$\\$");
		for (int i = 0; i < resArray.length; i++) {
			if (i % 2 == 0) {
				/* normal text, as we are outside of $$$$$$...$$$$$$ */
				if (resArray[i].equals(""))
					continue;
				final String htmlStr = substitute(resArray[i], "\n", "<br>");
				runOnUiThread(new Runnable() {@Override public void run() {webview.loadUrl("javascript:window.UpdateText('" + htmlStr + "')");}});
			} else {
				/* tex commands, as we are inside of $$$$$$...$$$$$$ */
				String texStr = substCRinMBOX(resArray[i]);
				texStr = substitute(texStr, "\n", " \\\\\\\\ ");
				texStr = substituteMBOXVERB(texStr);
				final String urlstr = "javascript:window.UpdateMath('" + texStr + "')";
				runOnUiThread(new Runnable() {@Override public void run() {webview.loadUrl(urlstr);}});
			}
		}
		if (allExampleFinished == true) {
			Toast.makeText(this, "All examples are executed.",
					Toast.LENGTH_LONG).show();
			allExampleFinished = false;
		}
	}

	private String substCRinMBOX(String str) {
		return MaximaText.splitMboxLines(str);
	}

	static private String substituteMBOXVERB(String texStr) {
		return MaximaText.mergeVerbChars(texStr);
	}

	static private String substitute(String input, String pattern,
			String replacement) {
		return MaximaText.substitute(input, pattern, replacement);
	}

	private void showHTML(String url, boolean hwaccel) {
		Intent intent = new Intent(this, HTMLActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra("url", url);
		intent.putExtra("hwaccel", hwaccel);
		this.startActivity(intent);
	}

    private void showPreference() {
        Intent intent = new Intent(this, MoAPreferenceActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        this.startActivity(intent);
    }

	private void showManual() {
		Intent intent = new Intent(this, ManualActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra("url", manURL);
		intent.putExtra("manLangChanged", manLangChanged);
		manLangChanged = false;
		this.startActivityForResult(intent, 0);
	}

	private void showGraph() {
		File graph = new File(internalDir, "maxout.html");
		if (graph.exists()) {
			showHTML("file://" + graph.getAbsolutePath(), false);
		} else {
			Toast.makeText(this, getString(R.string.toast_no_graph), Toast.LENGTH_LONG).show();
		}
	}

	private void removeTmpFiles() {
		// gnuplot scripts and data written by plot2d/plot3d/draw
		File[] scripts = internalDir.listFiles();
		if (scripts != null) {
			for (File f : scripts) {
				if (f.isFile() && f.getName().endsWith(".gnuplot")) {
					f.delete();
				}
			}
		}
		File a = new File(internalDir, "maxout.html");
		if (a.exists()) {
			a.delete();
		}
		a = new File(internalDir, "qepcad_input.txt");
		if (a.exists()) {
			a.delete();
		}
	}

	private void exitMOA() {
		try {
			maximaProccess.maximaCmd("quit();\n");
			finish();
		} catch (IOException e) {
			Log.d("MoA", "exception7");
			e.printStackTrace();
		} catch (Exception e) {
			Log.d("MoA", "exception8");
			e.printStackTrace();
		}
		finish();
	}
	
	/*
	 * The manual moved from maxima-doc/[en/]<lang>/maxima.html (Maxima 5.41) to
	 * maxima-doc/<lang>/index.html; map a stored choice to the new layout.
	 */
	private String migrateManualURL(SharedPreferences settings) {
		String url = settings.getString("manURL", manen);
		String migrated = MaximaText.migratedManualUrl(url);
		if (migrated != null) {
			url = migrated;
			settings.edit().putString("manURL", url).apply();
		}
		return url;
	}

	private boolean maximaBinaryExists() {
		return NativeBinaries.get(this, NativeBinaries.MAXIMA).exists();
	}

	/* An app update may change the bundled additions, so reinstall them. */
	private long appVersionCode() {
		try {
			return PackageInfoCompat.getLongVersionCode(
					getPackageManager().getPackageInfo(getPackageName(), 0));
		} catch (PackageManager.NameNotFoundException e) {
			return -1;
		}
	}

	private long installedAppVersionCode() {
		return getSharedPreferences("maxima", MODE_PRIVATE)
				.getLong("appVersionCode", -1);
	}

	private void saveInstalledAppVersionCode() {
		getSharedPreferences("maxima", MODE_PRIVATE).edit()
				.putLong("appVersionCode", appVersionCode()).apply();
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		if ((arg0 == webview) && (arg1.getAction() == MotionEvent.ACTION_UP)) {
			Log.v("MoA", "onTouch on webview");
			@SuppressWarnings("deprecation")
			float sc = webview.getScale();
			Log.v("MoA", "sc=" + Float.toString(sc));
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			Editor editor = settings.edit();
			editor.putFloat("maxima main scale", sc);
			editor.apply();
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.about) {
			showHTML("file:///android_asset/About_MoA/index.html", true);
			return true;
		} else if (itemId == R.id.preference) {
			showPreference();
			return true;
		} else if (itemId == R.id.graph) {
			showGraph();
			return true;
		} else if (itemId == R.id.quit) {
			exitMOA();
			return true;
		} else if (itemId == R.id.nextexample) {
			copyExampleInputToInputArea();
			return false;
		} else if (itemId == R.id.man) {
			showManual();
			return true;
		} else if (itemId == R.id.save) {
			sessionMenu("ssave();");
			return true;
		} else if (itemId == R.id.restore) {
			sessionMenu("srestore();");
			return true;
		} else if (itemId == R.id.playback) {
			sessionMenu("playback();");
			return true;
		} else if (itemId == R.id.loadscript) {
			selectScriptFile();
			return false;
		}
		return super.onOptionsItemSelected(item);
	}

	private void sessionMenu(String cmd) {
		editText.setText(cmd);
		editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
				KeyEvent.KEYCODE_ENTER));
		editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
				KeyEvent.KEYCODE_ENTER));
	}

	private void selectScriptFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		startActivityForResult(intent, READ_REQUEST_CODE);
	}

	/*
	 * Copies the selected script into the cache directory (the content URI is
	 * not a file Maxima can open) and puts a batch() command for it into the
	 * input field.
	 */
	private void copyScriptFileToInputArea(Uri fileUri) {
		deleteTemporaryScripts();
		File script = null;
		boolean tooLarge = false;
		try (InputStream in = getContentResolver().openInputStream(fileUri)) {
			if (in == null) {
				throw new IOException("Cannot open " + fileUri);
			}
			script = File.createTempFile("userscript", ".mac", getCacheDir());
			try (OutputStream out = new FileOutputStream(script)) {
				byte[] buffer = new byte[64 * 1024];
				long total = 0;
				int n;
				while ((n = in.read(buffer)) != -1) {
					total += n;
					if (total > SCRIPT_FILE_MAX_SIZE) {
						tooLarge = true;
						break;
					}
					out.write(buffer, 0, n);
				}
			}
		} catch (IOException | SecurityException e) {
			Log.e("MoA", "Cannot load script " + fileUri, e);
			if (script != null) {
				script.delete();
			}
			Toast.makeText(this, R.string.error_loading_script_file, Toast.LENGTH_LONG).show();
			return;
		}
		if (tooLarge) {
			script.delete();
			Toast.makeText(this, R.string.script_file_too_large, Toast.LENGTH_LONG).show();
			return;
		}
		editText.setText("batch(\"" + script.getAbsolutePath() + "\");");
	}

	/* Copies made by copyScriptFileToInputArea() for earlier scripts. */
	private void deleteTemporaryScripts() {
		File[] files = getCacheDir().listFiles();
		if (files == null) {
			return;
		}
		for (File f : files) {
			if (f.getName().startsWith("userscript") && f.getName().endsWith(".mac")) {
				f.delete();
			}
		}
	}
}
