package com.example.jeevan.ui.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import com.example.jeevan.UiState
import com.example.jeevan.data.JeevanRepository
import com.example.jeevan.data.LatLng
import com.example.jeevan.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun LiveTrackingScreen(
    uiState: UiState,
    onCancel: () -> Unit,
    onMarkArrived: () -> Unit,
) {
    val sos     = uiState.sosState
    val context = LocalContext.current

    // ── OSRM road routes (fetched once when screen loads) ──
    var ambToUserRoute  by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var userToHospRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Fetch Phase 1 route: ambulance → user (once)
    LaunchedEffect(sos?.sosId) {
        val amb  = sos?.ambulance ?: return@LaunchedEffect
        val user = sos.user       ?: return@LaunchedEffect
        ambToUserRoute = JeevanRepository.getOsrmRoute(LatLng(amb.lat, amb.lng), user)
    }

    // Fetch Phase 2 route: user → hospital (when stage switches to to_hospital)
    val stage = sos?.stage ?: ""
    LaunchedEffect(stage) {
        val user = sos?.user     ?: return@LaunchedEffect
        val hosp = sos?.hospital ?: return@LaunchedEffect
        if (stage == "to_hospital" || stage == "at_hospital") {
            userToHospRoute = JeevanRepository.getOsrmRoute(
                user, LatLng(hosp.lat, hosp.lng)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen OSMDroid Map ──
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                Configuration.getInstance().userAgentValue = ctx.packageName // Required by OSMDroid to load map tiles
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
                
                // Keep the camera centered on the active real-time location
                sos?.user?.let { loc ->
                    val centerPoint = GeoPoint(loc.lat, loc.lng)
                    mapView.controller.setCenter(centerPoint)
                }

                // ── User / Accident marker (red circle) ──
                sos?.user?.let { loc ->
                    Marker(mapView).apply {
                        position = GeoPoint(loc.lat, loc.lng)
                        title    = "🔴 Accident Location"
                        snippet  = "${loc.lat.fmt(5)}, ${loc.lng.fmt(5)}"
                        icon     = createTextMarker(context, "📍", android.graphics.Color.parseColor("#B91C1C"))
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(this)
                    }
                }

                // ── Ambulance marker (blue) ──
                sos?.ambulance?.let { amb ->
                    Marker(mapView).apply {
                        position = GeoPoint(amb.lat, amb.lng)
                        title    = "🚑 ${amb.driver}"
                        snippet  = "${amb.number} · ${amb.type}"
                        icon     = createTextMarker(context, "🚑", android.graphics.Color.parseColor("#1E40AF"))
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(this)
                    }
                }

                // ── Hospital marker (green) ──
                sos?.hospital?.let { hosp ->
                    Marker(mapView).apply {
                        position = GeoPoint(hosp.lat, hosp.lng)
                        title    = "🏥 ${hosp.name}"
                        snippet  = "${hosp.speciality} · ${hosp.beds} beds"
                        icon     = createTextMarker(context, "🏥", android.graphics.Color.parseColor("#16A34A"))
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(this)
                    }
                }

                // ── Route: ambulance → user (OSRM road route, red) ──
                val ambRoute = if (ambToUserRoute.size >= 2) ambToUserRoute else
                    listOfNotNull(sos?.ambulance?.let { LatLng(it.lat, it.lng) }, sos?.user)
                if (ambRoute.size >= 2) {
                    Polyline().apply {
                        ambRoute.forEach { addPoint(GeoPoint(it.lat, it.lng)) }
                        outlinePaint.color       = android.graphics.Color.parseColor("#B91C1C")
                        outlinePaint.strokeWidth = 10f
                        outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                        mapView.overlays.add(this)
                    }
                }

                // ── Route: user → hospital (OSRM road route, green dashed) ──
                val hospRoute = if (userToHospRoute.size >= 2) userToHospRoute else
                    listOfNotNull(sos?.user, sos?.hospital?.let { LatLng(it.lat, it.lng) })
                if (hospRoute.size >= 2) {
                    Polyline().apply {
                        hospRoute.forEach { addPoint(GeoPoint(it.lat, it.lng)) }
                        outlinePaint.color       = android.graphics.Color.parseColor("#16A34A")
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                        outlinePaint.pathEffect  = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f)
                        mapView.overlays.add(this)
                    }
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Stage badge — top left ──
        Surface(
            shape  = RoundedCornerShape(50),
            color  = JeevanSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 56.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val (dot, label) = when (stage) {
                    "to_user"     -> JeevanRed   to "EN ROUTE TO YOU"
                    "at_user"     -> Color(0xFFF59E0B) to "PICKING UP PATIENT"
                    "to_hospital" -> JeevanNavy  to "EN ROUTE TO HOSPITAL"
                    "at_hospital" -> JeevanGreen to "ARRIVED AT HOSPITAL"
                    else          -> JeevanRed   to "EN ROUTE TO YOU"
                }
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(6.dp))
                Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
                    color = JeevanOnBackground, letterSpacing = 0.5.sp)
            }
        }

        // ── ETA Card — top right ──
        if (sos != null) {
            Surface(
                shape  = RoundedCornerShape(16.dp),
                color  = JeevanSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(JeevanRed)
                    ) {
                        Icon(Icons.Filled.Timer, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("ARRIVING IN", fontSize = 10.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold, color = JeevanSubtext)
                        val min = uiState.etaRemainSec / 60
                        val sec = uiState.etaRemainSec % 60
                        Text(
                            if (uiState.etaRemainSec > 0) "${min}m ${sec.toString().padStart(2,'0')}s" else "ARRIVED!",
                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            color = if (uiState.etaRemainSec > 0) JeevanOnBackground else JeevanGreen,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    VerticalDivider(modifier = Modifier.height(36.dp), color = JeevanBorder, thickness = 1.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        val stageLabel = when (stage) {
                            "to_user"     -> "🔴 To You"
                            "at_user"     -> "🟡 At Scene"
                            "to_hospital" -> "🔵 To Hospital"
                            "at_hospital" -> "🟢 Arrived"
                            else          -> "🔴 En Route"
                        }
                        Text("STATUS", fontSize = 10.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold, color = JeevanSubtext)
                        Text(stageLabel, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = JeevanOnBackground)
                    }
                }
            }
        }

        // ── Bottom Info Card ──
        if (sos?.ambulance != null) {
            Surface(
                color  = JeevanSurface,
                shadowElevation = 16.dp,
                shape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(modifier = Modifier.navigationBarsPadding().padding(20.dp)) {

                    // Ambulance row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(JeevanRedSoft)
                        ) { Text("🚑", fontSize = 24.sp) }

                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sos.ambulance.driver, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(50), color = JeevanRedSoft) {
                                    Text(sos.ambulance.type, color = JeevanRed, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Text("${sos.ambulance.number}  ⭐ ${sos.ambulance.rating}", color = JeevanSubtext, fontSize = 13.sp)
                        }

                        // ── Call button (functional) ──
                        IconButton(
                            onClick = {
                                val phone = sos.ambulance.phone.ifEmpty { "108" }
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(JeevanGreen)
                        ) {
                            Icon(Icons.Filled.Phone, "Call Driver", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = JeevanBorder)
                    Spacer(Modifier.height(14.dp))

                    // Hospital row
                    sos.hospital?.let { hosp ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(JeevanGreenSoft)
                            ) {
                                Icon(Icons.Filled.LocalHospital, null, tint = JeevanGreen, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hosp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${hosp.speciality} · ${hosp.beds} beds · ${hosp.distanceKm} km",
                                    color = JeevanSubtext, fontSize = 12.sp)
                            }
                            Icon(Icons.Filled.Navigation, null, tint = JeevanSubtext, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // ── Cancel SOS only (no "Arrived" for user — that's for Driver/Hospital) ──
                    OutlinedButton(
                        onClick  = onCancel,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.5.dp, JeevanRed.copy(alpha = 0.5f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = JeevanRed)
                    ) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel SOS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}


private fun Double.fmt(digits: Int) = "%.${digits}f".format(this)

