/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

import org.newdawn.slick.util.Log;
import yugecin.opsudance.utils.ManifestWrapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.jar.JarFile;

import static yugecin.opsudance.core.InstanceContainer.*;

public class NativeLoader {

	public static void setNativePath() {
		String nativepath = config.NATIVE_DIR.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativepath);
		System.setProperty("java.library.path", nativepath);

		try {
			// Workaround for "java.library.path" property being read-only.
			// http://stackoverflow.com/a/24988095
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {
			Log.warn("Failed to set 'sys_paths' field.", e);
		}
	}

	/**
	 * Unpacks natives for the current operating system to the natives directory.
	 * @throws IOException if an I/O exception occurs
	 */
	public static void loadNatives(JarFile jarfile, ManifestWrapper manifest) throws IOException {
		if (!config.NATIVE_DIR.exists() && !config.NATIVE_DIR.mkdir()) {
			String msg = String.format("Could not create folder '%s'",
				config.NATIVE_DIR.getAbsolutePath());
			throw new RuntimeException(msg);
		}

		String osName = System.getProperty("os.name");
		String nativekey = null;
		if (osName.startsWith("Win")) {
			nativekey = "WinNatives";
		} else if (osName.startsWith("Linux")) {
			nativekey = "NixNatives";
		} else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
			nativekey = "MacNatives";
		}

		if (nativekey == null) {
			Log.warn("Cannot determine natives for os " + osName);
			return;
		}

		String natives = manifest.valueOrDefault(null, nativekey, null);
		if (natives == null) {
			String msg = String.format("No entry for '%s' in manifest, jar is badly packed or damaged",
				nativekey);
			throw new RuntimeException(msg);
		}

		String[] nativefiles = natives.split(",");
		for (String nativefile : nativefiles) {
			File unpackedFile = new File(config.NATIVE_DIR, nativefile);
			if (unpackedFile.exists()) {
				continue;
			}
			Utils.unpackFromJar(jarfile, unpackedFile, nativefile);
		}
	}

}