group = "com.example"

tasks {
	val wrapper by existing(Wrapper::class) {
		distributionType = Wrapper.DistributionType.ALL
		gradleVersion = "5.4.1"
	}

	// Setup IDE composite support tasks if developer has opted in by setting the system property
	if (System.getProperty("composite.intellij")?.toBoolean() == true) {

		val convertCompositeDependencies by registering(DefaultTask::class) {
			group = "composite"
			description = "Converts included builds' IDE dependencies from libraries (.jar) to modules."

			doLast {
				val ideMetadataDir = project.layout.projectDirectory.dir(".idea").asFile
				require(ideMetadataDir.exists() && ideMetadataDir.isDirectory)

				// Setup dependency conversion rules
				val moduleGroup = project.group
				val module = "kotlin-mp-lib"
				val platforms = "jvm|js"
				val runtimeScope = "RUNTIME"
				val testScope = "TEST"
				val metadataSuffix = "metadata"
				// Rules (map/data value contents:  <regex-string> to <replace-string>)
				val dependencyConversionRules = mapOf(
					"Common Module Library" to ("""(<orderEntry type)="library" (name)="Gradle: ($moduleGroup):($module)-$metadataSuffix:.*" level="project".*(/>)""" to "\$1=\"module\" module-\$2=\"\$3.\$4.commonMain\" \$5"),
					"Common Module Library (Test)" to ("""(<orderEntry type)="library" (scope="$testScope") (name)="Gradle: ($moduleGroup):(acornui.*)-$metadataSuffix:.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.commonTest\" \$2 \$6"),
					"Common Module Library (Runtime)" to ("""(<orderEntry type)="library" (scope="$runtimeScope") (name)="Gradle: ($moduleGroup):(acornui.*)-$metadataSuffix:.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.commonMain\" \$2 \$6"),
					"JVM & JS Module Library" to ("""(<orderEntry type)="library" (name)="Gradle: ($moduleGroup):($module)-($platforms):[0-9\.]+" level="project" (/>)""" to "\$1=\"module\" module-\$2=\"\$3.\$4.\$5Main\" \$6"),
					"JVM & JS Module Library (Test)" to ("""(<orderEntry type)="library" (scope="$testScope") (name)="Gradle: ($moduleGroup):(acornui.*)-($platforms):.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.\$6Test\" \$2 \$7"),
					"JVM & JS Module Library (Runtime)" to ("""(<orderEntry type)="library" (scope="$runtimeScope") (name)="Gradle: ($moduleGroup):(acornui.*)-($platforms):.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.commonMain\" \$2 \$6")
				).mapKeys { it.key + " dependency conversion" }.map { Rule(it) }


				val slash = (File.separator).let { if (it == "\\") it.repeat(2) else it }
				val localMavenRepoPathPart = """$slash\.m2${slash}repository"""
				val groupPathPart =
					moduleGroup.toString().takeIf { it.isNotBlank() }?.let { "$slash${it.replace(".", slash)}" } ?: ""

				fun getPlatformArtifactFileName(platformSuffix: String) = """$module-$platformSuffix-[\d\.]+\.jar"""
				fun getArtifactPath(platformSuffix: String) =
					"""[^:]*$groupPathPart$slash$module-$platformSuffix$slash[\d\.]+$slash${getPlatformArtifactFileName(
						platformSuffix
					)}:?"""

				fun getCleanClasspathRuleName(platformSuffix: String) =
					"Clean classpath - $module-$platformSuffix jar"

				fun getCleanClasspathPattern(platformSuffix: String) =
					"""(<option name="classpath" value=".*?)(${getArtifactPath(platformSuffix)})(.*)"""
				// Rules (map/data value contents:  <regex-string> to <replace-string>)
				val classpathCleanupRules = mapOf(
					"metadata".let { getCleanClasspathRuleName(it) to (getCleanClasspathPattern(it) to """$1$3""") },
					"jvm".let { getCleanClasspathRuleName(it) to (getCleanClasspathPattern(it) to """$1$3""") }
				).map { Rule(it) }

				val imlFiles = ideMetadataDir.walkTopDown().filter { child: File ->
					child.exists() && child.isFile && child.extension == "iml"
				}

				var testFileContents = ""
				imlFiles.forEach { file: File ->
					val fileContents = file.readText()

					if (!fileContents.isNullOrBlank()) {
						logger.lifecycle("${file.name}...")
						var newFileContents = fileContents

						logger.quiet(
							"Converting ${project.name}'s IDE library dependencies (via a composite build setup) " +
									"to IDE module dependencies."
						)
						dependencyConversionRules.forEach {
							it.process(newFileContents)?.let { newFileContents = it }
						}

						logger.quiet("Cleaning up library jars from classpath...")
						classpathCleanupRules.forEach {
							it.process(newFileContents)?.let { newFileContents = it }
						}

						file.writeText(newFileContents)
					}
				}

				// Remove library and artifact entries
				// logger.quiet("Cleaning up project library and artifact entries...")
			}
		}
	}
}

open class Rule(ruleEntry: Map.Entry<String, Pair<String, String>>) {
	private val shortname = ruleEntry.key
	private val pattern = Regex(ruleEntry.value.first)
	private val replacement = ruleEntry.value.second

	private fun renderMatchResult(result: String = "") = "\t$shortname [${result.toUpperCase()}]"

	fun process(contents: String): String? {
		return if (pattern.containsMatchIn(contents)) {
			logger.lifecycle(renderMatchResult(POSITIVE_RESULT_KEYPHRASE))
			pattern.replace(contents, replacement)
		} else {
			logger.lifecycle(renderMatchResult(NO_MATCH_RESULT_KEYPHRASE))
			null
		}
	}

	companion object {
		const val POSITIVE_RESULT_KEYPHRASE = "processed"
		const val NO_MATCH_RESULT_KEYPHRASE = "no match"
	}
}
