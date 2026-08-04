/*
 * Java
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

import static com.microej.test.util.GradleBuildRunner.BUILD_SUCCESS;
import static com.microej.test.util.TestProperties.EXECUTABLE_OUTPUT_DIR;
import static com.microej.test.util.TestProperties.KERNEL_PROJECT_NAME;
import static com.microej.test.util.TestProperties.VEEPORT_PROJECT_NAME;
import static com.microej.test.util.TestUtil.extractFrom;
import static com.microej.test.util.TestUtil.newOutputBuffer;
import static com.microej.test.util.TestUtil.stopRunOnDevice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.microej.test.util.GradleBuildRunner;
import com.microej.test.util.StringOutputBuffer;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BuildRunDeployTest extends PackagingTest {

	private final GradleBuildRunner buildRunner = new GradleBuildRunner();

	@AfterEach
	public void tearDown() {
		buildRunner.destroyAllProcesses();
	}

	@Test
	@Order(1)
	public void testAppRunSimulator() throws Exception {
		try (StringOutputBuffer output = newOutputBuffer()) {
			// When
			Process process = buildRunner.startAsynchronousBuild(srcDir, output.getWriter(), ":app:runOnSimulator",
					"--info");
			try {
				// Then
				assertTrue(output.waitUntilContains("Starting Kernel"));
				assertTrue(output.waitUntilContains("New application installation detected: app"));
				assertTrue(output.waitUntilContains("Hello World!"));
				assertTrue(output.waitUntilContains("Application running: app"));
			} finally {
				buildRunner.destroyProcess(process);
			}
		}
	}

	@Test
	@Order(2)
	public void testKernelBuildExecutable() throws Exception {
		try (StringOutputBuffer output = newOutputBuffer()) {
			// When
			int exitCode = buildRunner.startBuild(srcDir, output.getWriter(),
					":" + KERNEL_PROJECT_NAME + ":buildExecutable", "--info", "-Dapp.developer.mode.enabled=false");

			// Then
			assertEquals(BUILD_SUCCESS, exitCode);
			assertTrue(output.getContent().contains("[100%] Built target application.out"));
			assertTrue(new File(srcDir, EXECUTABLE_OUTPUT_DIR + "application.out").exists());
		}
	}

	@Test
	@Order(3)
	public void testKernelRunOnDevice() throws Exception {
		try (StringOutputBuffer output = newOutputBuffer()) {
			try {
				// When
				buildRunner.startAsynchronousBuild(srcDir, output.getWriter(),
						":" + KERNEL_PROJECT_NAME + ":runOnDevice", "--info", "-Dapp.developer.mode.enabled=false");

				// Then
				assertTrue(output.waitUntilContains("MicroEJ START"));
				assertTrue(output.waitUntilContains("INFO: Starting Kernel"));
			} finally {
				stopRunOnDevice(buildRunner, srcDir, output.getWriter());
			}
		}
	}

	@Test
	@Order(4)
	public void testAppBuildFeatureWithAppDeveloperModeEnabled() throws IOException {
		try (StringOutputBuffer output = newOutputBuffer()) {
			// When
			int exitCode = buildRunner.startBuild(srcDir, output.getWriter(), ":app:buildFeature", "--info");

			// Then
			assertEquals(BUILD_SUCCESS, exitCode);
			assertTrue(new File(srcDir, "app/build/application/feature/application.fo").exists());
			String outputContent = output.getContent();
			assertFalse(outputContent.contains("> Task :" + KERNEL_PROJECT_NAME + ":compileJava"));
			assertFalse(outputContent.contains("> Task :" + VEEPORT_PROJECT_NAME + ":buildVeePortConfiguration"));
		}
	}

	@Test
	@Order(5)
	public void testAppBuildFeatureWithAppDeveloperModeDisabled() throws IOException {
		try (StringOutputBuffer output = newOutputBuffer()) {
			// When
			int exitCode = buildRunner.startBuild(srcDir, output.getWriter(), ":app:buildFeature",
					"-Dapp.developer.mode.enabled=false", "--info");

			// Then
			assertEquals(BUILD_SUCCESS, exitCode);
			assertTrue(new File(srcDir, "app/build/application/feature/application.fo").exists());
			String outputContent = output.getContent();
			assertTrue(outputContent.contains("> Task :" + KERNEL_PROJECT_NAME + ":compileJava"));
			assertTrue(outputContent.contains("> Task :" + VEEPORT_PROJECT_NAME + ":buildVeePortConfiguration"));
			assertTrue(outputContent.contains("[100%] Built target application.out"));
		}
	}

	@Test
	@Disabled
	@Order(6)
	public void testAppFeatureDeploy() throws Exception {
		try (StringOutputBuffer output = newOutputBuffer()) {
			try {
				// When
				buildRunner.startAsynchronousBuild(srcDir, output.getWriter(),
						":" + KERNEL_PROJECT_NAME + ":runOnDevice", "--info", "-Dapp.developer.mode.enabled=false");

				// Then
				assertTrue(output.waitUntilContains("AppConnect listening on (http) port: "));
				assertTrue(output.waitUntilContains("--- IP Address:"));

				String outputText = output.getContent();
				String port = extractFrom(outputText, "listening on \\(http\\) port:\\s*(\\d+)", "4001");
				String host = extractFrom(outputText, "---\\s*IP Address:\\s*((?:\\d{1,3}\\.){3}\\d{1,3})",
						"127.0.0.1");

				// The kernel is now running and app connect is listening on host:port
				// Use the "featureDeploy" task declared in the app to deploy the app onto the kernel
				int exitCode = buildRunner.startBuild(srcDir, output.getWriter(), ":app:featureDeploy",
						"-Dapp.developer.mode.enabled=false", "-Pappconnect.host=" + host, "-Pappconnect.port=" + port,
						"--info");

				assertEquals(BUILD_SUCCESS, exitCode);
				assertTrue(output.waitUntilContains("Deployment Successful! Response Code: 200"));
				assertTrue(output.waitUntilContains("New application installation detected: app"));
				assertTrue(output.waitUntilContains("Application running: app"));
			} finally {
				stopRunOnDevice(buildRunner, srcDir, output.getWriter());
			}
		}
	}

}
