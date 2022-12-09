/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.service.define

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IArtifactScopeService : ISourceMarkerService {

    fun getFunctions(element: PsiFile): List<PsiNamedElement>
    fun getChildIfs(element: PsiElement): List<PsiElement>
    fun getParentIf(element: PsiElement): PsiElement?
    fun getParentFunction(element: PsiElement): PsiNamedElement?
    fun getCalls(element: PsiElement): List<PsiElement>

    fun getCalledFunctions(
        element: PsiElement,
        includeExternal: Boolean = false,
        includeIndirect: Boolean = false
    ): List<PsiNameIdentifierOwner>

    fun getCallerFunctions(element: PsiElement, includeIndirect: Boolean = false): List<PsiNameIdentifierOwner>
    fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String>
    fun isInsideFunction(element: PsiElement): Boolean
    fun isInsideEndlessLoop(element: PsiElement): Boolean = false
    fun isJVM(element: PsiElement): Boolean = false
    fun canShowControlBar(psiElement: PsiElement): Boolean = true
}
