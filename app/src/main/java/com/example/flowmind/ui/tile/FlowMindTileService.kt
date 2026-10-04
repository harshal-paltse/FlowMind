package com.example.flowmind.ui.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.flowmind.worker.WorkflowExecutionService

class FlowMindTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()

        val intent = Intent(this, WorkflowExecutionService::class.java).apply {
            action = WorkflowExecutionService.ACTION_START
            putExtra(WorkflowExecutionService.EXTRA_WORKFLOW_ID, "pinned_workflow")
        }
        startForegroundService(intent)
        
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
