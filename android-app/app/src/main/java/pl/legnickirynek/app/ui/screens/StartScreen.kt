package pl.legnickirynek.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StartScreen() {
    val transition = rememberInfiniteTransition(label = "start")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(pulse)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .semantics { contentDescription = "Logo Legnickiego Rynku" },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(76.dp)) {
                    val skyline = Path().apply {
                        moveTo(0f, size.height * .78f)
                        lineTo(size.width * .12f, size.height * .60f)
                        lineTo(size.width * .24f, size.height * .72f)
                        lineTo(size.width * .36f, size.height * .28f)
                        lineTo(size.width * .43f, size.height * .72f)
                        lineTo(size.width * .57f, size.height * .72f)
                        lineTo(size.width * .68f, size.height * .18f)
                        lineTo(size.width * .76f, size.height * .72f)
                        lineTo(size.width, size.height * .52f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(skyline, color = Color(0xFFFF6B4A))
                }
            }
            Text(
                text = "Legnicki Rynek",
                modifier = Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Wszystko, co lokalne. W jednym miejscu.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f),
                textAlign = TextAlign.Center
            )
        }
    }
}
