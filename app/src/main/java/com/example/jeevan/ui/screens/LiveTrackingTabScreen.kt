package com.example.jeevan.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.example.jeevan.AppStage
import com.example.jeevan.Screen
import com.example.jeevan.UiState
import com.example.jeevan.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// ─────────────────────────────────────────────────
// LIVE TRACKING TAB
// Shows real-time ambulance map when SOS is active,
// otherwise shows a "no emergency" placeholder.
// ─────────────────────────────────────────────────
@Composable
fun LiveTrackingTabScreen(
    uiState: UiState,
    onSOS: () -> Unit,
    onMarkArrived: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val sos    = uiState.sosState
    val active = sos != null && sos.active

    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(JeevanBackground).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("JEEVAN", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 1.sp)
            }
            if (active) {
                Surface(shape = RoundedCornerShape(50), color = JeevanRedSoft) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(JeevanRed))
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.8.sp)
                    }
                }
            }
        }

        // Bottom nav at top of scrollable content area
        JeevanBottomNav(currentRoute = Screen.LiveTracking.route, onNavigate = onNavigate)

        if (!active) {
            // ── No active SOS ──
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(JeevanSurface),
                    ) {
                        Icon(Icons.Filled.LocationOff, null, tint = JeevanBorder, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("No Active Emergency", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Live tracking will appear here when you trigger an SOS. Your ambulance position updates in real-time.",
                        color = JeevanSubtext, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick  = { onNavigate(Screen.Home.route); onSOS() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = JeevanRed),
                    ) {
                        Icon(Icons.Filled.Warning, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Trigger SOS", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            // ── Active SOS — show map + info ──
            Box(modifier = Modifier.fillMaxSize()) {

                // Full-screen map
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(14.5)
                            val center = sos?.user?.let { GeoPoint(it.lat, it.lng) } ?: GeoPoint(28.6139, 77.2090)
                            controller.setCenter(center)
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()

                        // User
                        sos?.user?.let { loc ->
                            Marker(mapView).apply {
                                position = GeoPoint(loc.lat, loc.lng)
                                title    = "📍 You are here"
                                mapView.overlays.add(this)
                            }
                        }

                        // Ambulance — key marker that moves
                        sos?.ambulance?.let { amb ->
                            Marker(mapView).apply {
                                position = GeoPoint(amb.lat, amb.lng)
                                title    = "🚑 ${amb.driver}"
                                snippet  = amb.number
                                mapView.overlays.add(this)
                            }
                        }

                        // Hospital
                        sos?.hospital?.let { hosp ->
                            Marker(mapView).apply {
                                position = GeoPoint(hosp.lat, hosp.lng)
                                title    = "🏥 ${hosp.name}"
                                mapView.overlays.add(this)
                            }
                        }

                        // Route: ambulance → user (red)
                        if (sos?.ambulance != null && sos.user != null) {
                            Polyline().apply {
                                addPoint(GeoPoint(sos.ambulance.lat, sos.ambulance.lng))
                                addPoint(GeoPoint(sos.user.lat, sos.user.lng))
                                outlinePaint.color       = android.graphics.Color.parseColor("#B91C1C")
                                outlinePaint.strokeWidth = 8f
                                mapView.overlays.add(this)
                            }
                        }

                        // Route: user → hospital (green dashed)
                        if (sos?.user != null && sos.hospital != null) {
                            Polyline().apply {
                                addPoint(GeoPoint(sos.user.lat, sos.user.lng))
                                addPoint(GeoPoint(sos.hospital.lat, sos.hospital.lng))
                                outlinePaint.color       = android.graphics.Color.parseColor("#16A34A")
                                outlinePaint.strokeWidth = 6f
                                outlinePaint.pathEffect  = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f)
                                mapView.overlays.add(this)
                            }
                        }

                        mapView.invalidate()
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // ETA overlay (top center)
                Surface(
                    shape  = RoundedCornerShape(16.dp),
                    color  = JeevanNavy.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("ETA", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            val remainSec = uiState.etaRemainSec
                            val min = remainSec / 60
                            val sec = remainSec % 60
                            Text(
                                if (remainSec > 0) "${min}m ${sec.toString().padStart(2,'0')}s" else "Arrived!",
                                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val stage = sos?.stage ?: ""
                            val (statusText, statusColor) = when {
                                stage == "at_user" || sos?.etaMin == 0 -> "🟢 Arrived" to JeevanGreen
                                stage == "to_user" -> "🔴 En Route to You" to JeevanRed
                                else -> "🟡 On the way" to Color(0xFFF59E0B)
                            }
                            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${sos?.totalKm ?: 0.0} km total", color = Color.White.copy(0.6f), fontSize = 11.sp)
                        }
                    }
                }

                // Ambulance info card (bottom)
                if (sos?.ambulance != null) {
                    Surface(
                        color  = JeevanSurface,
                        shadowElevation = 16.dp,
                        shape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.navigationBarsPadding().padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚑", fontSize = 28.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(sos.ambulance.driver, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text("${sos.ambulance.number} · ${sos.ambulance.type}", color = JeevanSubtext, fontSize = 13.sp)
                                }
                                Text("⭐ ${sos.ambulance.rating}", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))

                            val isArrived = sos.stage == "at_user" || sos.etaMin == 0
                            if (isArrived) {
                                Surface(shape = RoundedCornerShape(12.dp), color = JeevanGreenSoft) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = JeevanGreen, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Ambulance has arrived!", color = JeevanGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick  = onMarkArrived,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape    = RoundedCornerShape(14.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = JeevanGreen),
                                ) {
                                    Text("Mark as Complete ✓", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
