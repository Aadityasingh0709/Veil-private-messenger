package com.securechat.opticalresearch.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.securechat.opticalresearch.renderer.RenderingDiagnostics

/**
 * Phase 4B metadata extras attached to an [ExperimentSession].
 *
 * Encapsulates the experimental modulation parameters and hardware-measured
 * rendering diagnostics from the Choreographer frame loop.
 *
 * Serialized to JSON and persisted within [ExperimentSession.notes] (prefixed with `[PHASE4B_EXTRAS]`)
 * to maintain strict schema compatibility with existing Phase 4A stores, while enabling
 * full reconstruction of modulation parameters and frame timing during analysis.
 */
data class ModulationSessionExtras(
    val testMode: String,
    val modulationType: String,
    val spatialPattern: String? = null,
    val gridPeriodPx: Int? = null,
    val contrastRatio: Float? = null,
    val orientationDeg: Float? = null,
    val modulationFrequencyHz: Double? = null,
    val dutyCycle: Float? = null,
    val amplitudeFraction: Float? = null,
    val diagnosticsSnapshot: RenderingDiagnostics.DiagnosticsSnapshot? = null
) {
    fun toJson(): String = gson.toJson(this)

    companion object {
        private const val PREFIX = "[[PHASE4B_EXTRAS]]"
        private val gson: Gson = GsonBuilder().create()

        /**
         * Maps a [ModulationParams] instance to the standardized `testMode` enum value
         * defined in `research/optical-capture/python/schemas/manifest.schema.json`.
         */
        fun testModeFromParams(params: ModulationParams): String = when (params) {
            is ModulationParams.Baseline -> "baseline"
            is ModulationParams.Spatial -> "spatial_modulation"
            is ModulationParams.Temporal -> "temporal_modulation"
            is ModulationParams.Combined -> "combined_modulation"
        }

        /**
         * Constructs [ModulationSessionExtras] from active params and diagnostics snapshot.
         */
        fun from(
            params: ModulationParams,
            diagnostics: RenderingDiagnostics.DiagnosticsSnapshot? = null
        ): ModulationSessionExtras = when (params) {
            is ModulationParams.Baseline -> ModulationSessionExtras(
                testMode = "baseline",
                modulationType = "none",
                diagnosticsSnapshot = diagnostics
            )

            is ModulationParams.Spatial -> ModulationSessionExtras(
                testMode = "spatial_modulation",
                modulationType = "spatial",
                spatialPattern = params.patternType.name,
                gridPeriodPx = params.gridPeriodPx,
                contrastRatio = params.contrastRatio,
                orientationDeg = params.orientationDeg,
                diagnosticsSnapshot = diagnostics
            )

            is ModulationParams.Temporal -> ModulationSessionExtras(
                testMode = "temporal_modulation",
                modulationType = "temporal",
                modulationFrequencyHz = params.modulationFrequencyHz,
                dutyCycle = params.dutyCycle,
                amplitudeFraction = params.amplitudeFraction,
                diagnosticsSnapshot = diagnostics
            )

            is ModulationParams.Combined -> ModulationSessionExtras(
                testMode = "combined_modulation",
                modulationType = "combined",
                spatialPattern = params.spatial.patternType.name,
                gridPeriodPx = params.spatial.gridPeriodPx,
                contrastRatio = params.spatial.contrastRatio,
                orientationDeg = params.spatial.orientationDeg,
                modulationFrequencyHz = params.temporal.modulationFrequencyHz,
                dutyCycle = params.temporal.dutyCycle,
                amplitudeFraction = params.temporal.amplitudeFraction,
                diagnosticsSnapshot = diagnostics
            )
        }

        /**
         * Serializes extras and appends them to user notes string.
         */
        fun embedInNotes(userNotes: String, extras: ModulationSessionExtras): String {
            val cleanNotes = userNotes.substringBefore(PREFIX).trimEnd()
            val extrasJson = extras.toJson()
            return if (cleanNotes.isBlank()) {
                "$PREFIX$extrasJson"
            } else {
                "$cleanNotes\n\n$PREFIX$extrasJson"
            }
        }

        /**
         * Extracts [ModulationSessionExtras] from a session's notes string if present.
         */
        fun extractFromNotes(notes: String): ModulationSessionExtras? {
            if (!notes.contains(PREFIX)) return null
            return try {
                val jsonPart = notes.substringAfter(PREFIX).trim()
                gson.fromJson(jsonPart, ModulationSessionExtras::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }
}
