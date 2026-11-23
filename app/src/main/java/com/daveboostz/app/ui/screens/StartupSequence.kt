package com.daveboostz.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.daveboostz.app.R

/** Short, one-shot vector boot reveal. No video, shader, blur or continuously running particles. */
@Composable
fun StartupSequence(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    var flash by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180)
        flash = true
        progress.animateTo(1f, tween(durationMillis = 2050))
        delay(420)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050507)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val p = progress.value
            val red = Color(0xFFFF3442)
            val softRed = red.copy(alpha = .30f + .35f * p)

            // Four angular energy streams converge on the centre and become the emblem outline.
            val bolts = listOf(
                listOf(Offset(size.width*.12f,size.height*.18f), Offset(size.width*.24f,size.height*.29f), Offset(size.width*.20f,size.height*.38f), Offset(cx-34f,cy-18f)),
                listOf(Offset(size.width*.88f,size.height*.20f), Offset(size.width*.76f,size.height*.31f), Offset(size.width*.81f,size.height*.39f), Offset(cx+34f,cy-18f)),
                listOf(Offset(size.width*.16f,size.height*.82f), Offset(size.width*.29f,size.height*.70f), Offset(size.width*.25f,size.height*.62f), Offset(cx-30f,cy+23f)),
                listOf(Offset(size.width*.84f,size.height*.82f), Offset(size.width*.71f,size.height*.70f), Offset(size.width*.75f,size.height*.62f), Offset(cx+30f,cy+23f))
            )
            bolts.forEachIndexed { index, points ->
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    lineTo(points[3].x, points[3].y)
                }
                drawPath(path, red.copy(alpha = (1f-p*.25f).coerceIn(0f,1f)), style = Stroke(width = if (flash) 2.2f else 1f))
            }

            // Stylized DB crest assembles in short strokes from the outside toward its core.
            val emblem = listOf(
                Offset(cx-34f,cy-38f) to Offset(cx+20f,cy-38f),
                Offset(cx+20f,cy-38f) to Offset(cx+37f,cy-20f),
                Offset(cx+37f,cy-20f) to Offset(cx+37f,cy+20f),
                Offset(cx+37f,cy+20f) to Offset(cx+20f,cy+38f),
                Offset(cx+20f,cy+38f) to Offset(cx-34f,cy+38f),
                Offset(cx-34f,cy+38f) to Offset(cx-34f,cy-38f),
                Offset(cx-17f,cy-18f) to Offset(cx+15f,cy-18f),
                Offset(cx+15f,cy-18f) to Offset(cx+15f,cy+18f),
                Offset(cx+15f,cy+18f) to Offset(cx-17f,cy+18f)
            )
            emblem.forEachIndexed { i, segment ->
                val local = ((p * emblem.size) - i).coerceIn(0f,1f)
                if (local > 0f) {
                    val end = Offset(
                        segment.first.x + (segment.second.x-segment.first.x)*local,
                        segment.first.y + (segment.second.y-segment.first.y)*local
                    )
                    drawLine(red, segment.first, end, strokeWidth = 3.2f)
                }
            }
            if (p > .68f) {
                val alpha = ((p-.68f)/.32f).coerceIn(0f,1f)
                drawCircle(red.copy(alpha = alpha*.7f), radius = 62f, center = Offset(cx,cy), style = Stroke(width = 1.4f))
                drawCircle(Color.White.copy(alpha = alpha*.75f), radius = 3.5f, center = Offset(cx,cy))
            }
        }

        Image(
            painter = painterResource(R.drawable.daveboostz_emblem),
            contentDescription = "DaveBoostz emblem",
            modifier = Modifier.size(176.dp).graphicsLayer {
                val reveal = ((progress.value - .70f) / .30f).coerceIn(0f, 1f)
                alpha = reveal
                scaleX = .72f + .28f * reveal
                scaleY = .72f + .28f * reveal
            }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 132.dp)) {
            Text("DAVEBOOSTZ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(7.dp))
            Text("UNLEASH YOUR DEVICE.", color = Color(0xFFFF5962), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
        }
    }
}
