/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import spp.jetbrains.marker.jvm.JVMEndpointDetector

class ScalaEndpointNameDetectorTest : EndpointDetectorTest() {

    companion object {
        lateinit var vertx: Vertx

        @BeforeAll
        @JvmStatic
        fun startVertx() {
            vertx = Vertx.vertx()
        }

        @BeforeAll
        @JvmStatic
        fun stopVertx() {
            vertx.close()
        }
    }

    @Test
    fun `SpringMVCEndpoint RequestMapping method`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @RequestMapping(value = Array("/doGet"), method = Array(RequestMethod.GET))
                        def getStr() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(vertx).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVCEndpoint GetMapping method`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(name = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(vertx).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVCEndpoint GetMapping method_path`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(path = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(vertx).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVCEndpoint GetMapping method_value`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(value = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(vertx).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SkyWalking Trace with operation name`() {
        @Language("Scala") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace(operationName = "doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(vertx).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SkyWalking Trace no operation name`() {
        @Language("Scala") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(vertx).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("TestController.doGet", result.get().name)
            }
        }
    }
}
