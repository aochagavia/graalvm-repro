import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    java
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.graalvm.truffle:truffle-api:25.0.0")
}

// GraalVM configuration
val graalvmVersion = "25"
val graalvmSemVer = "25.0.0"
val graalvmDir = file("${rootProject.projectDir}/gradle/jdk")

/**
 * Determines the GraalVM platform identifier based on the current system's OS and architecture.
 * @return A platform string in the format "{os}-{arch}" (e.g., "macos-aarch64", "linux-amd64")
 * @throws GradleException if the OS or architecture is unsupported
 */
fun getGraalVMPlatform(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    val osName = when {
        os.contains("mac") || os.contains("darwin") -> "macos"
        os.contains("win") -> "windows"
        os.contains("linux") -> "linux"
        else -> throw GradleException("Unsupported OS: $os")
    }

    val archName = when {
        arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
        arch.contains("x86_64") || arch.contains("amd64") -> "x64"
        else -> throw GradleException("Unsupported architecture: $arch")
    }

    return "${osName}-${archName}"
}

/**
 * Determines the file extension for GraalVM downloads based on the current operating system.
 * @return A file extension string: "zip" for Windows, "tar.gz" for macOS/Linux
 * @throws GradleException if the OS is unsupported
 */
fun getGraalVMExtension(): String {
    val os = System.getProperty("os.name").lowercase()

    return when {
        os.contains("win") -> "zip"
        os.contains("mac") || os.contains("darwin") || os.contains("linux") -> "tar.gz"
        else -> throw GradleException("Unsupported OS: $os")
    }
}

/**
 * Determines the file extension for the native-image executable based on the operating system.
 * @return A file extension string: ".cmd" for Windows, empty string for macOS/Linux
 * @throws GradleException if the OS is unsupported
 */
fun getGraalNativeImageExtension(): String {
    val os = System.getProperty("os.name").lowercase()

    return when {
        os.contains("win") -> ".cmd"
        os.contains("mac") || os.contains("darwin") || os.contains("linux") -> ""
        else -> throw GradleException("Unsupported OS: $os")
    }
}

/**
 * Locates the GraalVM installation directory within the gradle/jdk folder.
 * Handles platform-specific directory structures (e.g., macOS's Contents/Home subdirectory).
 * @return The GraalVM home directory if found, null otherwise
 */
fun getGraalVMHome(): File? {
    val existingGraalVM = graalvmDir.listFiles()?.find {
        it.isDirectory && it.name.contains("graalvm") && it.name.contains(graalvmVersion)
    } ?: return null

    val platform = getGraalVMPlatform()
    return if (platform.startsWith("macos") && file("$existingGraalVM/Contents/Home").exists()) {
        file("$existingGraalVM/Contents/Home")
    } else {
        existingGraalVM
    }
}

val downloadGraalVM by tasks.registering {
    group = "build setup"
    description = "Downloads GraalVM $graalvmSemVer for native image compilation"

    doLast {
        if (getGraalVMHome() != null) {
            println("GraalVM already exists at: ${getGraalVMHome()}")
            return@doLast
        }

        graalvmDir.mkdirs()

        val platform = getGraalVMPlatform()
        val extension = getGraalVMExtension()
        val downloadUrl =
            "https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${graalvmSemVer}/graalvm-community-jdk-${graalvmSemVer}_${platform}_bin.$extension"
        val tempFile = file("$graalvmDir/graalvm-download.$extension")

        println("Downloading GraalVM from: $downloadUrl")

        URL(downloadUrl).openStream().use { input ->
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        println("Extracting GraalVM...")

        exec {
            commandLine("tar", "-xzf", tempFile.absolutePath, "-C", graalvmDir.absolutePath)
        }

        tempFile.delete()

        println("GraalVM installed to: ${getGraalVMHome()}")
    }
}

// Custom task to build native image using downloaded GraalVM
tasks.register<Exec>("buildNativeLibrary") {
    group = "build"
    description = "Builds native executable using GraalVM $graalvmSemVer"

    dependsOn(downloadGraalVM, tasks.jar)

    // Compute and set the command line
    doFirst {
        val graalHome = getGraalVMHome()
            ?: throw GradleException("GraalVM not found! Please run './gradlew :tooling:cli:downloadGraalVM' first")

        val nativeImageExe = file("${graalHome}/bin/native-image" + getGraalNativeImageExtension())
        if (!nativeImageExe.exists()) {
            throw GradleException("native-image not found at $nativeImageExe")
        }

        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val kotlinRuntimeJarFile = configurations.runtimeClasspath.get().find { it -> it.path.contains("org.jetbrains.kotlin") }!!

        commandLine(
            nativeImageExe.absolutePath,
            "--shared",
            "-cp", "${jarFile.absolutePath}:${kotlinRuntimeJarFile.absolutePath}",
            "-o", "kotlin-lib",
        )
    }
}
