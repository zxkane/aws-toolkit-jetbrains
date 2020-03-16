// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

class TailLogs : ToggleAction("tail logs <localize>", null, AllIcons.RunConfigurations.Scroll_down), DumbAware {
    private var isSelected = false

    override fun isSelected(e: AnActionEvent): Boolean = isSelected

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        isSelected = state
        if (state) {
            startTailing()
        } else {
            stopTailing()
        }
    }

    private fun startTailing() {
        println("start")
    }

    private fun stopTailing() {
        println("stop")
    }
}
