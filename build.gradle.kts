import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
	idea
	eclipse
	`maven-publish`
	id("net.neoforged.moddev") version "2.0.78"
}

val minecraft_version: String by project
val neoforge_version: String by project
val version_major: String by project
val version_minor: String by project
val version_patch: String by project
val jei_version: String by project
val crafttweaker_version: String by project
val top_version: String by project
val kiwi_version: String by project
val architecture_version: String by project
val astage_version: String by project
val ftblibrary_version: String by project
val ftbteam_version: String by project
val jade_version: String by project
val kubejs_version: String by project

version = "$version_major.$version_minor.$version_patch"
group = "snownee.research"

base {
	archivesName = "ResearchTable"
}

System.getenv("BUILD_NUMBER")?.let {
	version = "$version-build$it"
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
    maven {
        name = "latviandevReleases"
        url = uri("https://maven.latvian.dev/releases")
    }
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.rtyley")
        }
    }
	maven {
		name = "CraftTweaker"
		url = uri("https://maven.blamejared.com/")
	}

	maven {
		name = "The One Probe"
		url = uri("https://maven.k-4u.nl")
	}

	maven {
		name = "curse maven"
		url = uri("https://www.cursemaven.com")
	}
}

neoForge {
	version = neoforge_version

	runs {
		create("client") {
			client()
		}
		create("server") {
			server()
			programArgument("--nogui")
		}
		create("data") {
			data()
		}
	}

	mods {
		create("researchtable") {
			sourceSet(sourceSets["main"])
		}
	}
}

dependencies {
	compileOnly("mezz.jei:jei-$minecraft_version-common-api:$jei_version")
	compileOnly("mezz.jei:jei-$minecraft_version-neoforge-api:$jei_version")
	runtimeOnly("mezz.jei:jei-$minecraft_version-neoforge:$jei_version")

	runtimeOnly("curse.maven:kiwi-303657:$kiwi_version")
	implementation("curse.maven:architectury-api-419699:$architecture_version")
	implementation("curse.maven:astage-1120180:$astage_version")
	implementation("curse.maven:ftb-library-404465:$ftblibrary_version")
	implementation("curse.maven:ftb-team-404468:$ftbteam_version")
	implementation("curse.maven:jade-324717:$jade_version")

	implementation("mcjty.theoneprobe:theoneprobe:$top_version")
	implementation("com.blamejared.crafttweaker:CraftTweaker-neoforge-$minecraft_version:$crafttweaker_version")
    implementation("dev.latvian.mods:kubejs-neoforge:$kubejs_version")
}

tasks.processResources {
	val replaceProperties = mapOf(
		"minecraft_version" to minecraft_version,
		"neoforge_version" to neoforge_version,
		"mod_version" to project.version,
	)
	inputs.properties(replaceProperties)

	filesMatching("META-INF/neoforge.mods.toml") {
		expand(replaceProperties)
	}
}

tasks.jar {
	manifest {
		attributes(
			"Maven-Artifact" to "${project.group}:${base.archivesName.get()}:${project.version}",
			"Timestamp" to System.currentTimeMillis(),
		)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 21
}

tasks.javadoc {
	options.encoding = "UTF-8"
	(options as StandardJavadocDocletOptions).charSet = "UTF-8"
}

publishing {
	publications {
		create<MavenPublication>("mod") {
			groupId = project.group.toString()
			artifactId = base.archivesName.get()
			version = project.version.toString()
			artifact(tasks.jar)
		}
	}
	repositories {
        maven {
            name = "nexus"
            url = uri("https://maven.lyuxcfiles.top/repository/maven-releases/")
            credentials {
                username = (findProperty("maven_username") as String?) ?: System.getenv("MAVEN_USERNAME")
                password = (findProperty("maven_password") as String?) ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
