package transformers

import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class ReportUndocumentedTransformerTest : AbstractCoreTest() {
    @Test
    fun `undocumented class gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |class X
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport(Regex("init"))
                assertSingleUndocumentedReport(Regex("""sample/X/"""))
            }
        }
    }

    @Test
    fun `undocumented non-public class does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |internal class X
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `undocumented function gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |/** Documented */
            |class X {
            |    fun x()
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
                assertSingleUndocumentedReport(Regex("X/x"))
            }
        }
    }

    @Test
    fun `undocumented property gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |/** Documented */
            |class X {
            |    val x: Int = 0
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
                assertSingleUndocumentedReport(Regex("X/x"))
            }
        }
    }

    @Test
    fun `undocumented primary constructor does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |/** Documented */
            |class X(private val x: Int) {
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Disabled
    @Test
    fun `undocumented secondary constructor gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |/** Documented */
            |class X {
            |   constructor(unit: Unit) : this()
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
                assertSingleUndocumentedReport(Regex("X.*init.*Unit"))
            }
        }
    }

    @Test
    fun `undocumented inherited function does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |/** Documented */
            |open class A {
            |   fun a() = Unit
            |}
            |
            |/** Documented */
            |class B : A()
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport(Regex("B"))
                assertSingleUndocumentedReport(Regex("A.*a"))
            }
        }
    }

    @Test
    fun `undocumented inherited property does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |/** Documented */
            |open class A {
            |   val a = Unit
            |}
            |
            |/** Documented */
            |class B : A()
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport(Regex("B"))
                assertSingleUndocumentedReport(Regex("A.*a"))
            }
        }
    }

    @Test
    fun `overridden function does not get reported when super is documented`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package sample
            |import kotlin.Exception
            |
            |/** Documented */
            |open class A {
            |   /** Documented */
            |   fun a() = Unit
            |}
            |
            |/** Documented */
            |class B : A() {
            |    override fun a() = throw Exception()
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `overridden property does not get reported when super is documented`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package sample
            |import kotlin.Exception
            |
            |/** Documented */
            |open class A {
            |   /** Documented */
            |   open val a = 0
            |}
            |
            |/** Documented */
            |class B : A() {
            |    override val a = 1
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `report disabled by pass configuration`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = false
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |class X
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `report enabled by package configuration`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    perPackageOptions += packageOptions(
                        prefix = "sample",
                        reportUndocumented = true,
                    )
                    reportUndocumented = false
                    sourceRoots = listOf("src/main/kotlin/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt    
            |package sample
            |
            |class X
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
            }
        }
    }

    @Test
    fun `report enabled by more specific package configuration`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    perPackageOptions += packageOptions(
                        prefix = "sample",
                        reportUndocumented = false,
                    )
                    perPackageOptions += packageOptions(
                        prefix = "sample.enabled",
                        reportUndocumented = true,
                    )
                    reportUndocumented = false
                    sourceRoots = listOf("src/main/kotlin/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/disabled/Disabled.kt
            |package sample.disabled
            |class Disabled
            |
            |/src/main/kotlin/sample/enabled/Enabled.kt    
            |package sample.enabled
            |class Enabled
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("Enabled"))
                assertNumberOfUndocumentedReports(1)
            }
        }
    }

    @Test
    fun `report disabled by more specific package configuration`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    perPackageOptions += packageOptions(
                        prefix = "sample",
                        reportUndocumented = true,
                    )
                    perPackageOptions += packageOptions(
                        prefix = "sample.disabled",
                        reportUndocumented = false,
                    )
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/kotlin/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/disabled/Disabled.kt
            |package sample.disabled
            |class Disabled
            |
            |/src/main/kotlin/sample/enabled/Enabled.kt    
            |package sample.enabled
            |class Enabled
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("Enabled"))
                assertNumberOfUndocumentedReports(1)
            }
        }
    }

    @Test
    fun `multiplatform undocumented class gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.common.toString()
                    sourceSetID = "commonMain"
                    displayName = "commonMain"
                    sourceRoots = listOf("src/commonMain/kotlin")
                }

                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.jvm.toString()
                    sourceSetID = "jvmMain"
                    displayName = "jvmMain"
                    sourceRoots = listOf("src/jvmMain/kotlin")
                    dependentSourceSets = listOf("commonMain")
                }
            }
        }

        testInline(
            """
            |/src/commonMain/kotlin/sample/Common.kt
            |package sample
            |expect class X
            |
            |/src/jvmMain/kotlin/sample/JvmMain.kt    
            |package sample
            |actual class X
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNumberOfUndocumentedReports(2, Regex("X"))
                assertSingleUndocumentedReport(Regex("X.*jvmMain"))
                assertSingleUndocumentedReport(Regex("X.*commonMain"))
            }
        }
    }

    @Test
    fun `multiplatform undocumented class does not get reported if expect is documented`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.common.toString()
                    sourceSetID = "commonMain"
                    displayName = "commonMain"
                    sourceRoots = listOf("src/commonMain/kotlin")
                }

                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.jvm.toString()
                    sourceSetID = "jvmMain"
                    displayName = "jvmMain"
                    sourceRoots = listOf("src/jvmMain/kotlin")
                    dependentSourceSets = listOf("commonMain")
                }
            }
        }

        testInline(
            """
            |/src/commonMain/kotlin/sample/Common.kt
            |package sample
            |/** Documented */
            |expect class X
            |
            |/src/jvmMain/kotlin/sample/JvmMain.kt    
            |package sample
            |actual class X
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNumberOfUndocumentedReports(0)
            }
        }
    }

    @Test
    fun `multiplatform undocumented function gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.common.toString()
                    sourceSetID = "commonMain"
                    displayName = "commonMain"
                    sourceRoots = listOf("src/commonMain/kotlin")
                }

                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.jvm.toString()
                    sourceSetID = "jvmMain"
                    displayName = "jvmMain"
                    sourceRoots = listOf("src/jvmMain/kotlin")
                    dependentSourceSets = listOf("commonMain")
                }

                pass {
                    reportUndocumented = true
                    analysisPlatform = Platform.native.toString()
                    sourceSetID = "macosMain"
                    displayName = "macosMain"
                    sourceRoots = listOf("src/macosMain/kotlin")
                    dependentSourceSets = listOf("commonMain")
                }
            }
        }

        testInline(
            """
            |/src/commonMain/kotlin/sample/Common.kt
            |package sample
            |expect fun x()
            |
            |/src/macosMain/kotlin/sample/MacosMain.kt    
            |package sample
            |/** Documented */
            |actual fun x() = Unit
            |
            |/src/jvmMain/kotlin/sample/JvmMain.kt    
            |package sample
            |actual fun x() = Unit
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNumberOfUndocumentedReports(2)
                assertSingleUndocumentedReport(Regex("x.*commonMain"))
                assertSingleUndocumentedReport(Regex("x.*jvmMain"))
            }
        }
    }

    @Test
    fun `java undocumented class gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Test.java    
            |package sample
            |public class Test { }
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport(Regex("init"))
                assertSingleUndocumentedReport(Regex("""Test"""))
                assertNumberOfUndocumentedReports(1)
            }
        }
    }

    @Test
    fun `java undocumented non-public class does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Test.java    
            |package sample
            |class Test { }
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `java undocumented constructor does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Test.java    
            |package sample
            |/** Documented */
            |public class Test {
            |   public Test() {
            |   }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `java undocumented method gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/X.java    
            |package sample
            |/** Documented */
            |public class X {
            |   public void x { }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
                assertSingleUndocumentedReport(Regex("X.*x"))
                assertNumberOfUndocumentedReports(1)
            }
        }
    }

    @Test
    fun `java undocumented property gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/X.java    
            |package sample
            |/** Documented */
            |public class X {
            |   public int x = 0;
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
                assertSingleUndocumentedReport(Regex("X.*x"))
                assertNumberOfUndocumentedReports(1)
            }
        }
    }

    @Test
    fun `java undocumented inherited method gets reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Super.java    
            |package sample
            |/** Documented */
            |public class Super {
            |    public void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample
            |/** Documented */
            |public class X extends Super {
            |    public void x() {}
            |}
            |
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertSingleUndocumentedReport(Regex("X"))
                assertSingleUndocumentedReport(Regex("X.*x"))
                assertSingleUndocumentedReport(Regex("Super.*x"))
                assertNumberOfUndocumentedReports(2)
            }
        }
    }

    @Test
    fun `java documented inherited method does not get reported`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Super.java    
            |package sample
            |/** Documented */
            |public class Super {
            |    /** Documented */
            |    public void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample
            |/** Documented */
            |public class X extends Super {
            |    
            |}
            |
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    @Test
    fun `java overridden function does not get reported when super is documented`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    reportUndocumented = true
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Super.java    
            |package sample
            |/** Documented */
            |public class Super {
            |    /** Documented */
            |    public void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample
            |/** Documented */
            |public class X extends Super {
            |    @Override
            |    public void x() {}
            |}
            |
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = {
                assertNoUndocumentedReport()
            }
        }
    }

    private fun assertNumberOfUndocumentedReports(expectedReports: Int, regex: Regex = Regex(".")) {
        val reports = logger.warnMessages
            .filter { it.startsWith("Undocumented:") }
        val matchingReports = reports
            .filter { it.contains(regex) }

        assertEquals(
            expectedReports, matchingReports.size,
            "Expected $expectedReports report of documented code ($regex).\n" +
                    "Found matching reports: $matchingReports\n" +
                    "Found reports: $reports"
        )
    }

    private fun assertSingleUndocumentedReport(regex: Regex) {
        assertNumberOfUndocumentedReports(1, regex)
    }

    private fun assertNoUndocumentedReport(regex: Regex) {
        assertNumberOfUndocumentedReports(0, regex)
    }

    private fun assertNoUndocumentedReport() {
        assertNoUndocumentedReport(Regex("."))
    }

    private fun packageOptions(
        prefix: String,
        reportUndocumented: Boolean?,
        includeNonPublic: Boolean = true,
        skipDeprecated: Boolean = false,
        suppress: Boolean = false
    ) = PackageOptionsImpl(
        prefix = prefix,
        reportUndocumented = reportUndocumented,
        includeNonPublic = includeNonPublic,
        skipDeprecated = skipDeprecated,
        suppress = suppress
    )
}
