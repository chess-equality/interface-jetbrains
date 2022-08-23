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
package spp.jetbrains.plugin.impl

import com.apollographql.apollo3.exception.ApolloNetworkException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import spp.jetbrains.command.LiveCommand
import spp.jetbrains.indicator.LiveIndicator
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.plugin.LivePluginService
import spp.jetbrains.status.SourceStatus.ConnectionError
import spp.jetbrains.status.SourceStatusService

class LivePluginServiceImpl(val project: Project) : LivePluginService {

    companion object {
        private val log = logger<LivePluginServiceImpl>()
    }

    private val commands = mutableSetOf<LiveCommand>()
    private val indicators = mutableMapOf<LiveIndicator, SourceMarkEventListener>()

    override fun reset() {
        project.getUserData(LivePluginService.KEY)?.let { liveService ->
            liveService.getRegisteredLiveCommands().forEach {
                liveService.unregisterLiveCommand(it.name)
            }
            liveService.getRegisteredLiveIndicators().forEach {
                liveService.unregisterLiveIndicator(it)
            }
        }
    }

    override fun registerLiveCommand(command: LiveCommand) {
        commands.add(command)
    }

    override fun registerLiveIndicator(indicator: LiveIndicator) {
        val eventListener = SourceMarkEventListener {
            if ((indicator.listenForAllEvents || indicator.listenForEvents.contains(it.eventCode)) && it.sourceMark is GuideMark) {
                runBlocking {
                    try {
                        indicator.trigger(it.sourceMark as GuideMark, it)
                    } catch (e: ApolloNetworkException) {
                        log.warn("Error while triggering indicator $indicator", e)
                        SourceStatusService.getInstance(project).update(ConnectionError, e.message)
                    } catch (e: Exception) {
                        log.warn("Error while triggering indicator $indicator", e)
                    }
                }
            }
        }
        SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(eventListener)
        indicators[indicator] = eventListener

        runBlocking {
            indicator.onRegister()
        }
        log.debug("Registered indicator: $indicator - Current indicators: ${indicators.size}")
    }

    override fun unregisterLiveCommand(commandName: String) {
        commands.removeIf { it.name == commandName }
    }

    override fun unregisterLiveIndicator(indicator: LiveIndicator) {
        indicators.entries.find { it.key == indicator }?.let {
            SourceMarker.getInstance(project).removeGlobalSourceMarkEventListener(it.value)
            indicators.remove(it.key)

            runBlocking {
                indicator.onUnregister()
            }
            log.debug("Unregistered indicator: $indicator - Current indicators: ${indicators.size}")
        }
    }

    override fun getRegisteredLiveCommands(): List<LiveCommand> {
        return commands.toList()
    }

    override fun getRegisteredLiveCommands(element: PsiElement): List<LiveCommand> {
        return commands.filter { it.isAvailable(element) }
    }

    override fun getRegisteredLiveIndicators(): List<LiveIndicator> {
        return indicators.keys.toList()
    }
}
