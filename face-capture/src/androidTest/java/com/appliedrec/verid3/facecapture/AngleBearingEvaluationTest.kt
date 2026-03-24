package com.appliedrec.verid3.facecapture

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.EulerAngle
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AngleBearingEvaluationTest {

    private lateinit var settings: FaceCaptureSessionSettings
    private lateinit var eval: AngleBearingEvaluation

    @Before
    fun setUp() {
        settings = FaceCaptureSessionSettings()
        eval = AngleBearingEvaluation(settings)
    }

    @Test
    fun straightFaceMatchesStraightBearing() {
        Assert.assertTrue(eval.angleMatchesBearing(EulerAngle(0f, 0f, 0f), Bearing.STRAIGHT))
    }

    @Test
    fun leftTurnMatchesLeftBearing() {
        // yaw well past the left threshold
        Assert.assertTrue(eval.angleMatchesBearing(EulerAngle(-20f, 0f, 0f), Bearing.LEFT))
    }

    @Test
    fun rightTurnMatchesRightBearing() {
        Assert.assertTrue(eval.angleMatchesBearing(EulerAngle(20f, 0f, 0f), Bearing.RIGHT))
    }

    @Test
    fun straightFaceDoesNotMatchLeftBearing() {
        Assert.assertFalse(eval.angleMatchesBearing(EulerAngle(0f, 0f, 0f), Bearing.LEFT))
    }

    @Test
    fun angleAtThresholdBoundaryDoesNotMatchStraight() {
        // STRAIGHT max yaw is (yawThreshold - yawThresholdTolerance), comparison is exclusive (<)
        val boundaryYaw = settings.yawThreshold - settings.yawThresholdTolerance
        Assert.assertFalse(eval.angleMatchesBearing(EulerAngle(boundaryYaw, 0f, 0f), Bearing.STRAIGHT))
    }

    @Test
    fun angleInsideBoundaryMatchesStraight() {
        val insideYaw = settings.yawThreshold - settings.yawThresholdTolerance - 1f
        Assert.assertTrue(eval.angleMatchesBearing(EulerAngle(insideYaw, 0f, 0f), Bearing.STRAIGHT))
    }

    @Test
    fun offsetIsZeroWhenAlreadyMatchingBearing() {
        val offset = eval.offsetFromAngleToBearing(EulerAngle(0f, 0f, 0f), Bearing.STRAIGHT)
        Assert.assertEquals(0f, offset.yaw, 0.001f)
        Assert.assertEquals(0f, offset.pitch, 0.001f)
    }

    @Test
    fun offsetIsNonZeroWhenMisaligned() {
        val offset = eval.offsetFromAngleToBearing(EulerAngle(0f, 0f, 0f), Bearing.LEFT)
        Assert.assertNotEquals(0f, offset.yaw)
    }

    @Test
    fun angleHalfwayBetweenStraightAndLeftIsInBetween() {
        val halfwayYaw = -(settings.yawThreshold / 2f)
        Assert.assertTrue(
            eval.isAngleBetweenBearings(EulerAngle(halfwayYaw, 0f, 0f), Bearing.STRAIGHT, Bearing.LEFT)
        )
    }

    @Test
    fun angleOppositeDirectionIsNotBetweenStraightAndLeft() {
        // Must be outside the radius-17 start circle to escape the "near start" shortcut;
        // 25° is clearly in the rightward direction, opposite the STRAIGHT→LEFT path
        Assert.assertFalse(
            eval.isAngleBetweenBearings(EulerAngle(25f, 0f, 0f), Bearing.STRAIGHT, Bearing.LEFT)
        )
    }
}
