package com.securechat.opticalresearch

import com.securechat.opticalresearch.model.ExperimentSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class ExperimentIdTest {

    @Test
    fun testExperimentIdFormat() {
        val id = ExperimentSession.generateExperimentId()
        // Format: EXP-YYYYMMDD-HHMMSS-XXXXXX
        val regex = Pattern.compile("^EXP-[0-9]{8}-[0-9]{6}-[A-Za-z0-9]+$")
        assertTrue("Experiment ID '$id' must match EXP-YYYYMMDD-HHMMSS-XXXX pattern", regex.matcher(id).matches())
    }

    @Test
    fun testExperimentIdUniqueness() {
        val id1 = ExperimentSession.generateExperimentId()
        val id2 = ExperimentSession.generateExperimentId()
        assertNotEquals("Consecutively generated experiment IDs must be unique", id1, id2)
    }

    @Test
    fun testIsoTimestampFormat() {
        val ts = ExperimentSession.getCurrentIsoTimestamp()
        val regex = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$")
        assertTrue("Timestamp '$ts' must be valid ISO-8601 UTC format", regex.matcher(ts).matches())
    }
}
