package com.example.jeevan.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeevan.Screen
import com.example.jeevan.data.PatientDto
import com.example.jeevan.data.SosState
import com.example.jeevan.ui.theme.*

// ─────────────────────────────────────────────────
// STATUS SCREEN
// ─────────────────────────────────────────────────
@Composable
fun StatusScreen(onDone: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(JeevanBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp).clip(CircleShape).background(JeevanGreenSoft)
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = JeevanGreen, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Ambulance Reached Hospital",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 24.sp,
                textAlign  = TextAlign.Center,
                color      = JeevanOnBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Patient has been safely delivered. Emergency resolved.",
                color = JeevanSubtext,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick  = onDone,
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = JeevanRed)
            ) {
                Text("Done", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────
// DRIVER SCREEN
// ─────────────────────────────────────────────────
@Composable
fun DriverScreen(
    sosState: SosState?,
    onMarkArrived: () -> Unit = {},
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JeevanBackground)
    ) {
        JeevanAppBar(
            showBadge = sosState != null,
            onProfile = { onNavigate(Screen.Profile.route) }
        )
        RoleTabRow(current = "driver", onNavigate = onNavigate)

        if (sosState == null) {
            WaitingCard()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DispatchCard(sos = sosState)
                sosState.ambulance?.let { amb ->
                    AmbulanceInfoCard(
                        driver = amb.driver,
                        number = amb.number,
                        type   = amb.type,
                        rating = amb.rating,
                    )
                }
                // ── Arrived button (Driver marks arrival at patient) ──
                Button(
                    onClick  = onMarkArrived,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = JeevanGreen),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Arrived at Patient Location ✓", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun WaitingCard() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            val spin by rememberInfiniteTransition(label = "wait").animateFloat(
                initialValue = 0f,
                targetValue  = 360f,
                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                label = "spin"
            )
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp).clip(CircleShape).background(JeevanSurface)) {
                Icon(Icons.Filled.Sync, null, tint = JeevanRed,
                    modifier = Modifier.size(40.dp).rotate(spin))
            }
            Spacer(Modifier.height(20.dp))
            Text("🚑 Driver Console", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text("Waiting for SOS dispatch…", color = JeevanSubtext)
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(50), color = JeevanRedSoft) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(JeevanRed))
                    Spacer(Modifier.width(6.dp))
                    Text("Polling every 2s", color = JeevanRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DispatchCard(sos: SosState) {
    Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 4.dp) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(JeevanRed)) {
                    Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("EMERGENCY DISPATCH", color = JeevanRed, fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp, letterSpacing = 0.8.sp)
                    Text(sos.sosId ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                LiveBadge()
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = JeevanBorder)
            Spacer(Modifier.height(14.dp))
            sos.user?.let { InfoDetailRow(Icons.Filled.LocationOn, "Patient Pickup", "${it.lat.fmt(5)}, ${it.lng.fmt(5)}") }
            sos.hospital?.let {
                Spacer(Modifier.height(10.dp))
                InfoDetailRow(Icons.Filled.LocalHospital, it.name, it.speciality, iconTint = JeevanGreen)
            }
            Spacer(Modifier.height(10.dp))
            InfoDetailRow(Icons.Filled.Timer, "ETA", "${sos.etaMin} min  ·  ${sos.totalKm} km")
        }
    }
}

@Composable
private fun AmbulanceInfoCard(driver: String, number: String, type: String, rating: Double) {
    Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🚑", fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(driver, fontWeight = FontWeight.Bold)
                Text("$number  ·  $type", color = JeevanSubtext, fontSize = 13.sp)
            }
            Text("⭐ $rating", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────
// HOSPITAL SCREEN
// ─────────────────────────────────────────────────
@Composable
fun HospitalScreen(
    sosState: SosState?,
    onSubmitPatient: (name: String, condition: String, ambulanceId: String) -> Unit,
    onMarkArrived: () -> Unit = {},
    onNavigate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var ambId by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(sosState?.ambulance?.number) {
        if (ambId.isEmpty()) ambId = sosState?.ambulance?.number ?: ""
    }

    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {
        JeevanAppBar(showBadge = sosState != null, onProfile = { onNavigate(Screen.Profile.route) })
        RoleTabRow(current = "hospital", onNavigate = onNavigate)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Incoming banner
            if (sosState != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = JeevanSurface,
                    modifier = Modifier.fillMaxWidth().border(1.5.dp, JeevanRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(JeevanRed)) {
                            Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("INCOMING PATIENT", color = JeevanRed, fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp, letterSpacing = 0.8.sp)
                            Text(sosState.sosId ?: "", fontWeight = FontWeight.Bold)
                        }
                        LiveBadge()
                    }
                }
            }

            // Form
            Surface(shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 2.dp) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("🏥 Hospital Authority", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("Record incoming patient details", color = JeevanSubtext, fontSize = 13.sp)

                    PatientFormField("Patient Name", name, Icons.Filled.Person, singleLine = true) { name = it }
                    PatientFormField("Condition / Symptoms", condition, Icons.Filled.MedicalServices, singleLine = false) { condition = it }
                    PatientFormField("Ambulance ID", ambId, Icons.Filled.DirectionsCar, singleLine = true) { ambId = it }

                    if (submitted) {
                        Surface(shape = RoundedCornerShape(12.dp), color = JeevanGreenSoft) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = JeevanGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Patient record saved successfully.", color = JeevanGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Button(
                        onClick  = { onSubmitPatient(name, condition, ambId)
                            submitted = true; name = ""; condition = "" },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = JeevanRed)
                    ) {
                        Text("Save Patient Record", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }

                    // ── Hospital marks patient received ──
                    Button(
                        onClick  = onMarkArrived,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = JeevanGreen)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Patient Received at Hospital ✓", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientFormField(label: String, value: String, icon: ImageVector, singleLine: Boolean, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = JeevanOnBackground)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            leadingIcon = { Icon(icon, null, tint = JeevanSubtext, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JeevanRed,
                unfocusedBorderColor = JeevanBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = if (singleLine) 1 else 4,
        )
    }
}

// ─────────────────────────────────────────────────
// ADMIN SCREEN
// ─────────────────────────────────────────────────
@Composable
fun AdminScreen(
    sosState: SosState?,
    patients: List<PatientDto>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(JeevanBackground)) {
        JeevanAppBar(
            showBadge = sosState?.active == true,
            onProfile = { onNavigate(Screen.Profile.route) },
            trailing = {
                IconButton(onClick = onRefresh) {
                    Icon(
                        if (isLoading) Icons.Filled.Sync else Icons.Filled.Refresh,
                        "Refresh", tint = JeevanRed, modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
        RoleTabRow(current = "admin", onNavigate = onNavigate)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, null, tint = JeevanRed, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Admin Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("Auto-refresh every 2s", color = JeevanSubtext, fontSize = 12.sp)
                }
            }

            // Stats grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard("Active SOS", if (sosState?.active == true) "1" else "0",
                    Icons.Filled.Warning, if (sosState?.active == true) JeevanRed else JeevanSubtext, Modifier.weight(1f))
                AdminStatCard("Patients", "${patients.size}", Icons.Filled.People, JeevanOnBackground, Modifier.weight(1f))
                AdminStatCard("ETA", if (sosState != null) "${sosState.etaMin}m" else "—",
                    Icons.Filled.Timer, JeevanOnBackground, Modifier.weight(1f))
                AdminStatCard("System", "Online", Icons.Filled.CheckCircle, JeevanGreen, Modifier.weight(1f))
            }

            // Current SOS
            Surface(shape = RoundedCornerShape(16.dp), color = JeevanSurface, shadowElevation = 2.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("CURRENT SOS", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
                        color = JeevanSubtext, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    if (sosState != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoChipCard("ID", sosState.sosId ?: "—", Modifier.weight(1f))
                            InfoChipCard("Ambulance", sosState.ambulance?.number ?: "—", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoChipCard("Hospital", sosState.hospital?.name ?: "—", Modifier.weight(1f))
                            InfoChipCard("ETA", "${sosState.etaMin} min", Modifier.weight(1f))
                        }
                    } else {
                        Text("No active SOS at the moment.", color = JeevanSubtext)
                    }
                }
            }

            // Patient records table
            Surface(shape = RoundedCornerShape(16.dp), color = JeevanSurface, shadowElevation = 2.dp) {
                Column {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Patient Records", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Surface(shape = CircleShape, color = JeevanRed) {
                            Text("${patients.size}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    HorizontalDivider(color = JeevanBorder)
                    if (patients.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No patient records yet.", color = JeevanSubtext)
                        }
                    } else {
                        patients.forEachIndexed { i, p ->
                            PatientTableRow(index = i + 1, patient = p)
                            if (i < patients.size - 1) HorizontalDivider(color = JeevanBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientTableRow(index: Int, patient: PatientDto) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$index", color = JeevanSubtext, modifier = Modifier.width(24.dp), fontSize = 13.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(patient.name ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(patient.condition ?: "—", color = JeevanSubtext, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(shape = RoundedCornerShape(50), color = JeevanRedSoft) {
            Text(
                patient.ambulanceId ?: patient.ambulanceId2 ?: "—",
                color = JeevanRed, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = JeevanSurface, shadowElevation = 2.dp, modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = JeevanSubtext, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = tint)
        }
    }
}

@Composable
private fun InfoChipCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = JeevanSurface2, modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = JeevanSubtext, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────────

/** Top bar shared by Driver / Hospital / Admin */
@Composable
fun JeevanAppBar(
    showBadge: Boolean = false,
    onProfile: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBadge) {
                Surface(shape = RoundedCornerShape(50), color = JeevanRedSoft,
                    modifier = Modifier.border(1.5.dp, JeevanRed.copy(alpha = 0.4f), RoundedCornerShape(50))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(JeevanRed))
                        Spacer(Modifier.width(6.dp))
                        Text("EMERGENCY ACTIVE", color = JeevanRed, fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp, letterSpacing = 0.8.sp)
                    }
                }
            }
            trailing()
        }
    }
}

/** Role tab navigation strip */
@Composable
fun RoleTabRow(current: String, onNavigate: (String) -> Unit) {
    val tabs = listOf(
        Triple("user",     "👤 User",     Screen.Home.route),
        Triple("driver",   "🚑 Driver",   Screen.Driver.route),
        Triple("hospital", "🏥 Hospital", Screen.Hospital.route),
        Triple("admin",    "🛡 Admin",    Screen.Admin.route),
    )
    Surface(color = JeevanSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { (key, label, route) ->
                val active = current == key
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (active) JeevanRed else JeevanSurface2,
                    modifier = Modifier
                        .border(1.dp, if (active) Color.Transparent else JeevanBorder, RoundedCornerShape(50))
                        .clickable { onNavigate(route) }
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.White else JeevanSubtext,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(JeevanGreen))
        Spacer(Modifier.width(4.dp))
        Text("Live", color = JeevanGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun InfoDetailRow(icon: ImageVector, label: String, value: String, iconTint: Color = JeevanRed) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(value, color = JeevanSubtext, fontSize = 12.sp)
        }
    }
}

private fun Double.fmt(digits: Int) = "%.${digits}f".format(this)
