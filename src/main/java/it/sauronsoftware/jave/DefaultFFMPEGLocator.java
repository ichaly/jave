/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
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
package it.sauronsoftware.jave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The default ffmpeg executable locator, which exports on disk the ffmpeg
 * executable bundled with the library distributions. It should work both for
 * windows and many linux distributions. If it doesn't, try compiling your own
 * ffmpeg executable and plug it in JAVE with a custom {@link FFMPEGLocator}.
 * @author Carlo Pelliccia
 */
public class DefaultFFMPEGLocator extends FFMPEGLocator {
    /**
     * Trace the version of the bundled ffmpeg executable. It's a counter: every
     * time the bundled ffmpeg change it is incremented by 1.
     */
    private static final int myEXEversion = 3;

    /**
     * The ffmpeg executable file path.
     */
    private final String path;

    /**
     * It builds the default FFMPEGLocator, exporting the ffmpeg executable on a
     * temp file.
     */
    public DefaultFFMPEGLocator() {
        // Windows?
        boolean isWindows = false;
        String suffix = "";
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            isWindows = true;
            suffix = ".exe";
        } else if (os.contains("mac")) {
            suffix = ".mac";
        } else {
            suffix = ".linux";
        }
        // Temp dir?
        File temp = new File(System.getProperty("java.io.tmpdir"), "jave-" + myEXEversion);
        if (!temp.exists()) {
            temp.mkdirs();
            temp.deleteOnExit();
        }
        // ffmpeg executable export on disk.
        File exe = new File(temp, "ffmpeg" + suffix);
        if (!exe.exists()) {
            copyFile("ffmpeg" + suffix, exe);
        }
        // Need a chmod?
        if (!isWindows) {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(new String[]{"/bin/chmod", "755", exe.getAbsolutePath()});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Ok.
        this.path = exe.getAbsolutePath();
    }

    @Override
    protected String getFFMPEGExecutablePath() {
        return path;
    }

    /**
     * Copies a file bundled in the package to the supplied destination.
     * @param path The name of the bundled file.
     * @param dest The destination.
     * @throws RuntimeException If aun unexpected error occurs.
     */
    private void copyFile(String path, File dest) throws RuntimeException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = getClass().getResourceAsStream("/native/" + path);
            if (input != null) {
                output = new FileOutputStream(dest);
                byte[] buffer = new byte[1024];
                int l;
                while ((l = input.read(buffer)) != -1) {
                    output.write(buffer, 0, l);
                }
            } else {
                throw new RuntimeException("Cannot retrieve native file " + path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write file " + dest.getAbsolutePath());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable t) {
                    ; // Silent ignore
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ; // Silent ignore
                }
            }
        }
    }

}
