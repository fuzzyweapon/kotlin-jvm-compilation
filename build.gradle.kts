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
				val ideSettingsDir = project.layout.projectDirectory.dir(".idea").asFile
				require(ideSettingsDir.exists() && ideSettingsDir.isDirectory)

				// Setup dependency conversion rules
				val moduleGroup = project.group
				val sharedModuleBase = "kotlin-mp-lib"
				// Pseudo-Code:TODO | remove
				// val scratch = """lib(?:-[^-:]+)*?(?:-(?:metadata|jvm|js))?"""
				val modulePrefix = "$sharedModuleBase-"
				val modules = "$modulePrefix[^-]+"
				val platforms = "jvm|js"
				val runtimeScope = "RUNTIME"
				val testScope = "TEST"
				val metadataSuffix = "metadata"
				// Rules (map/data value contents:  <regex-string> to <replace-string>)
				val dependencyConversionRules = mapOf(
					"Common Module Library" to ("""(<orderEntry type)="library" (name)="Gradle: ($moduleGroup):($modules)-$metadataSuffix:.*" level="project".*(/>)""" to "\$1=\"module\" module-\$2=\"\$3.\$4.commonMain\" \$5"),
					"Common Module Library (Test)" to ("""(<orderEntry type)="library" (scope="$testScope") (name)="Gradle: ($moduleGroup):(acornui.*)-$metadataSuffix:.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.commonTest\" \$2 \$6"),
					"Common Module Library (Runtime)" to ("""(<orderEntry type)="library" (scope="$runtimeScope") (name)="Gradle: ($moduleGroup):(acornui.*)-$metadataSuffix:.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.commonMain\" \$2 \$6"),
					"JVM & JS Module Library" to ("""(<orderEntry type)="library" (name)="Gradle: ($moduleGroup):($modules)-($platforms):[0-9\.]+" level="project" (/>)""" to "\$1=\"module\" module-\$2=\"\$3.\$4.\$5Main\" \$6"),
					"JVM & JS Module Library (Test)" to ("""(<orderEntry type)="library" (scope="$testScope") (name)="Gradle: ($moduleGroup):(acornui.*)-($platforms):.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.\$6Test\" \$2 \$7"),
					"JVM & JS Module Library (Runtime)" to ("""(<orderEntry type)="library" (scope="$runtimeScope") (name)="Gradle: ($moduleGroup):(acornui.*)-($platforms):.*".*(/>)""" to "\$1=\"module\" module-\$3=\"\$4.\$5.commonMain\" \$2 \$6")
				).mapKeys { it.key + " dependency conversion" }.map { Rule(it) }


				val slash = (File.separator).let { if (it == "\\") it.repeat(2) else it }
				val pathSeparator = File.pathSeparator
				val groupPathPart =
					moduleGroup.toString().takeIf { it.isNotBlank() }?.let { "$slash${it.replace(".", slash)}" } ?: ""

				/**
				 * Get a regex pattern module name string for any modules that use [moduleNameBase]
				 *
				 * Supports the following module name styles given a [moduleNameBase] `lib`
				 * - `lib`
				 * - `lib-module` & `lib-module-name`
				 * - `lib-metadata`, `lib-module-metadata`, & `lib-module-name-metadata`
				 * - _(same as above with passed [platforms])_
				 *
				 * It is not recommended to use this outside of a classpath context with IDE config files.
				 * @param moduleNameBase to match (e.g. `lib`)
				 * @param platforms to match (e.g. `jvm|js`)
				 **/
				fun classpathModuleName(moduleNameBase: String, platforms: String) =
					"""$moduleNameBase(?:-[^-${File.pathSeparator}$slash]+)*?(?:-(?:$metadataSuffix|$platforms))?"""

				val gradleCache = """\.gradle${slash}caches"""
				val localMaven = """\.m2${slash}repository"""
				fun artifactFilenamePattern(moduleNameBase: String, platforms: String) =
					"""${classpathModuleName(moduleNameBase, platforms)}-[\d\.]+\.jar"""

				fun artifactPath(moduleNameBase: String, platforms: String) =
					"""$pathSeparator?[^$pathSeparator]+(?=(?:$gradleCache|$localMaven)[^$pathSeparator]+?$slash${artifactFilenamePattern(
						moduleNameBase,
						platforms
					)})[^$pathSeparator]+"""

				/**
				 * Get a display friendly description of the classpath cleaning rule.
				 *
				 * @param moduleGroup takes the form of "com.example"
				 * @param moduleNamePrefix takes the form of "something-" where the full module name is
				 * "something-core", "something-core-utils", or "something"
				 */
				fun getCleanClasspathRuleName(moduleGroup: Any, moduleNamePrefix: String) =
					"Clean classpath - $moduleGroup:$moduleNamePrefix.* $metadataSuffix|$platforms jars"

				// Rules (map/data value contents:  <regex-string> to <replace-string>)
				val classpathCleanupRules = mapOf(
					getCleanClasspathRuleName(moduleGroup, modulePrefix) to (artifactPath(
						sharedModuleBase,
						"jvm"
					) to "")
				).map { Rule(it) }

				val imlFiles = ideSettingsDir.walkTopDown().filter { child: File ->
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
						// Extract classpath for more efficient and easier to maintain regex
						val classpathValueRegex = Regex("""(?<=<option name="classpath" value=")[^"]+""")
						val classpath = classpathValueRegex.find(newFileContents)?.value
						val newClasspath = classpath?.let {
							var tempNewCP = it
							classpathCleanupRules.forEach { rule ->
								tempNewCP = rule.process(tempNewCP) ?: tempNewCP
							}
							tempNewCP
						}
						newClasspath?.let {
							newFileContents = newFileContents.replace(classpathValueRegex, it.replace("$", "\\$"))
						}

						file.writeText(newFileContents)
					}
				}
			}
		}
	}
}

open class Rule(ruleEntry: Map.Entry<String, Pair<String, String>>) {
	private val shortname = ruleEntry.key
	private val pattern = Regex(ruleEntry.value.first)
	private val replacement = ruleEntry.value.second

	private fun renderMatchResult(result: String = "") = "\t$shortname [${result.toUpperCase()}]"

	fun process(contents: String?): String? {
		return contents?.let {
			if (pattern.containsMatchIn(it)) {
				logger.lifecycle(renderMatchResult(POSITIVE_RESULT_KEYPHRASE))
				pattern.replace(it, replacement)
			} else {
				logger.lifecycle(renderMatchResult(NO_MATCH_RESULT_KEYPHRASE))
				null
			}
		}
	}

	companion object {
		const val POSITIVE_RESULT_KEYPHRASE = "processed"
		const val NO_MATCH_RESULT_KEYPHRASE = "no match"
	}
}
