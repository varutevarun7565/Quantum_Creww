@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jeevan.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.example.jeevan.Screen
import com.example.jeevan.UiState
import com.example.jeevan.data.JeevanRepository
import com.example.jeevan.data.LatLng
import com.example.jeevan.data.SosState
import com.example.jeevan.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// ═══════════════════════════════════════════════════════════
// HOSPITAL BOTTOM NAV — History | Live Tracking | Profile
// ═══════════════════════════════════════════════════════════
@Composable
private fun HospitalBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    Surface(color = JeevanSurface, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(62.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HospitalTabItem(Icons.Outlined.History, "HISTORY",
                currentRoute == Screen.HospitalDashboard.route) { onNavigate(Screen.HospitalDashboard.route) }
            HospitalTabItem(Icons.Outlined.Map, "TRACKING",
                currentRoute == Screen.HospitalTracking.route) { onNavigate(Screen.HospitalTracking.route) }
            HospitalTabItem(Icons.Outlined.Person, "PROFILE",
                currentRoute == Screen.Profile.route) { onNavigate(Screen.Profile.route) }
        }
    }
}

@Composable
private fun HospitalTabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) JeevanGreen else JeevanSubtext
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        if (selected) Box(Modifier.size(4.dp).clip(CircleShape).background(JeevanGreen))
    }
}

// ═══════════════════════════════════════════════════════════
// HOSPITAL DASHBOARD — Incoming patient notification
// ═══════════════════════════════════════════════════════════
@Composable
fun HospitalDashboardScreen(
    uiState: UiState,
    onPatientReceived: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val sos = uiState.sosState

    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {
        HospitalTopBar()
        HospitalBottomNav(currentRoute = Screen.HospitalDashboard.route, onNavigate = onNavigate)

        if (sos != null && sos.active) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Patient incoming notification
                PatientIncomingBanner(sos = sos)
                PatientDetailsCard(sos = sos)
                AmbulanceStatusCard(sos = sos)
                PrepareBedsCard()
                // Patient received button
                Button(
                    onClick  = onPatientReceived,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = JeevanGreen),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Patient Received ✓", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(JeevanSurface)
                    ) { Text("🏥", fontSize = 48.sp) }
                    Spacer(Modifier.height(20.dp))
                    Text("No Incoming Patient", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You will be notified here when an SOS emergency is dispatched and patient is en route.",
                        color = JeevanSubtext, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = RoundedCornerShape(50), color = JeevanGreenSoft) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(JeevanGreen))
                            Spacer(Modifier.width(6.dp))
                            Text("Hospital Ready", color = JeevanGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientIncomingBanner(sos: SosState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = JeevanGreen,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                ) { Icon(Icons.Filled.NotificationImportant, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("🚨 PATIENT INCOMING", color = Color.White.copy(0.8f),
                        fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.8.sp)
                    Text("Emergency — Prepare Immediately", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HospBannerChip("⏱ ETA", "${sos.etaMin} min")
                HospBannerChip("📍 Distance", "${sos.totalKm} km")
                HospBannerChip("🚑 Ambulance", sos.ambulance?.number?.take(8) ?: "—")
            }

            Spacer(Modifier.height(14.dp))
            sos.ambulance?.let { amb ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Driver: ${amb.driver}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${amb.number} · ${amb.type}", color = Color.White.copy(0.75f), fontSize = 11.sp)
                    }
                    Text("⭐ ${amb.rating}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HospBannerChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(0.15f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, color = Color.White.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PatientDetailsCard(sos: SosState) {
    Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PersonPin, null, tint = JeevanRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Accident Details", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = JeevanBorder)
            Spacer(Modifier.height(12.dp))
            HospInfoRow(Icons.Filled.Badge, "SOS ID", sos.sosId ?: "Unknown")
            Spacer(Modifier.height(10.dp))
            sos.user?.let {
                HospInfoRow(Icons.Filled.LocationOn, "Pickup Coordinates",
                    "${it.lat.fmt5h()}, ${it.lng.fmt5h()}", JeevanRed)
            }
            Spacer(Modifier.height(10.dp))
            sos.hospital?.let {
                HospInfoRow(Icons.Filled.LocalHospital, "Receiving Hospital",
                    "${it.name}\n${it.speciality}", JeevanGreen)
            }
        }
    }
}

@Composable
private fun AmbulanceStatusCard(sos: SosState) {
    val amb = sos.ambulance ?: return
    val context = LocalContext.current
    Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsCar, null, tint = JeevanNavy, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ambulance Status", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = JeevanBorder)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚑", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(amb.driver, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text("${amb.number} · ${amb.type}", color = JeevanSubtext, fontSize = 12.sp)
                    Text("⭐ ${amb.rating}", color = JeevanSubtext, fontSize = 12.sp)
                }
                val stageColor = when (sos.stage) {
                    "to_hospital", "at_hospital" -> JeevanGreen
                    "at_user" -> Color(0xFFF59E0B)
                    else -> JeevanRed
                }
                val stageLabel = when (sos.stage) {
                    "to_user"     -> "Picking Up"
                    "at_user"     -> "At Scene"
                    "to_hospital" -> "En Route"
                    "at_hospital" -> "Arrived"
                    else          -> "In Service"
                }
                Surface(shape = RoundedCornerShape(50), color = stageColor.copy(0.12f)) {
                    Text(stageLabel, color = stageColor, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }

            if (amb.phone.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${amb.phone}"))) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JeevanNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JeevanNavy.copy(0.4f)),
                ) {
                    Icon(Icons.Filled.Phone, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call Driver: ${amb.phone}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PrepareBedsCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFF7ED), shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFED7AA))
            ) { Icon(Icons.Filled.MedicalServices, null, tint = Color(0xFFEA580C), modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Prepare Emergency Bay", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF9A3412))
                Text("Patient arriving soon — alert trauma team", color = Color(0xFFEA580C), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HospInfoRow(icon: ImageVector, label: String, value: String, tint: Color = JeevanNavy) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HOSPITAL TRACKING — Map (ambulance + hospital + route)
// ═══════════════════════════════════════════════════════════
@Composable
fun HospitalTrackingScreen(
    uiState: UiState,
    onNavigate: (String) -> Unit,
) {
    val sos     = uiState.sosState
    val context = LocalContext.current
    val stage   = sos?.stage ?: ""
    var route   by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Route changes per stage:
    //   to_user / at_user  → ambulance → patient (pickup route)
    //   to_hospital / at_hospital → ambulance → hospital (delivery route)
    LaunchedEffect(sos?.ambulance?.lat, sos?.ambulance?.lng, stage) {
        val amb  = sos?.ambulance ?: return@LaunchedEffect
        val dest = when (stage) {
            "to_user", "at_user" -> sos.user ?: return@LaunchedEffect
            else                 -> sos.hospital?.let { LatLng(it.lat, it.lng) } ?: return@LaunchedEffect
        }
        route = JeevanRepository.getOsrmRoute(LatLng(amb.lat, amb.lng), dest)
    }

    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {
        HospitalTopBar()
        HospitalBottomNav(currentRoute = Screen.HospitalTracking.route, onNavigate = onNavigate)

        if (sos == null || !sos.active) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🗺️", fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No Active Emergency", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("Live ambulance tracking appears here when SOS is active.", color = JeevanSubtext, textAlign = TextAlign.Center)
                }
            }
            return
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen map
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        val center = sos.hospital?.let { GeoPoint(it.lat, it.lng) } ?: GeoPoint(28.6139, 77.2090)
                        controller.setCenter(center)
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    // ── Ambulance marker (always moving) ──
                    sos.ambulance?.let {
                        Marker(mapView).apply {
                            position = GeoPoint(it.lat, it.lng)
                            title    = "🚑 ${it.driver}"
                            icon     = createTextMarker(context, "🚑", android.graphics.Color.parseColor("#1E40AF"))
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(this)
                        }
                    }

                    // ── Hospital marker ──
                    sos.hospital?.let {
                        Marker(mapView).apply {
                            position = GeoPoint(it.lat, it.lng)
                            title    = "🏥 ${it.name}"
                            icon     = createTextMarker(context, "🏥", android.graphics.Color.parseColor("#16A34A"))
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(this)
                        }
                    }

                    // ── Patient/Accident marker ──
                    // Phase 1 (to_user / at_user): static at accident location
                    // Phase 2 (to_hospital / at_hospital): patient is ABOARD ambulance → marker follows ambulance
                    val patientLat: Double?
                    val patientLng: Double?
                    when (stage) {
                        "to_hospital", "at_hospital" -> {
                            // Patient is in ambulance — draw at ambulance position
                            patientLat = sos.ambulance?.lat
                            patientLng = sos.ambulance?.lng
                        }
                        else -> {
                            // Patient at accident location
                            patientLat = sos.user?.lat
                            patientLng = sos.user?.lng
                        }
                    }
                    if (patientLat != null && patientLng != null) {
                        Marker(mapView).apply {
                            position = GeoPoint(patientLat, patientLng)
                            title    = if (stage == "to_hospital" || stage == "at_hospital")
                                "🏥 Patient aboard ambulance" else "📍 Accident Location"
                            icon     = createTextMarker(context, "📍",
                                if (stage == "to_hospital" || stage == "at_hospital")
                                    android.graphics.Color.parseColor("#1E40AF")  // blue = with ambulance
                                else
                                    android.graphics.Color.parseColor("#B91C1C")  // red = at scene
                            )
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(this)
                        }
                    }

                    // ── Road route line ──
                    val pts = if (route.size >= 2) route else {
                        val amb = sos.ambulance?.let { LatLng(it.lat, it.lng) }
                        val dest = when (stage) {
                            "to_user", "at_user" -> sos.user
                            else -> sos.hospital?.let { LatLng(it.lat, it.lng) }
                        }
                        listOfNotNull(amb, dest)
                    }
                    if (pts.size >= 2) {
                        Polyline().apply {
                            pts.forEach { addPoint(GeoPoint(it.lat, it.lng)) }
                            outlinePaint.color = when (stage) {
                                "to_user", "at_user" -> android.graphics.Color.parseColor("#B91C1C") // red to patient
                                else                 -> android.graphics.Color.parseColor("#16A34A") // green to hospital
                            }
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                            mapView.overlays.add(this)
                        }
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )


            // Stage badge
            Surface(
                shape = RoundedCornerShape(50), color = JeevanSurface, shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val (dot, label) = when (stage) {
                        "to_user"     -> JeevanRed   to "PICKING UP PATIENT"
                        "at_user"     -> Color(0xFFF59E0B) to "LOADING PATIENT"
                        "to_hospital" -> JeevanGreen to "EN ROUTE TO HOSPITAL"
                        "at_hospital" -> JeevanGreen to "PATIENT ARRIVED"
                        else          -> JeevanNavy  to "MONITORING"
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }

            // Bottom info card
            Surface(
                color = JeevanSurface, shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(Modifier.navigationBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏥", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sos.hospital?.name ?: "—", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("${sos.hospital?.speciality ?: ""} · Receiving Bay Ready", color = JeevanSubtext, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = JeevanBorder)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("ETA", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                val etaText = if (uiState.etaRemainSec > 0) {
                                    val m = uiState.etaRemainSec / 60; val s = uiState.etaRemainSec % 60
                                    "${m}m ${s.toString().padStart(2,'0')}s"
                                } else "Arrived"
                                Text(etaText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                                    color = if (uiState.etaRemainSec > 0) JeevanOnBackground else JeevanGreen)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("AMBULANCE", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(sos.ambulance?.number?.take(10) ?: "—", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("STATUS", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                val s = when (stage) {
                                    "to_user" -> "🔴 Pickup"; "at_user" -> "🟡 Loading"
                                    "to_hospital" -> "🔵 Transit"; "at_hospital" -> "🟢 Here"
                                    else -> "🟡 Active"
                                }
                                Text(s, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HospitalTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth().background(JeevanBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("JEEVAN", color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
                Text("Hospital Console", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(shape = RoundedCornerShape(50), color = JeevanGreenSoft) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🏥", fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text("Hospital", color = JeevanGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

private fun Double.fmt5h() = "%.5f".format(this)
