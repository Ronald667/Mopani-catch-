package com.example

data class WormItem(
  val id: Long,
  val emoji: String,
  val points: Int,
  val xPercent: Float, // 0.0 to 85.0
  val yPercent: Float, // 0.0 to 75.0
  val lifetimeMillis: Long = 1400L
)

enum class GameDifficulty(val title: String, val durationSeconds: Int, val spawnIntervalMillis: Long) {
  EASY("Easy (30s)", 30, 800L),
  NORMAL("Normal (20s)", 20, 600L),
  HARD("Hard (15s)", 15, 450L)
}

sealed interface GameScreenState {
  object Menu : GameScreenState
  data class Playing(val difficulty: GameDifficulty) : GameScreenState
  data class GameOver(val score: Int, val isNewHighScore: Boolean) : GameScreenState
  object HighScores : GameScreenState
  object HowToPlay : GameScreenState
}
