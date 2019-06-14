plugins {
	id("kotlin-multiplatform") version "1.3.31"
}

repositories {
	mavenLocal()
	jcenter()
	maven("https://dl.bintray.com/kotlin/ktor")
	mavenCentral()
}

val ktor_version = "1.1.3"
val logback_version = "1.2.3"
val samples_version = "0.0.1"

kotlin {
	// For parity, but not confirmed this is a part of the issue yet.
	jvm {
		compilations.all {
			kotlinOptions {
				jvmTarget = "1.8"
			}
		}
	}
	js {
		compilations.all {
			kotlinOptions {
				languageVersion = "1.3"
				moduleKind = "umd"
				sourceMap = true
				metaInfo = true
			}
		}
	}

	sourceSets {
		commonMain {
			dependencies {
				implementation(kotlin("stdlib-common"))
				implementation("com.example:kotlin-mp-lib-metadata:$samples_version")
			}
		}
		commonTest {
			dependencies {
				implementation(kotlin("test-common"))
				implementation(kotlin("test-annotations-common"))
			}
		}
		jvm().compilations["main"].defaultSourceSet {
			dependencies {
				implementation(kotlin("stdlib-jdk8"))
				implementation("io.ktor:ktor-server-netty:$ktor_version")
				implementation("io.ktor:ktor-html-builder:$ktor_version")
				implementation("ch.qos.logback:logback-classic:$logback_version")
				implementation("com.example:kotlin-mp-lib-jvm:$samples_version")
			}
		}
		jvm().compilations["test"].defaultSourceSet {
			dependencies {
				implementation(kotlin("test"))
				implementation(kotlin("test-junit"))
			}
		}
		js().compilations["main"].defaultSourceSet {
			dependencies {
				implementation(kotlin("stdlib-js"))
				implementation("com.example:kotlin-mp-lib-js:$samples_version")
			}
		}
		js().compilations["test"].defaultSourceSet {
			dependencies {
				implementation(kotlin("test-js"))
			}
		}
	}
}

val webFolder = File(project.buildDir, "../src/jsMain/web")
val jsCompilations = kotlin.js().compilations

tasks {
	val populateWebFolder by registering(Copy::class) {
		val jsMainClasses by existing
		dependsOn(jsMainClasses)
		doLast {
			val jsMain = jsCompilations["main"]
			from(jsMain.output)
			from(jsMain.defaultSourceSet.resources.srcDirs)
			jsCompilations["test"].runtimeDependencyFiles.forEach {
				if (it.exists() && !it.isDirectory()) {
					from(zipTree(it.absolutePath).matching { include("*.js") })
				}
			}
		}

	}

	val jsJar by existing {
		dependsOn(populateWebFolder)
	}

	val run by registering(JavaExec::class) {
		val jvmMainClasses by existing
		dependsOn(jvmMainClasses, jsJar)
		val jvmRuntimeClasspath by configurations.existing
		classpath({ arrayOf(
			kotlin.jvm().compilations["main"].output.allOutputs.files,
			jvmRuntimeClasspath
		) })
		args = mutableListOf()
	}
}
