package com.securechat.opticalresearch.assets

/**
 * Enumeration of the 7 standardized synthetic reference test charts for Phase 4A.
 */
enum class TestChartType(
    val title: String,
    val description: String,
    val defaultFilename: String
) {
    COLOR_CALIBRATION(
        title = "Color Calibration Chart",
        description = "24-patch colorimetric reference with primary, secondary, skin-tone, and neutral patches.",
        defaultFilename = "chart_color_calibration.png"
    ),
    GRAYSCALE_GRADIENT(
        title = "Grayscale Gradient Chart",
        description = "16-step calibrated luminance wedge, smooth 0-255 ramp, and shadow/highlight resolving steps.",
        defaultFilename = "chart_grayscale_gradient.png"
    ),
    CHECKERBOARD(
        title = "Checkerboard Grid",
        description = "16x24 alternating black/white calibration grid for geometric linearity and lens distortion.",
        defaultFilename = "chart_checkerboard.png"
    ),
    FINE_LINES(
        title = "Fine-Line & Resolution Target",
        description = "Variable frequency Ronchi rulings (1, 2, 4, 8 px line pairs) and radial Siemens star target.",
        defaultFilename = "chart_fine_lines.png"
    ),
    HIGH_CONTRAST_EDGES(
        title = "High-Contrast Edge & MTF Target",
        description = "ISO 12233 5-degree slanted edges and knife-edge step response transitions.",
        defaultFilename = "chart_high_contrast_edges.png"
    ),
    TEXT_READABILITY(
        title = "Typographic Legibility Chart",
        description = "Snellen-style typographic cascade from 48pt down to 7pt, contrast ladder, and inverted text.",
        defaultFilename = "chart_text_readability.png"
    ),
    COMBINED_QUALITY(
        title = "Master Combined Quality Target",
        description = "Integrated diagnostic target incorporating color, grayscale, edges, lines, and microtext.",
        defaultFilename = "chart_combined_quality.png"
    )
}
