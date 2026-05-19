package com.example.jeevan.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeevan.Screen
import com.example.jeevan.UiState
import com.example.jeevan.ui.theme.*

@Composable
fun HomeScreen(
    uiState: UiState,
    onSOS: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val role = uiState.authState.currentUser?.role
    val isUser = role == null || role == com.example.jeevan.data.UserRole.USER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JeevanBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Top Nav Bar ──
            JeevanTopBar(onNavigate)

            // ── Main Content (fills between navbar and bottom tab) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isUser) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(Modifier.height(16.dp))

                        // ── Title Text ──
                        Text(
                            text = "In case of emergency,\npress SOS",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 34.sp,
                            ),
                            textAlign = TextAlign.Center,
                            color = JeevanOnBackground,
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Your location and medical profile will\nbe shared with emergency responders instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = JeevanSubtext,
                            lineHeight = 20.sp,
                        )

                        Spacer(Modifier.height(36.dp))

                        // ── SOS Button with Sonar Rings ──
                        SonarSOSButton(
                            enabled = true, // Forced true so it always works even without GPS lock
                            onClick = onSOS,
                        )

                        Spacer(Modifier.height(28.dp))

                        // ── Location Pill ──
                        LocationPill(address = uiState.locationAddress, status = uiState.locStatus)

                        Spacer(Modifier.height(24.dp))

                        // ── Call Ambulance Button (dials 108) ──
                        CallAmbulanceButton()

                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    // Non-USER role should not land here, but if they do, show login prompt
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("🔒", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("SOS Unavailable", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "SOS is only available for registered Users.\nPlease log in with a User account.",
                            color = JeevanSubtext, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onNavigate(Screen.Login.route) },
                            shape  = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = JeevanRed),
                        ) { Text("Log in as User", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // ── Bottom Tab Bar ──
            JeevanBottomNav(currentRoute = Screen.Home.route, onNavigate = onNavigate, showSos = isUser)
        }

        // ── Floating Chatbot Button ──
        if (isUser) {
            FloatingActionButton(
                onClick = { onNavigate(Screen.Chatbot.route) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp),
                containerColor = Color(0xFFFFF9E6),
                contentColor = Color(0xFF4A4A4A),
                shape = CircleShape
            ) {
                Text(text = "🤖", fontSize = 28.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────
// SOS Button with Sonar (Concentric) Rings
// ─────────────────────────────────────────────────
@Composable
fun SonarSOSButton(enabled: Boolean, onClick: () -> Unit) {
    // Breathing animation
    val breatheAnim = rememberInfiniteTransition(label = "breathe")
    val buttonScale by breatheAnim.animateFloat(
        initialValue = 1f,
        targetValue  = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonScale"
    )

    // Outer pulse ring animation
    val pulseAnim1 = rememberInfiniteTransition(label = "pulse1")
    val pulseScale1 by pulseAnim1.animateFloat(
        initialValue = 1f,
        targetValue  = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale1"
    )
    val pulseAlpha1 by pulseAnim1.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha1"
    )
    val pulseAnim2 = rememberInfiniteTransition(label = "pulse2")
    val pulseScale2 by pulseAnim2.animateFloat(
        initialValue = 1f,
        targetValue  = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale2"
    )
    val pulseAlpha2 by pulseAnim2.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha2"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(290.dp)
    ) {
        // ── Outermost static sonar ring ──
        Box(
            modifier = Modifier
                .size(290.dp)
                .clip(CircleShape)
                .background(SonarRing1)
        )
        // ── Middle sonar ring ──
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(SonarRing2)
        )
        // ── Inner sonar ring ──
        Box(
            modifier = Modifier
                .size(175.dp)
                .clip(CircleShape)
                .background(SonarRing3)
        )

        // ── Animated pulse ring 1 ──
        Box(
            modifier = Modifier
                .size(145.dp)
                .scale(pulseScale1)
                .clip(CircleShape)
                .background(JeevanRed.copy(alpha = pulseAlpha1))
        )
        // ── Animated pulse ring 2 ──
        Box(
            modifier = Modifier
                .size(145.dp)
                .scale(pulseScale2)
                .clip(CircleShape)
                .background(JeevanRed.copy(alpha = pulseAlpha2))
        )

        // ── SOS Button ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(if (enabled) buttonScale else 1f)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(JeevanRedLight, JeevanRed, JeevanRedDark)
                    )
                )
        ) {
            Text(
                text = "SOS",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────
// Location Pill
// ─────────────────────────────────────────────────
@Composable
fun LocationPill(address: String, status: String) {
    val icon = when (status) {
        "ready"     -> Icons.Filled.LocationOn
        "error"     -> Icons.Filled.LocationOff
        else        -> Icons.Filled.MyLocation
    }
    val iconColor = when (status) {
        "ready"  -> JeevanRed
        "error"  -> Color(0xFFEF4444)
        else     -> JeevanSubtext
    }

    Surface(
        shape  = RoundedCornerShape(50),
        color  = JeevanSurface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = Modifier.border(
            width = 1.5.dp,
            color = JeevanBorder,
            shape = RoundedCornerShape(50)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Location",
                tint  = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = address,
                style = MaterialTheme.typography.titleMedium,
                color = JeevanOnBackground,
                maxLines = 1,
            )
        }
    }
}

// ─────────────────────────────────────────────────
// Call Ambulance Button (outlined)
// ─────────────────────────────────────────────────
@Composable
fun CallAmbulanceButton() {
    val context = androidx.compose.ui.platform.LocalContext.current
    OutlinedButton(
        onClick  = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:108")
            )
            context.startActivity(intent)
        },
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = JeevanRed),
        border   = BorderStroke(2.dp, JeevanRed),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text       = "Call 108 – Ambulance",
            fontWeight = FontWeight.Bold,
            fontSize   = 17.sp,
        )
    }
}

// ─────────────────────────────────────────────────
// Top Navigation Bar  (* JEEVAN  |  profile)
// ─────────────────────────────────────────────────
@Composable
fun JeevanTopBar(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JeevanBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Brand — asterisk + JEEVAN
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "✦",
                color = JeevanRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "JEEVAN",
                color = JeevanRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
            )
        }

        // Profile avatar (dark circle)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(JeevanAvatarBg)
                .clickable { onNavigate(Screen.Profile.route) }
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────
// Bottom Tab Navigation
// ─────────────────────────────────────────────────
@Composable
fun JeevanBottomNav(currentRoute: String, onNavigate: (String) -> Unit, showSos: Boolean = true) {
    Surface(
        color = JeevanSurface,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // SOS tab: only visible for USER role
            if (showSos) {
                BottomTabItem(
                    icon       = Icons.Filled.Warning,
                    label      = "SOS",
                    selected   = currentRoute == Screen.Home.route,
                    activeColor = JeevanRed,
                    onClick    = { onNavigate(Screen.Home.route) }
                )
            }
            BottomTabItem(
                icon       = Icons.Outlined.History,
                label      = "HISTORY",
                selected   = currentRoute == Screen.History.route,
                onClick    = { onNavigate(Screen.History.route) }
            )
            BottomTabItem(
                icon       = Icons.Outlined.Favorite,
                label      = "TRACKING",
                selected   = currentRoute == Screen.LiveTracking.route,
                onClick    = { onNavigate(Screen.LiveTracking.route) }
            )
            BottomTabItem(
                icon       = Icons.Outlined.Person,
                label      = "PROFILE",
                selected   = currentRoute == Screen.Profile.route,
                onClick    = { onNavigate(Screen.Profile.route) }
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    activeColor: Color = JeevanRed,
    onClick: () -> Unit,
) {
    val color = if (selected) activeColor else JeevanSubtext
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (selected && label == "SOS") {
            // SOS tab has a red background circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(JeevanRedSoft)
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            }
        } else {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text  = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
