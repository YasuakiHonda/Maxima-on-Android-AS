/*
    Copyright 2015 Yasuaki Honda (yasuaki.honda@gmail.com)
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

import android.content.Context;

/*
 * The maxima, gnuplot and qepcad executables are packaged as
 * jniLibs/<abi>/lib*.so so that the package installer extracts them into
 * nativeLibraryDir. Since targetSdk 29, executing files from the app's
 * writable data directory is not permitted, but nativeLibraryDir is allowed.
 * The installer picks the right ABI, so no CPU detection is needed here.
 */
public final class NativeBinaries {
	static final String MAXIMA = "libmaxima.so";
	static final String GNUPLOT = "libgnuplot.so";
	static final String QEPCAD = "libqepcad.so";

	private NativeBinaries() {
	}

	public static File get(Context context, String name) {
		return new File(context.getApplicationInfo().nativeLibraryDir, name);
	}

	public static String path(Context context, String name) {
		return get(context, name).getAbsolutePath();
	}
}
