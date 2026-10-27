package com.aismartmath.board.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aismartmath.board.board.BoardScreen
import com.aismartmath.board.model.Topics
import com.aismartmath.board.ui.components.FloatingSymbols
import com.aismartmath.board.ui.home.HomeScreen
import com.aismartmath.board.ui.splash.SplashScreen
import com.aismartmath.board.ui.theme.BoardColors

@Composable
fun SmartMathApp() {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var topicId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = topicId != null) { topicId = null }

    Box(Modifier.fillMaxSize().background(BoardColors.Bg)) {
        FloatingSymbols()

        AnimatedContent(
            targetState = topicId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                        (slideOutHorizontally(tween(350)) { -it / 4 } + fadeOut(tween(250)))
                } else {
                    (slideInHorizontally(tween(350)) { -it / 4 } + fadeIn(tween(350))) togetherWith
                        (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(250)))
                }
            },
            label = "screen",
        ) { id ->
            val topic = id?.let { Topics.byId[it] }
            if (topic == null) {
                HomeScreen(ready = splashDone, onOpen = { topicId = it.id })
            } else {
                BoardScreen(topic = topic, onBack = { topicId = null })
            }
        }

        AnimatedVisibility(
            visible = !splashDone,
            enter = EnterTransition.None,
            exit = fadeOut(tween(600)) + scaleOut(targetScale = 1.3f, animationSpec = tween(600)),
        ) {
            SplashScreen(onFinished = { splashDone = true })
        }
    }
}
