/*
    Copyright 2012, 2013, Yasuaki Honda (yasuaki.honda@gmail.com)
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts the installer's zip archives (plain Java, unit tested). */
final class ZipExtractor {
    private ZipExtractor() {
    }

    /**
     * Extracts every entry of the zip stream below destDir, replacing existing
     * files, and closes the stream. Entries that would land outside destDir
     * are rejected.
     */
    static void extract(InputStream zip, File destDir) throws IOException {
        String root = destDir.getCanonicalPath() + File.separator;
        byte[] buffer = new byte[64 * 1024];
        try (ZipInputStream zin = new ZipInputStream(zip)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                File file = new File(destDir, entry.getName());
                if (!file.getCanonicalPath().startsWith(root)) {
                    throw new IOException("Zip entry outside the target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    mkdirs(file);
                    continue;
                }
                mkdirs(file.getParentFile());
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                    int n;
                    while ((n = zin.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                    }
                }
            }
        }
    }

    /** Deletes a file or a directory tree; returns false if anything remains. */
    static boolean deleteRecursively(File file) {
        if (!file.exists()) {
            return true;
        }
        File[] children = file.isDirectory() ? file.listFiles() : null;
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursively(child)) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    private static void mkdirs(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
    }
}
