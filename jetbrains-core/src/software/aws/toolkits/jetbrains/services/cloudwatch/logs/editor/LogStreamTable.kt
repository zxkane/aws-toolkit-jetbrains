// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ListTableModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.LogStreamActor
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.LogStreamFilterActor
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.LogStreamListActor
import software.aws.toolkits.jetbrains.utils.ApplicationThreadPoolScope
import software.aws.toolkits.resources.message
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SortOrder

class LogStreamTable<T : Any>(
    val project: Project,
    logGroup: String,
    logStream: String,
    typeOfT: Class<T>
) :
    CoroutineScope by ApplicationThreadPoolScope("LogStreamTable"), Disposable {

    val component: JScrollPane
    val channel: Channel<LogStreamActor.Message>
    val logsTable: TableView<T>
    private val logStreamActor: LogStreamActor<*>

    init {
        val model = ListTableModel<T>(
            arrayOf(LogStreamDateColumn(), LogStreamMessageColumn()),
            mutableListOf<T>(),
            // Don't sort in the model because the requests come sorted
            -1,
            SortOrder.UNSORTED
        )
        logsTable = TableView(model).apply {
            setPaintBusy(true)
            autoscrolls = true
            emptyText.text = message("loading_resource.loading")
            tableHeader.reorderingAllowed = false
        }
        // TODO fix resizing
        logsTable.columnModel.getColumn(0).preferredWidth = 150
        logsTable.columnModel.getColumn(0).maxWidth = 150
        logsTable.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

        component = ScrollPaneFactory.createScrollPane(logsTable)

        logStreamActor = when (typeOfT) {
            // These as's are always correct, but are needed because we can't reify T
            OutputLogEvent::class -> LogStreamListActor(project, logsTable as TableView<OutputLogEvent>, logGroup, logStream)
            FilteredLogEvent::class -> LogStreamFilterActor(project, logsTable as TableView<FilteredLogEvent>, logGroup, logStream)
            else -> throw IllegalStateException("LogStreamTable attempted to be made that was not OutputLogEvent or FilteredLogEvent")
        }
        Disposer.register(this@LogStreamTable, logStreamActor)
        channel = logStreamActor.channel

        component.verticalScrollBar.addAdjustmentListener {
            if (logsTable.model.rowCount == 0) {
                return@addAdjustmentListener
            }
            if (component.verticalScrollBar.isAtBottom()) {
                launch {
                    logStreamActor.channel.send(LogStreamActor.Message.LOAD_FORWARD())
                }
            } else if (component.verticalScrollBar.isAtTop()) {
                launch { logStreamActor.channel.send(LogStreamActor.Message.LOAD_BACKWARD()) }
            }
        }
    }

    private fun JScrollBar.isAtBottom(): Boolean = value == (maximum - visibleAmount)
    private fun JScrollBar.isAtTop(): Boolean = value == minimum

    override fun dispose() {}
}
