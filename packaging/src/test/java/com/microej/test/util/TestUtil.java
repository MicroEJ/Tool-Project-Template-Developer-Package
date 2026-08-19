/*
 * Java
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

package com.microej.test.util;

import static com.microej.test.util.TestProperties.DEPLOY_NAME;
import static com.microej.test.util.TestProperties.IS_WINDOWS;
import static com.microej.test.util.TestProperties.JENKINS_JOB_NAME;
import static com.microej.test.util.TestProperties.JENKINS_URL;
import static com.microej.test.util.TestProperties.JENKINS_USER_NAME;
import static com.microej.test.util.TestProperties.JENKINS_USER_TOKEN;
import static com.microej.test.util.TestProperties.KERNEL_PROJECT_NAME;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_ACTION;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_JOB_NAME;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_URL;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_USER_NAME;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_USER_TOKEN;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class TestUtil {

	public final static List<String> BINARY_EXTENSIONS = List.of("zip", "jar", "out", "exe", "o");

	/**
	 * Retrieves the package and unzips it to prepare it for the tests.
	 *
	 * @return the path of the unzipped package
	 * @throws IOException
	 *             if an error occurs while preparing the package
	 */
	public static File prepareTestedPackage() throws IOException {
		File buildDir = getBuildDir();
		File packageDir = new File(buildDir, "package");
		Optional<File> foundPackageFile = Arrays.stream(packageDir.listFiles((dir, name) -> name.endsWith(".zip")))
				.findFirst();
		File packageFile;
		if (foundPackageFile.isPresent() && foundPackageFile.get().exists()) {
			packageFile = foundPackageFile.get();
		} else {
			throw new RuntimeException("No package found!");
		}

		File testedPackageDir = new File(buildDir, "tmp/testedPackage");
		Path testedPackageDirPath = testedPackageDir.toPath();
		deleteDirectory(testedPackageDirPath);
		unzip(packageFile.toPath(), testedPackageDirPath);
		return testedPackageDir;
	}

	private static void deleteDirectory(Path dir) throws IOException {
		if (!Files.exists(dir))
			return;

		try (Stream<Path> stream = Files.walk(dir)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}

	private static File getBuildDir() {
		URL testProjectDirUrl = TestUtil.class.getResource("/");
		assert testProjectDirUrl != null;
		return new File(testProjectDirUrl.getFile()).toPath().resolve("../../..").toFile();
	}

	/**
	 * Returns the directory of the package being tested.
	 *
	 * @return a file that represents the directory of the package being tested
	 */
	public static File getTestedPackageDir() {
		File buildDir = getBuildDir();
		return buildDir.toPath().resolve("tmp/testedPackage").toFile();
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
						if (entryPath.getFileName().toString().equals("gradlew")) {
							setExecutablePermissionsTo(entryPath);
						}
					}
				}
			}
		}
	}

	private static void setExecutablePermissionsTo(Path executablePath) throws IOException {
		if (!System.getProperty("os.name").toLowerCase().contains("win")) {
			Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
					PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
					PosixFilePermission.OTHERS_EXECUTE));
			Files.setPosixFilePermissions(executablePath, permissions);
		}
	}

	/**
	 * Returns a new {@link StringOutputBuffer} to record and inspect the build output.
	 *
	 * @return a new instance of {@link StringOutputBuffer}.
	 */
	public static StringOutputBuffer newOutputBuffer() {
		return new StringOutputBuffer();
	}

	/**
	 * Returns a new {@link StringOutputBuffer} to record and inspect the build output, also redirecting the output from
	 * the serial port. The serial port configuration can be set via the system properties defined in
	 * {@link SerialConnection}.
	 *
	 * @return a new instance of {@link StringOutputBuffer}.
	 * @see SerialConnection
	 */
	public static StringOutputBuffer newOutputBufferWithSerialRedirection() throws IOException {
		return SerialConnection.fromSystemProperties();
	}

	/**
	 * Stops the process started by a {@code runOnDevice} build. If the deploy tool is Jenkins (i.e., the system
	 * property {@code microejtool.deploy.name} is set to {@code "jenkins"}), this method calls the Jenkins API to stop
	 * the specified Jenkins Deploy Job. Otherwise, it tries to kill the application process directly (VEE Linux case).
	 *
	 * <p>
	 * This overload allows specifying a custom Jenkins job name, which is useful when multiple deploy jobs exist (e.g.,
	 * one per VEE Port target).
	 *
	 * @param buildRunner
	 *            a build runner instance, used to invoke the Jenkins stop job when deploying via Jenkins
	 * @param projectDir
	 *            the root project directory (must contain a Gradle Wrapper)
	 * @param writer
	 *            a writer to redirect the stop command output to
	 * @param jobName
	 *            the name of the Jenkins Deploy Job to stop. Can be null, in this case it tries to kill the application
	 *            process directly (VEE Linux case).
	 */
	public static void stopRunOnDevice(GradleBuildRunner buildRunner, File projectDir, Writer writer, String jobName) {
		try {
			if ("jenkins".equals(DEPLOY_NAME) && jobName != null) {
				buildRunner.startBuild(projectDir, writer, ":" + KERNEL_PROJECT_NAME + ":execTool", "--name=jenkins",
						"--toolProperty=" + PROPERTY_JENKINS_JOB_NAME + "=" + jobName,
						"--toolProperty=" + PROPERTY_JENKINS_USER_NAME + "=" + JENKINS_USER_NAME,
						"--toolProperty=" + PROPERTY_JENKINS_USER_TOKEN + "=" + JENKINS_USER_TOKEN,
						"--toolProperty=" + PROPERTY_JENKINS_URL + "=" + JENKINS_URL,
						"--toolProperty=" + PROPERTY_JENKINS_ACTION + "=stopJob");
			} else {
				List<String> command = new ArrayList<>();
				if (IS_WINDOWS) {
					command.add("wsl");
				}
				command.addAll(List.of("pkill", "-f", "/application.out"));
				new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
						.redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor(5, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			// ignore any exceptions
		}
	}

	/**
	 * Stops the process started by a {@code runOnDevice} build, using the Jenkins job name from the system property
	 * {@code jenkins.job.name}.
	 *
	 * <p>
	 * This is a convenience overload of {@link #stopRunOnDevice(GradleBuildRunner, File, Writer, String)} that reads
	 * the job name from the system property. Use the overload with an explicit {@code jobName} parameter when the job
	 * name differs from the system property value (e.g., when stopping a specific target's deploy job).
	 *
	 * @param buildRunner
	 *            a build runner instance, used to invoke the Jenkins stop job when deploying via Jenkins
	 * @param projectDir
	 *            the root project directory (must contain a Gradle Wrapper)
	 * @param writer
	 *            a writer to redirect the stop command output to
	 */
	public static void stopRunOnDevice(GradleBuildRunner buildRunner, File projectDir, Writer writer) {
		stopRunOnDevice(buildRunner, projectDir, writer, JENKINS_JOB_NAME);
	}

	/**
	 * Fails if at least one of the specified files does not exist in the specified directory.
	 *
	 * @param baseDir
	 *            the base directory where to look for files
	 * @param paths
	 *            a list of file paths, relative to the given base directory
	 */
	public static void assertFilesExist(File baseDir, List<String> paths) {
		failOnFilesExistence(baseDir, paths, true);
	}

	/**
	 * Fails if at least one of the specified files exist in the specified directory.
	 *
	 * @param baseDir
	 *            the base directory where to look for files
	 * @param paths
	 *            a list of file paths, relative to the given base directory
	 */
	public static void assertFilesDoNotExist(File baseDir, List<String> paths) {
		failOnFilesExistence(baseDir, paths, false);
	}

	private static void failOnFilesExistence(File baseDir, List<String> paths, boolean shouldExist) {
		Predicate<File> predicate = shouldExist ? File::exists : ((Predicate<File>) File::exists).negate();

		List<String> failing = paths.stream().map(path -> new File(baseDir, path)).filter(predicate.negate())
				.map(file -> baseDir.toPath().relativize(file.toPath()).toString()).collect(Collectors.toList());

		if (!failing.isEmpty()) {
			fail((shouldExist ? "Missing files" : "Unexpected files") + ": " + failing);
		}
	}

	/**
	 * Fails if at least one file of the specified directory contains the specified terms in either its name or content
	 * (recursively).
	 *
	 * @param baseDir
	 *            the base directory where to look for files
	 * @param terms
	 *            a list of terms to look for in the files
	 */
	public static void assertFilesDoNotContainTerms(File baseDir, List<String> terms) {
		if (!baseDir.exists() || !baseDir.isDirectory()) {
			fail(baseDir + " is not a valid directory");
		}

		List<File> allFiles = listFilesRecursive(baseDir).collect(Collectors.toList());

		List<String> filesWithInvalidFilenames = allFiles.stream().map(File::getName)
				.filter(name -> terms.stream().anyMatch(name::contains)).collect(Collectors.toList());

		List<String> filesWithInvalidContent = allFiles.stream().filter(File::isFile)
				.filter(file -> !isBinaryFile(file)).filter(file -> {
					try {
						String content = Files.readString(file.toPath());
						return terms.stream().anyMatch(content::contains);
					} catch (IOException e) {
						// File could not be read (e.g., not text), skip it
						return false;
					}
				}).map(file -> baseDir.toPath().relativize(file.toPath()).toString()).collect(Collectors.toList());

		if (!filesWithInvalidFilenames.isEmpty() || !filesWithInvalidContent.isEmpty()) {
			fail("Unwanted term(s) detected in files: " + filesWithInvalidContent + filesWithInvalidFilenames);
		}
	}

	private static Stream<File> listFilesRecursive(File directory) {
		File[] children = directory.listFiles();
		if (children == null) {
			return Stream.empty();
		}

		return Stream.of(children).flatMap(file -> file.isDirectory() ? listFilesRecursive(file) : Stream.of(file));
	}

	private static boolean isBinaryFile(File file) {
		return BINARY_EXTENSIONS.contains(FilenameUtils.getExtension(file.getName()));
	}
}
