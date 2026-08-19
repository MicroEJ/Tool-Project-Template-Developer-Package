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
import static com.microej.test.util.TestUtil.newOutputBuffer;
import static com.microej.test.util.TestUtil.stopRunOnDevice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

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

	/**
	 * Matches the AppConnect trace printed by the kernel, and captures the port it listens on.
	 *
	 * <p>
	 * The pattern is anchored on the line break, so it cannot match a port number that is only partially written.
	 */
	private static final Pattern APPCONNECT_PORT_PATTERN = Pattern
			.compile("AppConnect listening on \\(http\\) port:\\s*(\\d+)\\R");

	/**
	 * Matches the IP address trace printed by the kernel, and captures the address.
	 *
	 * <p>
	 * The pattern is anchored on the line break, so it cannot match an address that is only partially written.
	 */
	private static final Pattern IP_ADDRESS_PATTERN = Pattern
			.compile("---\\s*IP Address:\\s*((?:\\d{1,3}\\.){3}\\d{1,3})\\R");

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

				// "Hello World!" is printed by the application thread while "Application running: app" is printed by
				// the kernel, so their relative order is not guaranteed: check this trace in the whole output instead
				// of relying on an order.
				assertTrue(output.waitUntilContains("Application running: app", true));
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
				// The AppConnect trace and the IP address trace are printed by two different threads of the kernel, so
				// their relative order is not guaranteed: check both traces in the whole output instead of relying on
				// an order.
				MatchResult portMatch = output.waitUntilMatches(APPCONNECT_PORT_PATTERN, true);
				assertNotNull(portMatch);
				MatchResult hostMatch = output.waitUntilMatches(IP_ADDRESS_PATTERN, true);
				assertNotNull(hostMatch);

				String port = portMatch.group(1);
				String host = hostMatch.group(1);

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
