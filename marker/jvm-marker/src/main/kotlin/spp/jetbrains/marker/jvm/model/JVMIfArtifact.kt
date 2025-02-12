/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm.model

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIfStatement
import org.jetbrains.kotlin.psi.KtIfExpression
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.artifact.service.toArtifact

class JVMIfArtifact(override val psiElement: PsiElement) : IfArtifact(psiElement) {

    override val condition: ArtifactElement?
        get() {
            return when (psiElement) {
                is PsiIfStatement -> psiElement.condition?.toArtifact()
                is KtIfExpression -> psiElement.condition?.toArtifact()
                else -> TODO()
            }
        }

    override val thenBranch: ArtifactElement?
        get() {
            return when (psiElement) {
                is PsiIfStatement -> psiElement.thenBranch?.toArtifact()
                is KtIfExpression -> psiElement.then?.toArtifact()
                else -> TODO()
            }
        }

    override val elseBranch: ArtifactElement?
        get() {
            return when (psiElement) {
                is PsiIfStatement -> psiElement.elseBranch?.toArtifact()
                is KtIfExpression -> psiElement.`else`?.toArtifact()
                else -> TODO()
            }
        }

    override fun clone(): JVMIfArtifact {
        return JVMIfArtifact(psiElement)
    }
}
