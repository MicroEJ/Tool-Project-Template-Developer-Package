/*
 * Kotlin
 *
 * Copyright 2024-2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLConnection
import java.net.URLEncoder
import java.util.*

open class AppConnectExtension {
    private var hostProvider: Provider<String>? = null
    var host: String? = null
        get() = hostProvider?.orNull ?: field

    private var portProvider: Provider<String>? = null
    var port: Int = 4001
        get() = portProvider?.orNull?.toInt() ?: field

    private var timeoutProvider: Provider<String>? = null
    var timeout: Int = -1
        get() = timeoutProvider?.orNull?.toInt() ?: field

    fun setHost(hostProvider: Provider<String>) {
        this.hostProvider = hostProvider
    }

    fun setPort(portProvider: Provider<String>) {
        this.portProvider = portProvider
    }

    fun setTimeout(timeoutProvider: Provider<String>) {
        this.timeoutProvider = timeoutProvider
    }
}

abstract class FeatureDeployTask : DefaultTask() {
    init {
        group = "MicroEJ"
        description = "Deploy a Feature to a device"
    }

    @TaskAction
    fun deploy() {
        // Locate app file and metadata
        val featureFile = project.tasks.getByName("buildFeature")
            .outputs.files.filter {
                it.extension == "fo"
            }.singleFile
        val featureProperties = Properties()
        val resourcesDir = project.layout.projectDirectory.dir("src/main/resources").asFile
        if (resourcesDir.exists()) {
            featureProperties.apply {
                resourcesDir.listFiles { file -> file.extension == "kf" }
                    ?.firstOrNull()
                    ?.inputStream()?.use {
                        load(it)
                    }
            }
        }
        if (featureProperties.isEmpty) {
            val featureResourceDir = project.tasks.getByName("generateApplicationWrapper")
                .outputs.files.filter {
                    it.isDirectory && it.name == "feature-resources"
                }.singleFile
            featureProperties.apply {
                featureResourceDir.listFiles { file -> file.extension == "kf" }
                    ?.firstOrNull()
                    ?.inputStream()?.use {
                        load(it)
                    }
            }
        }
        val appName = featureProperties.getProperty("name") ?: error("Can't load feature name from kf")

        val appConnect = project.extensions.getByName("appConnect") as AppConnectExtension
        val host = appConnect.host
            ?: error(
                """
                AppConnect server host is mandatory, you can configure it using the `appConnect` configuration:
                appConnect {
                  host = "x.x.x.x"
                }
                """.trimIndent()
            )
        val port = appConnect.port

        val urlParams = mapOf(
            "force" to "true",
            "start" to "true",
            "name" to appName
        ).entries.joinToString(prefix = "?", separator = "&") { "${it.key.urlEncode()}=${it.value.urlEncode()}" }

        println("Deploying $appName to board at $host:$port")
        // Construct install HTTP request
        val url = URI("http://$host:$port/api/app/install$urlParams").toURL()
        val connection = url.openConnection() as HttpURLConnection
        if (appConnect.timeout >= 0) {
            connection.connectTimeout = appConnect.timeout
        }
        val boundary = "Boundary-" + System.currentTimeMillis()
        setupConnection(connection, boundary)

        connection.outputStream.use { outputStream ->
            writeFileField(outputStream, boundary, "binary", featureFile)
            outputStream.write("--$boundary--\r\n".toByteArray())
            handleResponse(connection)
        }
    }

    // Extension function to encode URL parameters
    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    // Set up the connection
    private fun setupConnection(connection: HttpURLConnection, boundary: String) {
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            doOutput = true
        }
    }

    // Helper to write file field
    private fun writeFileField(outputStream: OutputStream, boundary: String, name: String, file: File) {
        outputStream.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n".toByteArray())
        outputStream.write("Content-Type: ${URLConnection.guessContentTypeFromName(file.name)}\r\n\r\n".toByteArray())
        file.inputStream().copyTo(outputStream)
        outputStream.write("\r\n".toByteArray())
    }

    // Handle response from server
    private fun handleResponse(connection: HttpURLConnection) {
        connection.run {
            try {
                when (responseCode) {
                    in 200..299 -> {
                        println("Deployment Successful! Response Code: $responseCode")
                        println("Server Response: ${inputStream.bufferedReader().use { it.readText() }}")
                    }

                    else -> {
                        System.err.println("Deployment Failed. Response Code: $responseCode")
                        System.err.println("Error Details: ${errorStream.bufferedReader().use { it.readText() }}")
                    }
                }
            } catch (e: Exception) {
                System.err.println("An error occurred while handling the server response: ${e.message}")
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
    }
}

class FeatureDeployPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.add("appConnect", AppConnectExtension::class.java)

        project.tasks.register<FeatureDeployTask>("featureDeploy") {
            val buildFeatureTask = project.tasks.named("buildFeature")
            dependsOn(buildFeatureTask)
        }
    }
}
