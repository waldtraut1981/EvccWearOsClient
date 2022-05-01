/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wagner_wedtlenstedt.evccwearosclient

import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders
import androidx.wear.tiles.DimensionBuilders.degrees
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.tiles.LayoutElementBuilders.Arc
import androidx.wear.tiles.LayoutElementBuilders.ArcLine
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccRepository
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"

// dimensions
private val PROGRESS_BAR_THICKNESS = dp(20f)

// Complete degrees for a circle (relates to [Arc] component)
private const val ARC_TOTAL_DEGREES = 180f


class EvccTileService: TileService() {
    // For coroutines, use a custom scope we can cancel when the service is destroyed
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // TODO: Build a Tile.
    override fun onTileRequest(requestParams: TileRequest) = serviceScope.future {

        val evccRepository = EvccRepository()
        evccRepository.fetchEvccState()

        val deviceParams = requestParams.deviceParameters!!

        // Creates Tile.
        Tile.Builder()
            // If there are any graphics/images defined in the Tile's layout, the system will
            // retrieve them via onResourcesRequest() and match them with this version number.
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(1000 * 20)

            // Creates a timeline to hold one or more tile entries for a specific time periods.
            .setTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                Layout.Builder()
                                    .setRoot(
                                        // Creates the root [Box] [LayoutElement]
                                        layout(evccRepository.getEvccLiveData(),deviceParams)
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest) = serviceScope.future {
        Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    private fun layout(
        liveData: MutableLiveData<EvccStateModel>,
        deviceParams: DeviceParametersBuilders.DeviceParameters
    ) =
        Box.Builder()
            // Sets width and height to expand and take up entire Tile space.
            .setWidth(expand())
            .setHeight(expand())

            // Adds an [Arc] via local function.
            .addContent(progressArc(liveData))

            .addContent(
                LayoutElementBuilders.Column.Builder()
                    // Adds a [Text] via local function.
                    .addContent(
                        LayoutElementBuilders.Text.Builder().setText("X ${liveData.value?.result?.homePower ?: 0.0f}")
                        .setFontStyle(LayoutElementBuilders.FontStyles.display2(deviceParams).build())
                        .build()
                    )
                    .build()
            )
            .build()


    override fun onDestroy() {
        super.onDestroy()
        // Cleans up the coroutine
        serviceScope.cancel()
    }

    private fun progressArc(liveData: MutableLiveData<EvccStateModel>) = Arc.Builder()
        .addContent(
            ArcLine.Builder()
                // Uses degrees() helper to build an [AngularDimension] which represents progress.
                .setLength(degrees(liveData.value?.result?.homePower?.div(5000.0f)
                    ?.times(ARC_TOTAL_DEGREES) ?: 0.0f))
                .setColor(argb(ContextCompat.getColor(this, R.color.green)))
                .setThickness(PROGRESS_BAR_THICKNESS)
                .build()
        )
        // Element will start at 12 o'clock or 0 degree position in the circle.
        .setAnchorAngle(degrees(0.0f))
        // Aligns the contents of this container relative to anchor angle above.
        // ARC_ANCHOR_START - Anchors at the start of the elements. This will cause elements
        // added to an arc to begin at the given anchor_angle, and sweep around to the right.
        .setAnchorType(ARC_ANCHOR_START)
        .build()

}
