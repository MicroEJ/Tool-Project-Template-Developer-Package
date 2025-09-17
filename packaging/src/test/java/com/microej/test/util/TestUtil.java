/*
 * Java
 *
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.microej.test.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TestUtil {

	/**
	 * Retrieves the package and unzips it to prepare it for the tests.
	 *
	 * @return the path of the unzipped package
	 * @throws IOException
	 *             if an error occurs while preparing the package
	 */
	public static File prepareTestedPackage() throws IOException {
		URL testProjectDirUrl = TestUtil.class.getResource("/");
		File buildDir = new File(testProjectDirUrl.getFile()).toPath().resolve("../../..").toFile();
		File packageDir = new File(buildDir, "package");
		Optional<File> foundPackageFile = Arrays.stream(packageDir.listFiles((dir, name) -> name.endsWith(".zip")))
				.findFirst();
		File packageFile;
		if (foundPackageFile.isPresent() && foundPackageFile.get().exists()) {
			packageFile = foundPackageFile.get();
		} else {
			throw new RuntimeException("No package found!");
		}

		Path testPackageDirPath = buildDir.toPath().resolve("tmp/testedPackage");
		unzip(packageFile.toPath(), testPackageDirPath);
		return testPackageDirPath.toFile();
	}

	private static void unzip(Path path, Path destFolderPath) throws IOException {
		try (ZipFile zipFile = new ZipFile(path.toFile(), ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				Path entryPath = destFolderPath.resolve(entry.getName());
				if (entryPath.normalize().startsWith(destFolderPath.normalize())) {
					if (entry.isDirectory()) {
						Files.createDirectories(entryPath);
					} else {
						Files.createDirectories(entryPath.getParent());
						try (InputStream in = zipFile.getInputStream(entry)) {
							try (OutputStream out = new FileOutputStream(entryPath.toFile())) {
								IOUtils.copy(in, out);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Gets the arguments for the Gradle build command.
	 * 
	 * @param testPackageDir
	 *            the root dir of the package to test
	 * @param args
	 *            the other arguments to add in the command line
	 * @return the list of arguments to pass to the command
	 */
	public static List<String> getBuildArguments(File testPackageDir, String... args) {
		List<String> arguments = new ArrayList<>(List.of(args));

		String initScriptFilePath = getInitScriptPath();
		arguments.add("-I");
		arguments.add(initScriptFilePath);
		arguments.add("-Dpackage.root.dir=" + testPackageDir.getAbsolutePath());

		return arguments;
	}

	private static String getInitScriptPath() {
		URL initScriptFile = TestUtil.class.getResource("/test.init.gradle.kts");
		return initScriptFile.getFile();
	}

}
