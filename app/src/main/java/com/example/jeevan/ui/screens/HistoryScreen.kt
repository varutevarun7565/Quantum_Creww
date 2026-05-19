package com.example.jeevan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeevan.Screen
import com.example.jeevan.data.SosHistoryEntry
import com.example.jeevan.ui.theme.*

@Composable
fun HistoryScreen(
    history: List<SosHistoryEntry>,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(JeevanBackground)
    ) {
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
            Icon(Icons.Filled.History, null, tint = JeevanSubtext, modifier = Modifier.size(22.dp))
        }

        // Bottom tab row
        JeevanBottomNav(currentRoute = Screen.History.route, onNavigate = onNavigate)

        // Title
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(JeevanRedSoft)
            ) {
                Icon(Icons.Filled.History, null, tint = JeevanRed, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("SOS History", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("${history.size} record${if (history.size != 1) "s" else ""} found", color = JeevanSubtext, fontSize = 13.sp)
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Filled.HistoryToggleOff, null, tint = JeevanBorder, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No SOS History", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = JeevanSubtext)
                    Spacer(Modifier.height(8.dp))
                    Text("Your past emergency events will appear here after your first SOS.", color = JeevanSubtext, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(history, key = { it.id }) { entry ->
                    HistoryEntryCard(entry)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: SosHistoryEntry) {
    val isCompleted = entry.stage == "completed"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = JeevanSurface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {

            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(if (isCompleted) JeevanGreenSoft else JeevanRedSoft)
                ) {
                    Icon(
                        if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        null,
                        tint = if (isCompleted) JeevanGreen else JeevanRed,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.id, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(entry.timestamp, color = JeevanSubtext, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCompleted) JeevanGreenSoft else JeevanRedSoft,
                ) {
                    Text(
                        if (isCompleted) "Completed" else "Cancelled",
                        color     = if (isCompleted) JeevanGreen else JeevanRed,
                        fontWeight = FontWeight.Bold,
                        fontSize  = 11.sp,
                        modifier  = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = JeevanBorder)
            Spacer(Modifier.height(14.dp))

            // Detail grid
            HistoryDetailRow(Icons.Filled.LocationOn, "Your Location", entry.userAddress)
            Spacer(Modifier.height(10.dp))
            HistoryDetailRow(Icons.Filled.DirectionsCar, "Ambulance", entry.ambulanceNumber, iconTint = JeevanNavy)
            Spacer(Modifier.height(10.dp))
            HistoryDetailRow(Icons.Filled.LocalHospital, "Hospital", entry.hospitalName, iconTint = JeevanGreen)
            Spacer(Modifier.height(10.dp))

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryStatChip("⏱ ETA", "${entry.etaMin} min", Modifier.weight(1f))
                HistoryStatChip("📍 Distance", "${entry.distanceKm} km", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: Color = JeevanRed,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HistoryStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = JeevanSurface2, modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}
