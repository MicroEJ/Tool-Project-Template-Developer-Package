/*
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

allprojects {
    group = providers.systemProperty("package.group.name").get()
    version = providers.systemProperty("package.version").get()
}
