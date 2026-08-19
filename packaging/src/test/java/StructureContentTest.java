/*
 * Java
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

import static com.microej.test.util.TestUtil.assertFilesDoNotContainTerms;
import static com.microej.test.util.TestUtil.assertFilesDoNotExist;
import static com.microej.test.util.TestUtil.assertFilesExist;
import static com.microej.test.util.TestUtil.getTestedPackageDir;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests that the structure and the content of the package complies with the expected output.
 */
public class StructureContentTest extends PackagingTest {

	@Test
	public void testFileStructure() {
		// Given
		File packageDir = getTestedPackageDir();
		List<String> files = List.of("README.md", "CHANGELOG.md", "doc/", "doc/index.html", "src", "src/app",
				"src/vee/", "src/vee/kernel", "src/vee/linux-x86", "src/settings.gradle.kts",
				"bin/executable/kernel-linux-x86/application.out", "javadoc");
		// Then
		assertFilesExist(packageDir, files);

		// Given
		files = List.of("JenkinsFile", ".gh-copyright.template", ".gh.keep_binary", "packaging", ".git",
				"src/vee/linux-x86/linux-abstraction-layer/vee/build-i686-linux-gnu-gcc",
				"src/vee/linux-x86/linux-abstraction-layer/vee/lib",
				"src/vee/linux-x86/linux-abstraction-layer/vee/scripts/application.out", "CLAUDE.md");
		// Then
		assertFilesDoNotExist(packageDir, files);
	}

	@Test
	public void testUnwantedTerms() {
		// Given
		List<String> terms = List.of("CustomerXXX", "LicenseXXX");

		// Then
		assertFilesDoNotContainTerms(getTestedPackageDir(), terms);
	}

	@Test
	public void testModuleRepository() {
		// Given
		File packageDir = getTestedPackageDir();
		List<String> files = List.of("repository/module-repository.gradle.kts",
				"repository/com/microej/architecture/I386/GNUvX_X86Linux/atsauce6/8.4.0/atsauce6-8.4.0-eval.xpf");
		// Then
		assertFilesExist(packageDir, files);

		// Given
		files = List
				.of("repository/com/microej/architecture/I386/GNUvX_X86Linux/atsauce6/8.4.0/atsauce6-8.4.0-prod.xpf");
		// Then
		assertFilesDoNotExist(packageDir, files);
	}

	@Test
	public void testVirtualDevice() {
		// Given
		List<String> files = List.of("bin/virtualDevice/kernel-linux-x86/vee");

		// Then
		assertFilesExist(getTestedPackageDir(), files);
	}
}
