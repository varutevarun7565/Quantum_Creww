@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jeevan.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.jeevan.data.SosHistoryEntry
import com.example.jeevan.data.SosState
import com.example.jeevan.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// ═══════════════════════════════════════════════════════════
// DRIVER BOTTOM NAV — History | Live Tracking | Profile
// ═══════════════════════════════════════════════════════════
@Composable
private fun DriverBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    Surface(color = JeevanSurface, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(62.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DriverTabItem(Icons.Outlined.History, "HISTORY",
                currentRoute == Screen.DriverDashboard.route) { onNavigate(Screen.DriverDashboard.route) }
            DriverTabItem(Icons.Outlined.Map, "TRACKING",
                currentRoute == Screen.DriverTracking.route) { onNavigate(Screen.DriverTracking.route) }
            DriverTabItem(Icons.Outlined.Person, "PROFILE",
                currentRoute == Screen.Profile.route) { onNavigate(Screen.Profile.route) }
        }
    }
}

@Composable
private fun DriverTabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) JeevanNavy else JeevanSubtext
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        if (selected) Box(Modifier.size(4.dp).clip(CircleShape).background(JeevanNavy))
    }
}

// ═══════════════════════════════════════════════════════════
// DRIVER DASHBOARD — Dispatch history / Waiting
// ═══════════════════════════════════════════════════════════
@Composable
fun DriverDashboardScreen(
    uiState: UiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val sos = uiState.sosState

    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {
        // Top bar
        DriverTopBar()
        DriverBottomNav(currentRoute = Screen.DriverDashboard.route, onNavigate = onNavigate)

        if (sos != null && sos.active) {
            // ── Incoming SOS notification ──
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { IncomingDispatchBanner(sos = sos, onAccept = onAccept, onDecline = onDecline, onNavigate = onNavigate) }
                item { DriverAccidentDetailCard(sos = sos) }
                item { DriverNearbyHospitalsCard(hospitals = uiState.nearbyHospitals) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        } else {
            // ── Waiting state ──
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(JeevanSurface)
                    ) { Text("🚑", fontSize = 48.sp) }
                    Spacer(Modifier.height(20.dp))
                    Text("No Active Dispatch", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You will be notified here when a new SOS emergency is assigned to you.",
                        color = JeevanSubtext, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = RoundedCornerShape(50), color = JeevanRedSoft) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(JeevanRed))
                            Spacer(Modifier.width(6.dp))
                            Text("Monitoring for SOS…", color = JeevanRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingDispatchBanner(sos: SosState, onAccept: () -> Unit, onDecline: () -> Unit, onNavigate: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = JeevanRed,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                ) { Icon(Icons.Filled.NotificationImportant, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("🚨 EMERGENCY DISPATCH", color = Color.White.copy(0.8f),
                        fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.8.sp)
                    Text("New SOS — Immediate Response", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            sos.user?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${it.lat.fmt5()}, ${it.lng.fmt5()}", color = Color.White.copy(0.9f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
            sos.hospital?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalHospital, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("→ ${it.name}", color = Color.White.copy(0.9f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ETA: ${sos.etaMin} min  ·  ${sos.totalKm} km", color = Color.White.copy(0.9f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAccept(); onNavigate(Screen.DriverTracking.route) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = JeevanRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Accept", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(0.4f)),
                ) {
                    Text("Decline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DriverAccidentDetailCard(sos: SosState) {
    Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Accident Details", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = JeevanBorder)
            Spacer(Modifier.height(12.dp))
            DriverDetailRow(Icons.Filled.Badge, "SOS ID", sos.sosId ?: "SOS-UNKNOWN")
            Spacer(Modifier.height(10.dp))
            sos.user?.let {
                DriverDetailRow(Icons.Filled.LocationOn, "Accident Coordinates",
                    "${it.lat.fmt5()}, ${it.lng.fmt5()}", iconTint = JeevanRed)
            }
            Spacer(Modifier.height(10.dp))
            sos.hospital?.let {
                DriverDetailRow(Icons.Filled.LocalHospital, "Assigned Hospital",
                    "${it.name}\n${it.speciality} · ${it.beds} beds", iconTint = JeevanGreen)
            }
        }
    }
}

@Composable
private fun DriverNearbyHospitalsCard(hospitals: List<com.example.jeevan.data.HospitalInfo>) {
    Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalHospital, null, tint = JeevanGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nearby Hospitals", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = JeevanBorder)
            Spacer(Modifier.height(12.dp))
            if (hospitals.isEmpty()) {
                Text("Fetching nearby hospitals…", color = JeevanSubtext, fontSize = 13.sp)
            } else {
                hospitals.take(4).forEachIndexed { i, h ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(JeevanGreenSoft)
                        ) { Text("${i + 1}", color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${h.speciality} · ${h.distanceKm} km", color = JeevanSubtext, fontSize = 11.sp)
                        }
                        Text("${h.beds} beds", color = JeevanSubtext, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverDetailRow(icon: ImageVector, label: String, value: String, iconTint: Color = JeevanNavy) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// DRIVER LIVE TRACKING — Map + Accept + Move to patient
// ═══════════════════════════════════════════════════════════
@Composable
fun DriverTrackingScreen(
    uiState: UiState,
    onMarkArrived: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val sos     = uiState.sosState
    val context = LocalContext.current
    var route   by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val stage   = sos?.stage ?: ""

    // Fetch OSRM road route from ambulance to patient
    LaunchedEffect(sos?.ambulance?.lat, sos?.ambulance?.lng) {
        val amb  = sos?.ambulance ?: return@LaunchedEffect
        val user = sos.user       ?: return@LaunchedEffect
        route = JeevanRepository.getOsrmRoute(LatLng(amb.lat, amb.lng), user)
    }

    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {
        DriverTopBar()
        DriverBottomNav(currentRoute = Screen.DriverTracking.route, onNavigate = onNavigate)

        if (sos == null || !sos.active) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🗺️", fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No Active Emergency", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("Map will appear when SOS is dispatched.", color = JeevanSubtext, textAlign = TextAlign.Center)
                }
            }
            return
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Full screen map
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.5)
                        val center = sos.user?.let { GeoPoint(it.lat, it.lng) } ?: GeoPoint(28.6139, 77.2090)
                        controller.setCenter(center)
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    // Accident location marker
                    sos.user?.let {
                        Marker(mapView).apply {
                            position = GeoPoint(it.lat, it.lng)
                            title    = "🔴 Accident Location"
                            icon     = createTextMarker(context, "📍", android.graphics.Color.parseColor("#B91C1C"))
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(this)
                        }
                    }

                    // Ambulance (driver) marker
                    sos.ambulance?.let {
                        Marker(mapView).apply {
                            position = GeoPoint(it.lat, it.lng)
                            title    = "🚑 Your Vehicle"
                            icon     = createTextMarker(context, "🚑", android.graphics.Color.parseColor("#1E40AF"))
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(this)
                        }
                    }

                    // Hospital marker
                    sos.hospital?.let {
                        Marker(mapView).apply {
                            position = GeoPoint(it.lat, it.lng)
                            title    = "🏥 ${it.name}"
                            icon     = createTextMarker(context, "🏥", android.graphics.Color.parseColor("#16A34A"))
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(this)
                        }
                    }

                    // Route line (road-following)
                    val routePoints = if (route.size >= 2) route else
                        listOfNotNull(sos.ambulance?.let { LatLng(it.lat, it.lng) }, sos.user)
                    if (routePoints.size >= 2) {
                        Polyline().apply {
                            routePoints.forEach { addPoint(GeoPoint(it.lat, it.lng)) }
                            outlinePaint.color       = android.graphics.Color.parseColor("#1E40AF")
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                            mapView.overlays.add(this)
                        }
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Stage badge top-left
            Surface(
                shape  = RoundedCornerShape(50), color = JeevanSurface, shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val (dot, label) = when (stage) {
                        "to_user"     -> JeevanRed   to "EN ROUTE TO PATIENT"
                        "at_user"     -> Color(0xFFF59E0B) to "AT ACCIDENT SITE"
                        "to_hospital" -> JeevanNavy  to "EN ROUTE TO HOSPITAL"
                        "at_hospital" -> JeevanGreen to "ARRIVED AT HOSPITAL"
                        else          -> JeevanRed   to "TRACKING ACTIVE"
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }

            // Bottom info card
            Surface(
                color  = JeevanSurface, shadowElevation = 16.dp,
                shape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(Modifier.navigationBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Patient Location", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(
                                sos.user?.let { "${it.lat.fmt5()}, ${it.lng.fmt5()}" } ?: "—",
                                color = JeevanSubtext, fontSize = 12.sp
                            )
                        }
                        sos.ambulance?.phone?.let { phone ->
                            if (phone.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                    },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(JeevanGreen)
                                ) { Icon(Icons.Filled.Phone, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = JeevanBorder)
                    Spacer(Modifier.height(12.dp))

                    // ETA pill
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("ETA", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${sos.etaMin} min", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("DISTANCE", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${sos.totalKm} km", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("HOSPITAL", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(sos.hospital?.name?.take(10) ?: "—", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Arrived button (driver only)
                    val arrivedLabel = when (stage) {
                        "to_user"     -> "Mark: Arrived at Patient ✓"
                        "at_user"     -> "Mark: Departed to Hospital ✓"
                        "to_hospital" -> "Mark: Patient Delivered ✓"
                        else          -> "Mission Complete ✓"
                    }
                    Button(
                        onClick  = onMarkArrived,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = JeevanGreen),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(arrivedLabel, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth().background(JeevanBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = JeevanNavy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("JEEVAN", color = JeevanNavy, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
                Text("Driver Console", color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(shape = RoundedCornerShape(50), color = Color(0xFF1E3A5F).copy(0.08f)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🚑", fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text("Driver", color = JeevanNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

private fun Double.fmt5() = "%.5f".format(this)
