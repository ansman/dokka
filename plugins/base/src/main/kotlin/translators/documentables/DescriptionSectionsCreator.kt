package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentStyle
import org.jetbrains.dokka.pages.SimpleAttr
import org.jetbrains.dokka.pages.TextStyle
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


internal fun PageContentBuilder.DocumentableContentBuilder.descriptionSectionContent(
    documentable: Documentable,
    platforms: Set<DokkaConfiguration.DokkaSourceSet>
) {
    val descriptions = documentable.descriptions
    if (descriptions.any { it.value.root.children.isNotEmpty() }) {
        platforms.forEach { platform ->
            descriptions[platform]?.also {
                group(sourceSets = setOf(platform), styles = emptySet()) {
                    comment(it.root)
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.customTagSectionContent(
    documentable: Documentable,
    platforms: Set<DokkaConfiguration.DokkaSourceSet>,
    customTagContentProviders: List<CustomTagContentProvider>
) {
    val customTags = documentable.customTags
    if (customTags.isNotEmpty()) {
        platforms.forEach { platform ->
            customTags.forEach { (_, sourceSetTag) ->
                sourceSetTag[platform]?.let { tag ->
                    customTagContentProviders.filter { it.isApplicable(tag) }.forEach { provider ->
                        group(sourceSets = setOf(platform), styles = setOf(ContentStyle.KDocTag)) {
                            with(provider) {
                                contentForDescription(platform, tag)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.unnamedTagSectionContent(
    documentable: Documentable,
    platforms: Set<DokkaConfiguration.DokkaSourceSet>,
    toHeaderString: TagWrapper.() -> String
) {
    val unnamedTags =
        documentable.groupedTags.filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
            .values.flatten().groupBy { it.first }.mapValues { it.value.map { it.second } }
    if (unnamedTags.isNotEmpty()) {
        platforms.forEach { platform ->
            unnamedTags[platform]?.let { tags ->
                if (tags.isNotEmpty()) {
                    tags.groupBy { it::class }.forEach { (_, sameCategoryTags) ->
                        group(sourceSets = setOf(platform), styles = setOf(ContentStyle.KDocTag)) {
                            header(
                                level = KDOC_TAG_HEADER_LEVEL,
                                text = sameCategoryTags.first().toHeaderString(),
                                styles = setOf()
                            )
                            sameCategoryTags.forEach { comment(it.root, styles = setOf()) }
                        }
                    }
                }
            }
        }
    }
}


internal fun PageContentBuilder.DocumentableContentBuilder.contentForParams(
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
    tags: GroupedTags
) {
    if (tags.isNotEmptyForTag<Param>()) {
        val params = tags.withTypeNamed<Param>()
        val availablePlatforms = params.values.flatMap { it.keys }.toSet()

        header(KDOC_TAG_HEADER_LEVEL, "Parameters", kind = ContentKind.Parameters, sourceSets = availablePlatforms)
        group(
            extra = mainExtra + SimpleAttr.header("Parameters"),
            styles = setOf(ContentStyle.WithExtraAttributes),
            sourceSets = availablePlatforms
        ) {
            table(kind = ContentKind.Parameters, sourceSets = availablePlatforms) {
                availablePlatforms.forEach { platform ->
                    val possibleFallbacks = sourceSets.getPossibleFallbackSourcesets(platform)
                    params.mapNotNull { (_, param) ->
                        (param[platform] ?: param.fallback(possibleFallbacks))?.let {
                            row(sourceSets = setOf(platform), kind = ContentKind.Parameters) {
                                text(
                                    it.name,
                                    kind = ContentKind.Parameters,
                                    styles = mainStyles + setOf(ContentStyle.RowTitle, TextStyle.Underlined)
                                )
                                if (it.isNotEmpty()) {
                                    comment(it.root)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.contentForSeeAlso(
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
    tags: GroupedTags
) {
    if (tags.isNotEmptyForTag<See>()) {
        val seeAlsoTags = tags.withTypeNamed<See>()
        val availablePlatforms = seeAlsoTags.values.flatMap { it.keys }.toSet()

        header(KDOC_TAG_HEADER_LEVEL, "See also", kind = ContentKind.Comment, sourceSets = availablePlatforms)
        group(
            extra = mainExtra + SimpleAttr.header("See also"),
            styles = setOf(ContentStyle.WithExtraAttributes),
            sourceSets = availablePlatforms
        ) {
            table(kind = ContentKind.Sample) {
                availablePlatforms.forEach { platform ->
                    val possibleFallbacks = sourceSets.getPossibleFallbackSourcesets(platform)
                    seeAlsoTags.forEach { (_, see) ->
                        (see[platform] ?: see.fallback(possibleFallbacks))?.let { seeTag ->
                            row(
                                sourceSets = setOf(platform),
                                kind = ContentKind.Comment,
                                styles = this@group.mainStyles,
                            ) {
                                seeTag.address?.let { dri ->
                                    link(
                                        text = seeTag.name.removePrefix("${dri.packageName}."),
                                        address = dri,
                                        kind = ContentKind.Comment,
                                        styles = mainStyles + ContentStyle.RowTitle
                                    )
                                } ?: text(
                                    text = seeTag.name,
                                    kind = ContentKind.Comment,
                                    styles = mainStyles + ContentStyle.RowTitle
                                )
                                if (seeTag.isNotEmpty()) {
                                    comment(seeTag.root)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.contentForThrows(
    tags: GroupedTags
) {
    val throws = tags.withTypeNamed<Throws>()
    if (throws.isNotEmpty()) {
        val availablePlatforms = throws.values.flatMap { it.keys }.toSet()

        header(KDOC_TAG_HEADER_LEVEL, "Throws", sourceSets = availablePlatforms)
        availablePlatforms.forEach { sourceset ->
            table(
                kind = ContentKind.Main,
                sourceSets = setOf(sourceset),
                extra = mainExtra + SimpleAttr.header("Throws")
            ) {
                throws.entries.forEach { entry ->
                    entry.value[sourceset]?.let { throws ->
                        row(sourceSets = setOf(sourceset)) {
                            group(styles = mainStyles + ContentStyle.RowTitle) {
                                throws.exceptionAddress?.let {
                                    val className = it.takeIf { it.target is PointingToDeclaration }?.classNames
                                    link(text = className ?: entry.key, address = it)
                                } ?: text(entry.key)
                            }
                            if (throws.isNotEmpty()) {
                                comment(throws.root)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.contentForSamples(
    tags: GroupedTags
) {
    val samples = tags.withTypeNamed<Sample>()
    if (samples.isNotEmpty()) {
        val availablePlatforms = samples.values.flatMap { it.keys }.toSet()
        header(KDOC_TAG_HEADER_LEVEL, "Samples", kind = ContentKind.Sample, sourceSets = availablePlatforms)
        availablePlatforms.map { platformData ->
            val content = samples.filter { it.value.isEmpty() || platformData in it.value }
            group(
                sourceSets = setOf(platformData),
                kind = ContentKind.Sample,
                styles = setOf(TextStyle.Monospace, ContentStyle.RunnableSample)
            ) {
                content.forEach {
                    text(it.key)
                }
            }
        }
    }
}

private fun TagWrapper.isNotEmpty() = this.children.isNotEmpty()

private fun <V> Map<DokkaConfiguration.DokkaSourceSet, V>.fallback(sourceSets: List<DokkaConfiguration.DokkaSourceSet>): V? =
    sourceSets.firstOrNull { it in this.keys }.let { this[it] }

private fun Set<DokkaConfiguration.DokkaSourceSet>.getPossibleFallbackSourcesets(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    this.filter { it.sourceSetID in sourceSet.dependentSourceSets }

private inline fun <reified T : TagWrapper> GroupedTags.isNotEmptyForTag(): Boolean =
    this[T::class]?.isNotEmpty() ?: false

private val specialTags: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Param::class, See::class)