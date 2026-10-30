package com.example

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore(name = "mopani_prefs")

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val _screenState = MutableStateFlow<GameScreenState>(GameScreenState.Menu)
  val screenState: StateFlow<GameScreenState> = _screenState.asStateFlow()

  private val _score = MutableStateFlow(0)
  val score: StateFlow<Int> = _score.asStateFlow()

  private val _timeLeft = MutableStateFlow(20)
  val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

  private val _worms = MutableStateFlow<List<WormItem>>(emptyList())
  val worms: StateFlow<List<WormItem>> = _worms.asStateFlow()

  private val _highScore = MutableStateFlow(0)
  val highScore: StateFlow<Int> = _highScore.asStateFlow()

  private val highScoreKey = intPreferencesKey("high_score")

  private var gameTimerJob: Job? = null
  private var spawnJob: Job? = null
  private var nextWormId = 0L

  init {
    loadHighScore()
  }

  private fun loadHighScore() {
    viewModelScope.launch {
      getApplication<Application>().applicationContext.dataStore.data
        .map { prefs -> prefs[highScoreKey] ?: 0 }
        .collect { stored ->
          _highScore.value = stored
        }
    }
  }

  private fun saveHighScore(newScore: Int) {
    if (newScore > _highScore.value) {
      _highScore.value = newScore
      viewModelScope.launch {
        getApplication<Application>().applicationContext.dataStore.edit { prefs ->
          prefs[highScoreKey] = newScore
        }
      }
    }
  }

  fun setScreen(state: GameScreenState) {
    stopGame()
    _screenState.value = state
  }

  fun startGame(difficulty: GameDifficulty) {
    _score.value = 0
    _timeLeft.value = difficulty.durationSeconds
    _worms.value = emptyList()
    _screenState.value = GameScreenState.Playing(difficulty)

    startTimer(difficulty.durationSeconds)
    startSpawner(difficulty)
  }

  private fun startTimer(duration: Int) {
    gameTimerJob?.cancel()
    gameTimerJob = viewModelScope.launch {
      var current = duration
      while (current > 0) {
        delay(1000L)
        current--
        _timeLeft.value = current
      }
      endGame()
    }
  }

  private fun startSpawner(difficulty: GameDifficulty) {
    spawnJob?.cancel()
    spawnJob = viewModelScope.launch {
      while (true) {
        delay(difficulty.spawnIntervalMillis)
        spawnWorm()
      }
    }
  }

  private fun spawnWorm() {
    val currentWorms = _worms.value.toMutableList()
    // Limit active worms on screen
    if (currentWorms.size > 10) return

    val rand = Random.Default
    val x = rand.nextFloat() * 80f // 0 to 80%
    val y = rand.nextFloat() * 70f // 0 to 70%

    val roll = rand.nextInt(100)
    val (emoji, points) = when {
      roll < 70 -> "🐛" to 1       // Regular Mopani Worm
      roll < 90 -> "🦋" to 3       // Golden Mopani Moth
      else -> "🪲" to -2           // Sneaky Beetle (penalty)
    }

    val item = WormItem(
      id = nextWormId++,
      emoji = emoji,
      points = points,
      xPercent = x,
      yPercent = y,
      lifetimeMillis = if (emoji == "🪲") 1000L else 1400L
    )

    currentWorms.add(item)
    _worms.value = currentWorms

    // Automatically remove after lifetime
    viewModelScope.launch {
      delay(item.lifetimeMillis)
      removeWorm(item.id)
    }
  }

  fun tapWorm(id: Long) {
    val currentWorms = _worms.value.toMutableList()
    val worm = currentWorms.find { it.id == id } ?: return
    currentWorms.remove(worm)
    _worms.value = currentWorms

    val updatedScore = (_score.value + worm.points).coerceAtLeast(0)
    _score.value = updatedScore
  }

  private fun removeWorm(id: Long) {
    val currentWorms = _worms.value.toMutableList()
    if (currentWorms.removeIf { it.id == id }) {
      _worms.value = currentWorms
    }
  }

  private fun endGame() {
    spawnJob?.cancel()
    gameTimerJob?.cancel()
    _worms.value = emptyList()

    val finalScore = _score.value
    val isNewHigh = finalScore > _highScore.value
    if (isNewHigh) {
      saveHighScore(finalScore)
    }
    _screenState.value = GameScreenState.GameOver(finalScore, isNewHigh)
  }

  private fun stopGame() {
    spawnJob?.cancel()
    gameTimerJob?.cancel()
    _worms.value = emptyList()
  }
}
