package com.example.hardware

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

object FaceFeatureExtractor {

    fun createDetector(): FaceDetector {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        return FaceDetection.getClient(options)
    }

    fun eyesOpen(face: Face): Boolean {
        val l = face.leftEyeOpenProbability ?: 1f
        val r = face.rightEyeOpenProbability ?: 1f
        return l > 0.35f && r > 0.35f
    }

    fun isFrontalEnough(face: Face): Boolean {
        return abs(face.headEulerAngleY) < 24f && abs(face.headEulerAngleZ) < 22f
    }

    /**
     * Extracts a normalized, unit-length geometric facial signature vector
     * invariant to camera distance and translation using ML Kit landmarks and contours.
     */
    fun extractDescriptor(face: Face): List<Double>? {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return null
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return null
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position ?: return null
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

        val interOcular = hypot(
            (rightEye.x - leftEye.x).toDouble(),
            (rightEye.y - leftEye.y).toDouble()
        )
        if (interOcular < 12.0) return null

        val midEyeX = (leftEye.x + rightEye.x) / 2f
        val midEyeY = (leftEye.y + rightEye.y) / 2f

        val keyPoints = mutableListOf<PointF>()
        keyPoints.add(leftEye)
        keyPoints.add(rightEye)
        keyPoints.add(nose)
        if (mouthLeft != null) keyPoints.add(mouthLeft)
        if (mouthRight != null) keyPoints.add(mouthRight)
        if (mouthBottom != null) keyPoints.add(mouthBottom)
        face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.let { keyPoints.add(it) }
        face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.let { keyPoints.add(it) }

        val contourTypes = listOf(
            FaceContour.FACE,
            FaceContour.LEFT_EYEBROW_TOP,
            FaceContour.RIGHT_EYEBROW_TOP,
            FaceContour.NOSE_BRIDGE,
            FaceContour.NOSE_BOTTOM,
            FaceContour.UPPER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM
        )

        val features = mutableListOf<Double>()

        // Normalized pairwise landmark distances
        for (i in keyPoints.indices) {
            for (j in i + 1 until keyPoints.size) {
                val dist = hypot(
                    (keyPoints[i].x - keyPoints[j].x).toDouble(),
                    (keyPoints[i].y - keyPoints[j].y).toDouble()
                ) / interOcular
                features.add(dist)
            }
        }

        // Normalized radial coordinates of sampled contour points relative to eye midpoint
        for (ct in contourTypes) {
            val pts = face.getContour(ct)?.points.orEmpty()
            if (pts.isNotEmpty()) {
                val sampleIndices = listOf(0, pts.size / 3, (2 * pts.size) / 3, pts.lastIndex).distinct()
                for (idx in sampleIndices) {
                    val p = pts[idx]
                    features.add((p.x - midEyeX) / interOcular)
                    features.add((p.y - midEyeY) / interOcular)
                }
            }
        }

        if (features.size < 16) return null

        // L2 normalize so threshold ~0.35-0.55 behaves consistently
        val norm = sqrt(features.sumOf { it * it })
        if (norm < 1e-6) return null
        return features.map { it / norm }
    }
}
