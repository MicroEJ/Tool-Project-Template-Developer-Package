/*
 * Java
 *
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.microej.test.util.TestUtil.getBuildArguments;
import static com.microej.test.util.TestUtil.prepareTestedPackage;

@Disabled // To be removed to enable the tests
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PackagingTest {

	static File testPackageDir;

	@BeforeAll
	public static void setup() throws IOException {
		testPackageDir = prepareTestedPackage();

		Assertions.assertTrue(testPackageDir.exists());
	}

	@Test
	@Order(1)
	public void testKernelBuildExecutable() {
		// Given
		File projectDir = new File(testPackageDir, "src/kernel");
		List<String> argumentsBuildExecutable = getBuildArguments(testPackageDir, "clean", "buildExecutable", "--info");

		// When
		BuildResult result = GradleRunner.create().withProjectDir(projectDir).withArguments(argumentsBuildExecutable)
				.forwardOutput().build();

		// Then
		Assertions.assertTrue(result.getOutput().contains("[430/430] Linking CXX executable npavee.elf"));
		Assertions.assertTrue(
				new File(testPackageDir, "src/kernel/build/application/executable/application.out").exists());
	}

	@Test
	@Order(2)
	public void testAppBuildFeature() {
		// Given
		File projectDir = new File(testPackageDir, "src/app");
		List<String> arguments = getBuildArguments(testPackageDir, "clean", "buildFeature", "--info");

		// When
		BuildResult result = GradleRunner.create().withProjectDir(projectDir).withArguments(arguments).forwardOutput()
				.build();

		// Then
		String output = result.getOutput();
		Assertions.assertTrue(output.contains("=============== [ Launching SOAR ] ==============="));
		Assertions.assertTrue(output.contains("=============== [ Completed Successfully ] ==============="));
		Assertions.assertTrue(new File(testPackageDir, "src/app/build/application/feature/application.fo").exists());
	}

}
