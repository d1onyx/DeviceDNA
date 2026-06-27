package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthInsight(
    val id: String,
    val title: String,
    val summary: String,
    val severity: InsightSeverity,
    val confidence: Float,
    val actions: List<RecommendedAction>,
)

@Serializable
data class RecommendedAction(val label: String, val description: String)

@Serializable
enum class InsightSeverity { Good, Info, Warning, Critical }

@Serializable
data class HealthScore(
    val overall: Int,
    val battery: Int,
    val performance: Int,
    val storage: Int,
    val security: Int,
    val thermal: Int,
    val insights: List<HealthInsight>,
    val fraudRisk: FraudRiskScore = FraudRiskScore(),
)

@Serializable
data class FraudRiskScore(
    val score: Int = 0,
    val level: FraudRiskLevel = FraudRiskLevel.Low,
    val signals: List<FraudSignal> = emptyList(),
)

@Serializable
data class FraudSignal(
    val id: String,
    val label: String,
    val severity: FraudSignalSeverity,
    val evidence: String,
)

@Serializable
enum class FraudRiskLevel { Low, Medium, High, Critical }

@Serializable
enum class FraudSignalSeverity { Info, Low, Medium, High, Critical }
