/*
 * Java
 *
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import static com.microej.test.util.TestUtil.prepareTestedPackage;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

public abstract class PackagingTest {

	static File srcDir;

	@BeforeAll
	public static void setUpAll() throws IOException {
		File testPackageDir = prepareTestedPackage();
		Assertions.assertTrue(testPackageDir.exists());
		srcDir = new File(testPackageDir, "src");
	}
}
