package jp.yhonda;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ZipExtractorTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** A zip with the given entries; names ending in '/' are directories. */
    private static ByteArrayInputStream zip(String... namesAndContents) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                out.putNextEntry(new ZipEntry(namesAndContents[i]));
                out.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void extractsFilesAndDirectories() throws IOException {
        File dest = tmp.newFolder();
        ZipExtractor.extract(zip(
                "additions/", "",
                "additions/gnuplot/js/canvastext.js", "js",
                "additions/qepcad/bin/qepcad.help", "help"), dest);
        assertEquals("js", read(new File(dest, "additions/gnuplot/js/canvastext.js")));
        assertEquals("help", read(new File(dest, "additions/qepcad/bin/qepcad.help")));
    }

    @Test
    public void createsParentDirectoriesWithoutDirectoryEntries() throws IOException {
        File dest = tmp.newFolder();
        ZipExtractor.extract(zip("maxima-5.50.0/share/draw/draw.lisp", "(in-package :maxima)"), dest);
        assertTrue(new File(dest, "maxima-5.50.0/share/draw/draw.lisp").isFile());
    }

    @Test
    public void replacesExistingFiles() throws IOException {
        File dest = tmp.newFolder();
        ZipExtractor.extract(zip("a/init.txt", "old content that is longer"), dest);
        ZipExtractor.extract(zip("a/", "", "a/init.txt", "new"), dest);
        assertEquals("new", read(new File(dest, "a/init.txt")));
    }

    @Test
    public void rejectsEntriesOutsideTheTarget() throws IOException {
        File dest = tmp.newFolder("dest");
        try {
            ZipExtractor.extract(zip("../evil.txt", "x"), dest);
            fail("expected IOException");
        } catch (IOException expected) {
            // ok
        }
        assertFalse(new File(dest.getParentFile(), "evil.txt").exists());
    }

    @Test
    public void deleteRecursivelyRemovesTree() throws IOException {
        File dir = tmp.newFolder("maxima-5.41.0");
        ZipExtractor.extract(zip("share/a/b.mac", "1", "share/c.lisp", "2"), dir);
        assertTrue(ZipExtractor.deleteRecursively(dir));
        assertFalse(dir.exists());
        assertTrue(ZipExtractor.deleteRecursively(new File(dir, "missing")));
    }
}
