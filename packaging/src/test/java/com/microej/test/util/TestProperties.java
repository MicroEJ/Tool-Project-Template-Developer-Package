/*
 * Java
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

package com.microej.test.util;

/**
 * Centralizes test properties (deploy/Jenkins and target-specific).
 *
 * <p>
 * Target-specific values are read from system properties: the Gradle test task sets different system properties via the
 * {@code targetTestProperties()} function in {@code packaging/build.gradle.kts}.
 */
public final class TestProperties {

	// --- Jenkins properties ---

	/** System property key for the deploy tool name (e.g. {@code "jenkins"}). */
	public static final String PROPERTY_DEPLOY_NAME = "microejtool.deploy.name";

	/** System property key for the Jenkins trace file path. */
	public static final String PROPERTY_JENKINS_TRACE_FILE = "launch.test.trace.file";

	/** System property key for the Jenkins server URL. */
	public static final String PROPERTY_JENKINS_URL = "jenkins.url";

	/** System property key for the Jenkins job prefix. */
	public static final String PROPERTY_JENKINS_JOB_PREFIX = "jenkins.job.prefix";

	/** System property key for the Jenkins job name. */
	public static final String PROPERTY_JENKINS_JOB_NAME = "jenkins.job.name";

	/** System property key for the Jenkins user name. */
	public static final String PROPERTY_JENKINS_USER_NAME = "jenkins.user.name";

	/** System property key for the Jenkins user token. */
	public static final String PROPERTY_JENKINS_USER_TOKEN = "jenkins.user.token";

	/** System property key for the Jenkins action (e.g. {@code "stopJob"}). */
	public static final String PROPERTY_JENKINS_ACTION = "jenkins.action";

	// --- Jenkins values ---

	/** The deploy tool name, or {@code null} if not set. */
	public static final String DEPLOY_NAME = System.getProperty(PROPERTY_DEPLOY_NAME);

	/** The Jenkins trace file path, or {@code null} if not set. */
	public static final String JENKINS_TRACE_FILE = System.getProperty(PROPERTY_JENKINS_TRACE_FILE);

	/** The Jenkins server URL, or {@code null} if not set. */
	public static final String JENKINS_URL = System.getProperty(PROPERTY_JENKINS_URL);

	/** The Jenkins job prefix, or {@code null} if not set. */
	public static final String JENKINS_JOB_PREFIX = System.getProperty(PROPERTY_JENKINS_JOB_PREFIX);

	/** The Jenkins job name, or {@code null} if not set. */
	public static final String JENKINS_JOB_NAME = System.getProperty(PROPERTY_JENKINS_JOB_NAME);

	/** The Jenkins user name, or {@code null} if not set. */
	public static final String JENKINS_USER_NAME = System.getProperty(PROPERTY_JENKINS_USER_NAME);

	/** The Jenkins user token, or {@code null} if not set. */
	public static final String JENKINS_USER_TOKEN = System.getProperty(PROPERTY_JENKINS_USER_TOKEN);

	// --- Target-specific properties ---

	/** Gradle project name of the VEE Port (e.g. {@code "linux-x86"}, {@code "rt1170"}). */
	public static final String VEEPORT_PROJECT_NAME = System.getProperty("test.veeport.project.name", "linux-x86");

	/** Gradle project name of the kernel (e.g. {@code "kernel-linux-x86"}, {@code "kernel-rt1170"}). */
	public static final String KERNEL_PROJECT_NAME = System.getProperty("test.kernel.project.name",
			"kernel-" + VEEPORT_PROJECT_NAME);

	/** Relative path to the kernel sources inside {@code src/} (e.g. {@code "vee/kernel"}). */
	public static final String KERNEL_SRC_DIR = System.getProperty("test.kernel.src.dir", "vee/kernel");

	/**
	 * Relative path to the executable output folder produced by the kernel build (e.g.
	 * {@code "vee/kernel/build/application/executable/"}).
	 */
	public static final String EXECUTABLE_OUTPUT_DIR = System.getProperty("test.executable.output.dir",
			KERNEL_SRC_DIR + "/build/application/executable/");

	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

	private TestProperties() {
		// utility class
	}
}
