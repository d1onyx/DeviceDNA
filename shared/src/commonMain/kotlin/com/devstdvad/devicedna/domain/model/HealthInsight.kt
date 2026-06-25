package com.devstdvad.devicedna.domain.model

data class HealthInsight(
    val id: String,
    val title: String,
    val summary: String,
    val severity: InsightSeverity,
    val confidence: Float,
    val actions: List<RecommendedAction>,
)

data class RecommendedAction(val label: String, val description: String)

enum class InsightSeverity { Good, Info, Warning, Critical }

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

data class FraudRiskScore(
    val score: Int = 0,
    val level: FraudRiskLevel = FraudRiskLevel.Low,
    val signals: List<FraudSignal> = emptyList(),
)

data class FraudSignal(
    val id: String,
    val label: String,
    val severity: FraudSignalSeverity,
    val evidence: String,
)

enum class FraudRiskLevel { Low, Medium, High, Critical }
enum class FraudSignalSeverity { Info, Low, Medium, High, Critical }
