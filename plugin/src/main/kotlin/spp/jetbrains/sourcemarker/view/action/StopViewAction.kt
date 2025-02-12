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
package spp.jetbrains.sourcemarker.view.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.view.ResumableViewManager

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class StopViewAction(private val viewManager: ResumableViewManager) : AnAction(PluginIcons.stop) {

    init {
        templatePresentation.text = "Stop View"
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = viewManager.currentView?.isRunning == true
        e.presentation.isVisible = viewManager.currentView != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        viewManager.currentView?.pause()
    }
}
