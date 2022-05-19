/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.jvm

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarker.creationService
import spp.jetbrains.marker.plugin.SourceLineMarkerProvider
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker.Companion.SUPPORTED_FILE_TYPES
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.ClassGutterMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class JVMLineMarkerProvider : SourceLineMarkerProvider() {

    companion object {
        private val log = LoggerFactory.getLogger(JVMLineMarkerProvider::class.java)
    }

    override fun getLineMarkerInfo(
        parent: PsiElement?,
        element: PsiElement
    ): LineMarkerInfo<PsiElement>? {
        return when {
            parent is PsiClass && element === parent.nameIdentifier -> getClassGutterMark(element)
            element == JVMMarkerUtils.getNameIdentifier(parent) -> getMethodGutterMark(element)
            else -> null
        }
    }

    private fun getClassGutterMark(element: PsiIdentifier): LineMarkerInfo<PsiElement>? {
        val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile) ?: return null
        val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UClass)
        if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) return null

        //check by artifact name first due to user can erroneously name same class twice
        var gutterMark = fileMarker.getSourceMark(artifactQualifiedName, SourceMark.Type.GUTTER) as ClassGutterMark?
        if (gutterMark == null) {
            gutterMark = JVMMarkerUtils.getOrCreateClassGutterMark(fileMarker, element) ?: return null
        }
        if (!gutterMark.isVisible()) {
            return null
        }

        var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
        if (gutterMark.configuration.activateOnMouseClick) {
            navigationHandler = GutterIconNavigationHandler { _, _ ->
                element.getUserData(SourceKey.GutterMark)!!.displayPopup()
            }
        }
        return LineMarkerInfo(
            getFirstLeaf(element),
            element.textRange,
            gutterMark.configuration.icon,
            null,
            navigationHandler,
            GutterIconRenderer.Alignment.CENTER
        )
    }

    private fun getMethodGutterMark(element: PsiElement): LineMarkerInfo<PsiElement>? {
        val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile) ?: return null
        val uMethod = element.parent.toUElement() as UMethod?
        if (uMethod == null) {
            log.warn("Unable to transform to UMethod: {}", element.parent)
            return null
        }
        val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(uMethod)
        if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) return null

        //check by artifact name first due to user can erroneously name same method twice
        var gutterMark = fileMarker.getSourceMark(
            artifactQualifiedName,
            SourceMark.Type.GUTTER
        ) as MethodGutterMark?
        if (gutterMark == null) {
            gutterMark = creationService.getOrCreateMethodGutterMark(fileMarker, element) ?: return null
        }
        if (!gutterMark.isVisible()) {
            return null
        }

        var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
        if (gutterMark.configuration.activateOnMouseClick) {
            navigationHandler = GutterIconNavigationHandler { _, _ ->
                element.getUserData(SourceKey.GutterMark)!!.displayPopup()
            }
        }
        return LineMarkerInfo(
            getFirstLeaf(element),
            element.textRange,
            gutterMark.configuration.icon,
            null,
            navigationHandler,
            GutterIconRenderer.Alignment.CENTER
        )
    }

    /**
     * Associates Groovy [GutterMark]s to PSI elements.
     *
     * @since 0.1.0
     */
    class GroovyDescriptor : JVMLineMarkerProvider() {
        init {
            SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
        }

        override fun getName(): String {
            return "Groovy source line markers"
        }
    }

    /**
     * Associates Java [GutterMark]s to PSI elements.
     *
     * @since 0.1.0
     */
    class JavaDescriptor : JVMLineMarkerProvider() {
        init {
            SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        }

        override fun getName(): String {
            return "Java source line markers"
        }
    }

    /**
     * Associates Kotlin [GutterMark]s to PSI elements.
     *
     * @since 0.1.0
     */
    class KotlinDescriptor : JVMLineMarkerProvider() {
        init {
            SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        }

        override fun getName(): String {
            return "Kotlin source line markers"
        }
    }
}
