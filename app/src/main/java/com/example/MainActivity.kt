package com.example

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MopaniCatchTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MopaniCatchTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MopaniCatchApp()
        }
      }
    }
  }
}

@Composable
fun MopaniCatchApp(viewModel: GameViewModel = viewModel()) {
  val screenState by viewModel.screenState.collectAsState()
  val score by viewModel.score.collectAsState()
  val timeLeft by viewModel.timeLeft.collectAsState()
  val worms by viewModel.worms.collectAsState()
  val highScore by viewModel.highScore.collectAsState()

  val context = LocalContext.current
  val vibrator = remember {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun triggerHaptic() {
    try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(50)
      }
    } catch (_: Exception) {}
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedSwitcher(targetState = screenState) { state ->
      when (state) {
        is GameScreenState.Menu -> {
          MenuScreen(
            highScore = highScore,
            onStartGame = { diff -> viewModel.startGame(diff) },
            onOpenHowToPlay = { viewModel.setScreen(GameScreenState.HowToPlay) }
          )
        }
        is GameScreenState.Playing -> {
          GameScreen(
            difficulty = state.difficulty,
            score = score,
            timeLeft = timeLeft,
            worms = worms,
            onTapWorm = { id ->
              triggerHaptic()
              viewModel.tapWorm(id)
            },
            onQuit = { viewModel.setScreen(GameScreenState.Menu) }
          )
        }
        is GameScreenState.GameOver -> {
          GameOverScreen(
            score = state.score,
            highScore = highScore,
            isNewHighScore = state.isNewHighScore,
            onPlayAgain = { viewModel.startGame(state.score.let { GameDifficulty.NORMAL }) },
            onHome = { viewModel.setScreen(GameScreenState.Menu) }
          )
        }
        is GameScreenState.HowToPlay -> {
          HowToPlayScreen(
            onBack = { viewModel.setScreen(GameScreenState.Menu) }
          )
        }
        is GameScreenState.HighScores -> {
          // Handled via Menu or back
        }
      }
    }
  }
}

@Composable
fun AnimatedSwitcher(
  targetState: GameScreenState,
  content: @Composable (GameScreenState) -> Unit
) {
  Crossfade(targetState = targetState, animationSpec = tween(300), label = "ScreenCrossfade") { state ->
    content(state)
  }
}

@Composable
fun MenuScreen(
  highScore: Int,
  onStartGame: (GameDifficulty) -> Unit,
  onOpenHowToPlay: () -> Unit
) {
  var selectedDifficulty by remember { mutableStateOf(GameDifficulty.NORMAL) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.background
          )
        )
      )
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Title Banner Icon
    Box(
      modifier = Modifier
        .size(96.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary),
      contentAlignment = Alignment.Center
    ) {
      Text(text = "🐛", fontSize = 48.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Mopani Catch",
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Text(
      text = "Tap the crawling Mopani worms before time runs out!",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // High Score Badge
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth(0.8f)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "High Score: $highScore",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Difficulty Selector
    Text(
      text = "Select Difficulty",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
      GameDifficulty.values().forEach { diff ->
        FilterChip(
          selected = selectedDifficulty == diff,
          onClick = { selectedDifficulty = diff },
          label = { Text(diff.title.substringBefore(" ")) },
          modifier = Modifier.testTag("difficulty_${diff.name.lowercase()}")
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Play Button
    Button(
      onClick = { onStartGame(selectedDifficulty) },
      modifier = Modifier
        .fillMaxWidth(0.8f)
        .height(56.dp)
        .testTag("play_button"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "Start Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // How to Play Button
    OutlinedButton(
      onClick = onOpenHowToPlay,
      modifier = Modifier
        .fillMaxWidth(0.8f)
        .height(50.dp)
        .testTag("how_to_play_button"),
      shape = RoundedCornerShape(16.dp)
    ) {
      Icon(Icons.Default.Info, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "How to Play")
    }
  }
}

@Composable
fun GameScreen(
  difficulty: GameDifficulty,
  score: Int,
  timeLeft: Int,
  worms: List<WormItem>,
  onTapWorm: (Long) -> Unit,
  onQuit: () -> Unit
) {
  BackHandler {
    onQuit()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onQuit, modifier = Modifier.testTag("quit_button")) {
        Icon(Icons.Default.Close, contentDescription = "Quit Game")
      }

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Score: ", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "$score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (timeLeft <= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${timeLeft}s",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (timeLeft <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    // Game Area
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF86EFAC), // Soft mint green field
              Color(0xFF34D399)
            )
          )
        )
        .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
      if (worms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = "Watch out for crawling worms! 🐛",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF065F46),
            fontWeight = FontWeight.Bold
          )
        }
      }

      worms.forEach { worm ->
        Box(
          modifier = Modifier
            .offset(x = worm.xPercent.dp * 4, y = worm.yPercent.dp * 6)
            .size(56.dp)
            .testTag("worm_${worm.id}")
            .clickable { onTapWorm(worm.id) },
          contentAlignment = Alignment.Center
        ) {
          val scaleAnim = remember { Animatable(0.5f) }
          LaunchedEffect(worm.id) {
            scaleAnim.animateTo(
              targetValue = 1f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
              )
            )
          }

          Box(
            modifier = Modifier
              .size(48.dp)
              .scale(scaleAnim.value)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.85f))
              .border(2.dp, if (worm.points > 0) Color.Green else Color.Red, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(text = worm.emoji, fontSize = 28.sp)
          }
        }
      }
    }
  }
}

@Composable
fun GameOverScreen(
  score: Int,
  highScore: Int,
  isNewHighScore: Boolean,
  onPlayAgain: () -> Unit,
  onHome: () -> Unit
) {
  BackHandler {
    onHome()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = "🏆", fontSize = 40.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Game Over!",
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    if (isNewHighScore) {
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "🎉 New High Score! 🎉",
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = Color.Black,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Card(
      modifier = Modifier.fillMaxWidth(0.85f),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Final Score", style = MaterialTheme.typography.bodyMedium)
        Text(
          text = "$score",
          style = MaterialTheme.typography.displayMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "High Score", style = MaterialTheme.typography.bodySmall)
            Text(text = "$highScore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onPlayAgain,
      modifier = Modifier
        .fillMaxWidth(0.8f)
        .height(56.dp)
        .testTag("play_again_button"),
      shape = RoundedCornerShape(16.dp)
    ) {
      Icon(Icons.Default.Refresh, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = onHome,
      modifier = Modifier
        .fillMaxWidth(0.8f)
        .height(50.dp)
        .testTag("home_button"),
      shape = RoundedCornerShape(16.dp)
    ) {
      Icon(Icons.Default.Home, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "Main Menu")
    }
  }
}

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
  BackHandler {
    onBack()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp, bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "How to Play",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        RuleItem(emoji = "🐛", title = "Green Mopani Worm", description = "Tap quickly to score +1 point!")
        RuleItem(emoji = "🦋", title = "Golden Mopani Moth", description = "Rare and fast! Tap for +3 bonus points!")
        RuleItem(emoji = "🪲", title = "Sneaky Beetle", description = "Watch out! Tapping this reduces your score by -2 points.")
        RuleItem(emoji = "⏱️", title = "Beat the Clock", description = "Catch as many worms as you can before the timer runs out!")
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onBack,
      modifier = Modifier
        .fillMaxWidth(0.8f)
        .height(52.dp),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(text = "Got It!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun RuleItem(emoji: String, title: String, description: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = emoji, fontSize = 24.sp)
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column {
      Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
  }
}
