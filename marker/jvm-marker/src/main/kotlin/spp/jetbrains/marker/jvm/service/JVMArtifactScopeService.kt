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
package spp.jetbrains.marker.jvm.service

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.*
import com.intellij.psi.scope.processor.VariablesProcessor
import com.intellij.psi.scope.util.PsiScopesUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import com.siyeh.ig.psiutils.ControlFlowUtils
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.joor.Reflect
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.service.define.IArtifactScopeService
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * Used to determine the scope of JVM artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactScopeService : IArtifactScopeService {

    override fun getCalledFunctions(
        element: PsiElement,
        includeExternal: Boolean,
        includeIndirect: Boolean
    ): List<PsiNameIdentifierOwner> {
        if (includeIndirect) {
            return ReadAction.compute(ThrowableComputable {
                val calledFunctions = getResolvedCalls(element)
                val filteredFunctions = calledFunctions.filter { includeExternal || it.isWritable }
                return@ThrowableComputable (filteredFunctions + filteredFunctions.flatMap {
                    getCalledFunctions(it, includeExternal, true)
                }).toList()
            })
        }

        return ReadAction.compute(ThrowableComputable {
            return@ThrowableComputable getResolvedCalls(element).filter { includeExternal || it.isWritable }.toList()
        })
    }

    override fun getCallerFunctions(element: PsiElement, includeIndirect: Boolean): List<PsiNameIdentifierOwner> {
        val references = ProgressManager.getInstance().runProcess(Computable {
            ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project)).toList()
        }, EmptyProgressIndicator(ModalityState.defaultModalityState()))
        return ReadAction.compute(ThrowableComputable {
            references.mapNotNull {
                when (element.language.id) {
                    "Groovy" -> it.element.parentOfType<GrMethod>()
                    "kotlin" -> it.element.parentOfType<KtNamedFunction>()
                    else -> it.element.parentOfType<PsiMethod>()
                }
            }.filter { it.isWritable() }
        })
    }

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        //determine available vars
        var checkLine = lineNumber
        val scopeVars = mutableListOf<String>()
        var minScope: PsiElement? = null
        while (minScope == null) {
            minScope = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, --checkLine)
        }
        val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
            override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
        }
        PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
        for (i in 0 until variablesProcessor.size()) {
            scopeVars.add(variablesProcessor.getResult(i).name!!)
        }
        return scopeVars
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        if (element.language.id == "kotlin") {
            return element.parentOfTypes(KtNamedFunction::class) != null
        }
        return element.parentOfTypes(PsiMethod::class) != null
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        val parentLoop = element.parentOfTypes(PsiConditionalLoopStatement::class)
        return parentLoop != null && ControlFlowUtils.isEndlessLoop(parentLoop)
    }

    override fun isJVM(element: PsiElement): Boolean = true

    override fun canShowControlBar(psiElement: PsiElement): Boolean {
        return when (psiElement::class.java.name) {
            "org.jetbrains.kotlin.psi.KtObjectDeclaration" -> false
            "org.jetbrains.kotlin.psi.KtProperty" -> {
                Reflect.on(psiElement).call("isLocal").get<Boolean>() == true
            }

            else -> super.canShowControlBar(psiElement)
        }
    }

    private fun getResolvedCalls(element: PsiElement): Sequence<PsiNameIdentifierOwner> {
        return when (element.language.id) {
            "Groovy" -> element.descendantsOfType<GrCall>().map {
                it.resolveMethod()
            }.filterNotNull()

            "kotlin" -> element.descendantsOfType<KtCallExpression>().map {
                it.resolveToCall()?.candidateDescriptor?.psiElement as? PsiNameIdentifierOwner
            }.filterNotNull()

            else -> element.descendantsOfType<PsiCall>().map { call ->
                call.resolveMethod()
            }.filterNotNull()
        }
    }
}
