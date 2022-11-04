package org.jetbrains.dokka.base.transformers.pages.annotations

import com.intellij.util.containers.ComparatorUtil.max
import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.utilities.associateWithNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SinceKotlinTransformer(val context: DokkaContext) : DocumentableTransformer {
    class Version constructor(str: String) : Comparable<Version> {
        private val parts: List<Int> = str.split(".").map { it.toInt() }

        override fun compareTo(other: Version): Int {
            val i1 = parts.listIterator()
            val i2 = other.parts.listIterator()

            while (i1.hasNext() || i2.hasNext()) {
                val diff = (if (i1.hasNext()) i1.next() else 0) - (if (i2.hasNext()) i2.next() else 0)
                if (diff != 0) return diff
            }

            return 0
        }

        override fun toString(): String = parts.joinToString(".")
    }
    private val minSinceKotlin = mapOf(
        Platform.common to Version("1.2"),
        Platform.jvm to Version("1.0"),
        Platform.js to Version("1.1"),
        Platform.native to Version("1.3")
    )

    override fun invoke(original: DModule, context: DokkaContext) = original.transform() as DModule

    private fun <T : Documentable> T.transform(parent: SourceSetDependent<Version>? = null): Documentable {
        val versions = calculateVersions(parent)
        return when (this) {
            is DModule -> copy(
                packages = packages.map { it.transform() as DPackage }
            )

            is DPackage -> copy(
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty },
                typealiases = typealiases.map { it.transform() as DTypeAlias }
            )

            is DClass -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DEnum -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DInterface -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DObject -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DTypeAlias -> copy(
                documentation = appendSinceKotlin(versions)
            )

            is DAnnotation -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DFunction -> copy(
                documentation = appendSinceKotlin(versions)
            )

            is DProperty -> copy(
                documentation = appendSinceKotlin(versions)
            )

            is DParameter -> copy(
                documentation = appendSinceKotlin(versions)
            )

            else -> this.also { context.logger.warn("Unrecognized documentable $this while SinceKotlin transformation") }
        }
    }

    private fun Documentable.getVersion(sourceSet: DokkaConfiguration.DokkaSourceSet): Version? {
        val annotatedVersion =
            safeAs<WithExtraProperties<Documentable>>()?.extra?.get(Annotations)?.directAnnotations?.get(sourceSet)
                ?.find {
                    it.dri == DRI("kotlin", "SinceKotlin")
                }?.params?.get("version").safeAs<StringValue>()?.value

        val version = annotatedVersion?.let {
            Version(annotatedVersion.dropWhile { it == '"' }.dropLastWhile { it == '"' })
        }?.takeIf { version -> minSinceKotlin[sourceSet.analysisPlatform]?.let { version >= it } ?: true }
            ?: minSinceKotlin[sourceSet.analysisPlatform]
        return version
    }

    private fun Documentable.calculateVersions(parent: SourceSetDependent<Version>?): SourceSetDependent<Version> {
        return sourceSets.associateWithNotNull { sourceSet ->
            val version = getVersion(sourceSet)
            val parentVersion = parent?.get(sourceSet)
            if(version != null && parentVersion != null)
                 max(version, parentVersion)
            else
             version
        }
    }

    private fun Documentable.appendSinceKotlin(versions: SourceSetDependent<Version>) =
        sourceSets.fold(documentation) { acc, sourceSet ->

            val version = versions[sourceSet]

            val customTag = CustomTagWrapper(
                CustomDocTag(
                    listOf(
                        Text(
                            version.toString()
                        )
                    ),
                    name = MarkdownElementTypes.MARKDOWN_FILE.name
                ),
                "Since Kotlin"
            )
            if (acc[sourceSet] == null)
                acc + (sourceSet to DocumentationNode(listOf(customTag)))
            else
                acc.mapValues {
                    if (it.key == sourceSet) it.value.copy(
                        it.value.children + listOf(
                            customTag
                        )
                    ) else it.value
                }
        }
}
