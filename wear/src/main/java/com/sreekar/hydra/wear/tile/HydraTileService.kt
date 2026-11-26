package com.sreekar.hydra.wear.tile

import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.material.Text
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Tile for Galaxy Watch Ultra swipe screen.
 * Shows Hydra shortcut — tap opens the app to log.
 * Keep it simple for v1: text only, no custom click handling yet.
 */
class HydraTileService : TileService() {
    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val tile = Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(60_000)
            .setTileTimeline(
                Timeline.Builder().addTimelineEntry(
                    TimelineEntry.Builder().setLayout(
                        Layout.Builder().setRoot(
                            Column.Builder()
                                .addContent(Text.Builder(this, "💧 Hydra").build())
                                .addContent(Text.Builder(this, "Tap to log water").build())
                                .build()
                        ).build()
                    ).build()
                ).build()
            ).build()
        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion("1").build()
        )
    }
}
