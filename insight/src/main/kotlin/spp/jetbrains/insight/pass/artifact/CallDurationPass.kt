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
package spp.jetbrains.insight.pass.artifact

import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.model.ReferenceArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.InsightKeys.CALL_ARGS
import spp.jetbrains.insight.getDuration
import spp.jetbrains.insight.pass.ArtifactPass
import spp.jetbrains.insight.path.ProceduralMultiPath
import spp.jetbrains.insight.path.ProceduralPath
import spp.protocol.insight.InsightType.FUNCTION_DURATION
import spp.protocol.insight.InsightType.PATH_DURATION
import spp.protocol.insight.InsightValue

/**
 * Sets the [FUNCTION_DURATION] on [CallArtifact]s which can be resolved and have a known function duration.
 */
class CallDurationPass : ArtifactPass() {

    override fun analyze(element: ArtifactElement) {
        if (element !is CallArtifact) return //only interested in calls

        val resolvedFunction = element.getResolvedFunction()
        if (resolvedFunction != null) {
            resolvedFunction.data[CALL_ARGS] = resolveArguments(element) //todo: don't set if contains unresolved?

            var multiPath = element.getData(InsightKeys.PROCEDURAL_MULTI_PATH)
            if (shouldAnalyzeResolvedFunction(multiPath, element)) {
                multiPath = analyzer.analyze(resolvedFunction)
            }

            val callArgs = resolvedFunction.getData(CALL_ARGS) ?: resolvedFunction.parameters
            if (multiPath != null && isFullyResolved(callArgs)) {
                //use the average of pre-determined durations (if available)
                val possiblePaths = determinePossiblePaths(callArgs, multiPath)
                val duration = possiblePaths.mapNotNull {
                    it.getInsights().find { it.type == PATH_DURATION }?.value as Long?
                }.ifEmpty { null }?.average()?.toLong()
                if (duration != null) {
                    element.data[InsightKeys.FUNCTION_DURATION] =
                        InsightValue.of(FUNCTION_DURATION, duration).asDerived()
                }
            }

            val duration = resolvedFunction.getDuration()
            if (duration != null) {
                element.data[InsightKeys.FUNCTION_DURATION] =
                    InsightValue.of(FUNCTION_DURATION, duration).asDerived()
            }
        }
    }

    /**
     * Replace [ReferenceArtifact] params with the actual values from the call.
     */
    private fun resolveArguments(element: CallArtifact): List<ArtifactElement> {
        val callArgs = rootArtifact.getData(CALL_ARGS)
        return element.getArguments().map {
            if (it is ReferenceArtifact && it.isFunctionParameter()) {
                callArgs?.getOrNull(it.getFunctionParameterIndex()) ?: it
            } else {
                it
            }
        }
    }

    private fun isFullyResolved(callArgs: List<ArtifactElement>): Boolean {
        return callArgs.none { (it as? ReferenceArtifact)?.isFunctionParameter() == true }
    }

    private fun shouldAnalyzeResolvedFunction(multiPath: ProceduralMultiPath?, element: CallArtifact): Boolean {
        if (!analyzer.passConfig.analyzeResolvedFunctions) return false
        if (element.isRecursive()) return false
        return multiPath == null || !multiPath.paths.all { it.getInsights().any { it.type == PATH_DURATION } }
    }

    private fun determinePossiblePaths(
        params: List<ArtifactElement>,
        multiPath: ProceduralMultiPath,
    ): ProceduralMultiPath {
        val possiblePaths = mutableListOf<ProceduralPath>()
        for (path in multiPath) {
            if (path.evaluateParams(params)) {
                possiblePaths.add(path)
            }
        }
        return ProceduralMultiPath(possiblePaths)
    }

    private fun CallArtifact.isRecursive(): Boolean {
        return getData(InsightKeys.RECURSIVE_CALL)?.value == true
    }
}
