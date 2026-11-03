package com.securechat.opticalresearch.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

/**
 * Standardized manifest structure serialized to manifest.json in exported research archives.
 * Conforms 100% to research/optical-capture/python/schemas/manifest.schema.json.
 */
data class ExperimentManifest(
    @SerializedName("schemaVersion")
    val schemaVersion: String = "1.0.0",

    @SerializedName("experimentId")
    val experimentId: String,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("testMode")
    val testMode: String,

    @SerializedName("sourceImage")
    val sourceImage: SourceImageManifest,

    @SerializedName("phoneA")
    val phoneA: PhoneADeviceInfo,

    @SerializedName("phoneB")
    val phoneB: PhoneBCaptureInfo,

    @SerializedName("captures")
    val captures: List<CaptureManifestItem>,

    @SerializedName("notes")
    val notes: String = ""
) {
    fun toJson(): String {
        return gson.toJson(this)
    }

    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        fun fromJson(json: String): ExperimentManifest {
            return gson.fromJson(json, ExperimentManifest::class.java)
        }

        fun fromSession(session: ExperimentSession): ExperimentManifest {
            return ExperimentManifest(
                schemaVersion = "1.0.0",
                experimentId = session.experimentId,
                timestamp = session.timestamp,
                testMode = session.testMode,
                sourceImage = SourceImageManifest(
                    name = session.sourceImage.name,
                    hashSha256 = session.sourceImage.hashSha256,
                    width = session.sourceImage.width,
                    height = session.sourceImage.height
                ),
                phoneA = session.phoneA,
                phoneB = session.phoneB,
                captures = session.captures.map { c ->
                    CaptureManifestItem(
                        filename = c.filename,
                        hashSha256 = c.hashSha256,
                        repetitionNumber = c.repetitionNumber,
                        timestamp = c.timestamp,
                        exif = c.exif
                    )
                },
                notes = session.notes
            )
        }
    }
}

data class SourceImageManifest(
    @SerializedName("name") val name: String,
    @SerializedName("hash_sha256") val hashSha256: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

data class CaptureManifestItem(
    @SerializedName("filename") val filename: String,
    @SerializedName("hash_sha256") val hashSha256: String?,
    @SerializedName("repetitionNumber") val repetitionNumber: Int,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("exif") val exif: ExifMetadata?
)
