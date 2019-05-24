buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    id 'kotlin-multiplatform' version '1.3.31'
}

repositories {
    mavenLocal()
    jcenter()
    maven { url "https://dl.bintray.com/kotlin/ktor" }
    mavenCentral()
}

group = "com.example"

def ktor_version = '1.1.3'
def logback_version = '1.2.3'
def samples_version = '0.0.1'

kotlin {
    // For parity, but not confirmed this is a part of the issue yet.
    jvm() {
        compilations.all {
            kotlinOptions {
                jvmTarget '1.8'
            }
        }
    }
    js() {
        compilations.all {
            kotlinOptions {
                languageVersion '1.3'
                moduleKind 'umd'
                sourceMap true
                metaInfo true
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation kotlin('stdlib-common')
                implementation "com.example:kotlin-mp-lib-metadata:$samples_version"
            }
        }
        commonTest {
            dependencies {
                implementation kotlin('test-common')
                implementation kotlin('test-annotations-common')
            }
        }
        jvmMain {
            dependencies {
                implementation kotlin('stdlib-jdk8')
                implementation "io.ktor:ktor-server-netty:$ktor_version"
                implementation "io.ktor:ktor-html-builder:$ktor_version"
                implementation "ch.qos.logback:logback-classic:$logback_version"
                implementation "com.example:kotlin-mp-lib-jvm:$samples_version"
            }
        }
        jvmTest {
            dependencies {
                implementation kotlin('test')
                implementation kotlin('test-junit')
            }
        }
        jsMain {
            dependencies {
                implementation kotlin('stdlib-js')
                implementation "com.example:kotlin-mp-lib-js:$samples_version"
            }
        }
        jsTest {
            dependencies {
                implementation kotlin('test-js')
            }
        }
    }
}

def webFolder = new File(project.buildDir, "../src/jsMain/web")
def jsCompilations = kotlin.targets.js.compilations

task populateWebFolder(dependsOn: [jsMainClasses]) {
    doLast {
        copy {
            from jsCompilations.main.output
            from kotlin.sourceSets.jsMain.resources.srcDirs
            jsCompilations.test.runtimeDependencyFiles.each {
                if (it.exists() && !it.isDirectory()) {
                    from zipTree(it.absolutePath).matching { include '*.js' }
                }
            }
            into webFolder
        }
    }
}

jsJar.dependsOn(populateWebFolder)

task run(type: JavaExec, dependsOn: [jvmMainClasses, jsJar]) {
    main = "sample.SampleJvmKt"
    classpath { [
            kotlin.targets.jvm.compilations.main.output.allOutputs.files,
            configurations.jvmRuntimeClasspath,
    ] }
    args = []
}