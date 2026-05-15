package com.moto.airecorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moto.airecorder.demo.TranscriptLine

@Composable
internal fun ControlButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        modifier = Modifier.border(1.dp, Color(0xFF3A383D), RoundedCornerShape(12.dp)),
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun LevelMeter() {
    val bars = listOf(7, 12, 16, 10, 22, 28, 18, 34, 14, 26, 38, 24, 16, 32, 42, 30, 20, 28, 36, 22, 16, 30, 20, 26, 12, 22, 16, 14, 20, 10, 14, 8)
    Canvas(modifier = Modifier.size(width = 190.dp, height = 44.dp)) {
        val gap = size.width / bars.size
        bars.forEachIndexed { index, barHeight ->
            val color = if (index in 14..17) Color(0xFFE5484D) else Color(0xFF8A857A)
            val x = index * gap + gap / 2
            val half = barHeight / 2f
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f - half),
                end = Offset(x, size.height / 2f + half),
                strokeWidth = 3f,
            )
        }
    }
}

@Composable
internal fun TranscriptLineRow(line: TranscriptLine, highlighted: Boolean, compact: Boolean) {
    val borderColor = if (highlighted) Color(0xFFE5484D) else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(if (compact) 0.dp else 12.dp),
    ) {
        Text(
            "${line.speaker} · ${formatTimer(line.timestampMs)}",
            color = speakerColor(line.speaker),
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.sp else 14.sp,
        )
        Text(
            "“${line.text}”",
            color = if (highlighted) Color.White else Color(0xFFC9C3B7),
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.sp else 15.sp,
        )
    }
}

private fun speakerColor(speaker: String): Color {
    return when (speaker) {
        "小雅" -> Color(0xFFE9DFC4)
        "阿俊" -> Color(0xFF9BD1E5)
        "佳怡" -> Color(0xFFF4A679)
        else -> Color(0xFFC7B7F0)
    }
}
