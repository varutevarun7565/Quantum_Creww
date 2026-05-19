package com.example.jeevan.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeevan.UiState
import com.example.jeevan.data.AgentStatus
import com.example.jeevan.data.AgentStep
import com.example.jeevan.ui.theme.*

@Composable
fun ProcessingScreen(
    uiState: UiState,
    onCancel: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JeevanBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // ── Top Bar ──
            ProcessingTopBar()

            // ── Progress Bar ──
            LinearProgressSection(progress = uiState.processingProgress)

            Spacer(Modifier.height(24.dp))

            // ── Circular AI Coordinator ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularCoordinator()
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text  = "Coordinating...",
                style = MaterialTheme.typography.headlineMedium,
                color = JeevanOnBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Connecting multiple AI agents for rapid response",
                style = MaterialTheme.typography.bodyMedium,
                color = JeevanSubtext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // ── Agent Cards ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.agentSteps.forEach { step ->
                    AgentCard(step = step)
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // ── ETA Footer ──
        ETABottomBar(
            etaMin = uiState.sosState?.etaMin ?: 8,
            etaSec = uiState.sosState?.etaSec ?: 34,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────
// Top Bar with EMERGENCY ACTIVE badge
// ─────────────────────────────────────────────────
@Composable
private fun ProcessingTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JeevanBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text("JEEVAN", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 1.sp)
        }

        // EMERGENCY ACTIVE badge
        Surface(
            shape = RoundedCornerShape(50),
            color = JeevanRedSoft,
            modifier = Modifier.border(1.5.dp, JeevanRed.copy(alpha = 0.4f), RoundedCornerShape(50))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(JeevanRed)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "EMERGENCY ACTIVE",
                    color = JeevanRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
// "AI RESPONSE SEQUENCE  75% Complete" + green bar
// ─────────────────────────────────────────────────
@Composable
private fun LinearProgressSection(progress: Float) {
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "progress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "AI RESPONSE SEQUENCE",
                style = MaterialTheme.typography.labelSmall,
                color = JeevanRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = "${(animProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.labelSmall,
                color = JeevanSubtext,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color = JeevanGreen,
            trackColor = JeevanBorder,
            strokeCap = StrokeCap.Round,
        )
    }
}

// ─────────────────────────────────────────────────
// Circular Coordinator (spinning rings + AI icon)
// ─────────────────────────────────────────────────
@Composable
private fun CircularCoordinator() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotateCw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rotateCw"
    )
    val rotateCcw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "rotateCcw"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        // Outer faded ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(JeevanRedSoft)
        )
        // Inner white circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(JeevanSurface)
        )
        // Spinning outer arc (simulated with rotate)
        CircularProgressIndicator(
            progress = { 0.75f },
            modifier = Modifier
                .size(160.dp)
                .rotate(rotateCw),
            color = JeevanRed,
            trackColor = JeevanRedSoft,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
        )
        // Counter-spinning inner arc
        CircularProgressIndicator(
            progress = { 0.5f },
            modifier = Modifier
                .size(120.dp)
                .rotate(rotateCcw),
            color = SonarRing3,
            trackColor = Color.Transparent,
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round,
        )
        // AI Network icon in center
        Icon(
            imageVector = Icons.Filled.Hub,
            contentDescription = "AI Agent",
            tint = JeevanRed,
            modifier = Modifier.size(40.dp)
        )
    }
}

// ─────────────────────────────────────────────────
// Agent Card
// ─────────────────────────────────────────────────
@Composable
fun AgentCard(step: AgentStep) {
    val (borderColor, bgColor) = when (step.status) {
        AgentStatus.SUCCESS -> Pair(JeevanGreen.copy(alpha = 0.3f), JeevanSurface)
        AgentStatus.ACTIVE  -> Pair(JeevanRed.copy(alpha = 0.5f),  JeevanRedSoft)
        AgentStatus.ERROR   -> Pair(JeevanRed, JeevanRedSoft)
        AgentStatus.PENDING -> Pair(JeevanBorder, JeevanSurface)
    }

    val spinAnim = rememberInfiniteTransition(label = "agentSpin")
    val spin by spinAnim.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "spin"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Status icon
            AgentStatusIcon(status = step.status, spin = spin)

            Spacer(Modifier.width(14.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = step.agentName,
                    style = MaterialTheme.typography.titleMedium,
                    color = JeevanOnBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text  = "${step.description}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = JeevanSubtext,
                    )
                    Text(
                        text = step.detail,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = if (step.status == AgentStatus.ACTIVE) FontStyle.Italic else FontStyle.Normal,
                            fontWeight = if (step.status == AgentStatus.SUCCESS) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = when (step.status) {
                            AgentStatus.ACTIVE  -> JeevanRed
                            AgentStatus.SUCCESS -> JeevanOnBackground
                            else                -> JeevanSubtext
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right label
            when (step.status) {
                AgentStatus.SUCCESS -> Text(
                    text = "SUCCESS",
                    color = JeevanGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                )
                AgentStatus.PENDING -> Text(
                    text = "PENDING",
                    color = JeevanPending,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                )
                AgentStatus.ACTIVE -> Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "active",
                    tint = JeevanRed,
                    modifier = Modifier.size(20.dp),
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun AgentStatusIcon(status: AgentStatus, spin: Float) {
    val (bg, icon, tint) = when (status) {
        AgentStatus.SUCCESS -> Triple(JeevanGreen,        Icons.Filled.CheckCircle,    Color.White)
        AgentStatus.ACTIVE  -> Triple(JeevanRedSoft,      Icons.Filled.Sync,            JeevanRed)
        AgentStatus.ERROR   -> Triple(JeevanRed,          Icons.Filled.Error,           Color.White)
        AgentStatus.PENDING -> Triple(Color(0xFFF3F4F6),  Icons.Filled.HourglassEmpty,  JeevanPending)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bg)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .then(if (status == AgentStatus.ACTIVE) Modifier.rotate(spin) else Modifier)
        )
    }
}

// ─────────────────────────────────────────────────
// ETA Bottom Bar (dark navy) — Live Help removed
// ─────────────────────────────────────────────────
@Composable
private fun ETABottomBar(etaMin: Int, etaSec: Int, modifier: Modifier = Modifier) {
    Surface(
        color = JeevanNavy,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "ETA TO HOSPITAL",
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${etaMin}m ${etaSec.toString().padStart(2, '0')}s",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
            )
        }
    }
}


