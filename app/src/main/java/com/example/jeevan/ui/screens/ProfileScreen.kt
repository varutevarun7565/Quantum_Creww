package com.example.jeevan.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeevan.JeevanViewModel
import com.example.jeevan.Screen
import com.example.jeevan.data.UserProfile
import com.example.jeevan.data.UserRole
import com.example.jeevan.ui.theme.*
import java.io.File

// ─────────────────────────────────────────────────
// PROFILE SCREEN (gated by auth)
// ─────────────────────────────────────────────────
@Composable
fun ProfileScreen(
    viewModel: JeevanViewModel,
    isLoggedIn: Boolean,
    currentUser: UserProfile?,
    onNavigate: (String) -> Unit,
) {
    if (!isLoggedIn || currentUser == null) {
        AuthLandingScreen(onNavigate = onNavigate)
    } else if (currentUser.role == UserRole.HOSPITAL) {
        // Hospital role: no photo, show hospital info card
        HospitalProfileScreen(
            user       = currentUser,
            onLogout   = { viewModel.logout(); onNavigate(Screen.Home.route) },
        )
    } else {
        LoggedInProfileScreen(
            user       = currentUser,
            onEdit     = { onNavigate(Screen.ProfileEdit.route) },
            onLogout   = { viewModel.logout(); onNavigate(Screen.Home.route) },
            onNavigate = onNavigate,
        )
    }
}

// ─────────────────────────────────────────────────
// LOGGED-IN PROFILE VIEW
// ─────────────────────────────────────────────────
@Composable
private fun LoggedInProfileScreen(
    user: UserProfile,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(JeevanBackground).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("JEEVAN", color = JeevanRed, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 1.sp)
            }
            OutlinedButton(
                onClick = onLogout,
                shape   = RoundedCornerShape(50),
                border  = BorderStroke(1.dp, JeevanRed),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = JeevanRed),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Logout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Avatar — shows real photo if available
        val context = LocalContext.current
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(100.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp).clip(CircleShape).background(JeevanAvatarBg),
            ) {
                val bitmap = remember(user.photoUri) {
                    user.photoUri?.let { uriStr ->
                        try {
                            val uri = Uri.parse(uriStr)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) { null }
                    }
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap             = bitmap.asImageBitmap(),
                        contentDescription = "Profile Photo",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(52.dp))
                }
            }
            // Edit badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).clip(CircleShape).background(JeevanRed),
            ) {
                Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(user.name.ifEmpty { user.userId }, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)

        // Role badge
        val (roleColor, roleLabel) = when (user.role) {
            UserRole.USER     -> Pair(JeevanRed,   "👤 User")
            UserRole.DRIVER   -> Pair(JeevanNavy,  "🚑 Driver")
            UserRole.HOSPITAL -> Pair(JeevanGreen, "🏥 Hospital")
            UserRole.ADMIN    -> Pair(JeevanNavy,  "🛡 Admin")
        }
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(50), color = roleColor.copy(alpha = 0.12f)) {
            Text(roleLabel, color = roleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }

        Spacer(Modifier.height(6.dp))
        if (user.bloodGroup.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bloodtype, null, tint = JeevanRed, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Blood Group: ${user.bloodGroup}", color = JeevanSubtext, fontSize = 13.sp)
                if (user.age.isNotEmpty()) {
                    Text("  ·  Age: ${user.age}", color = JeevanSubtext, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Info card
        Surface(
            shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Personal Information", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                HorizontalDivider(color = JeevanBorder)
                if (user.contactNumber.isNotEmpty())
                    ProfileInfoRow(Icons.Filled.Phone, "Contact", user.contactNumber)
                if (user.emergencyContact.isNotEmpty())
                    ProfileInfoRow(Icons.Filled.ContactPhone, "Emergency Contact", user.emergencyContact)
                if (user.medicalCondition.isNotEmpty())
                    ProfileInfoRow(Icons.Filled.MedicalServices, "Medical Condition", user.medicalCondition)
                if (user.contactNumber.isEmpty() && user.emergencyContact.isEmpty() && user.medicalCondition.isEmpty())
                    Text("No additional info. Tap Edit to add details.", color = JeevanSubtext, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Edit Profile button
        Button(
            onClick   = onEdit,
            modifier  = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 20.dp),
            shape     = RoundedCornerShape(14.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = JeevanRed),
        ) {
            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit Profile", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(14.dp))

        // Role-based quick nav (only for non-user roles)
        if (user.role != UserRole.USER) {
            Surface(
                shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Role Access", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = JeevanSubtext)
                    Spacer(Modifier.height(10.dp))
                    val roleRoute = when (user.role) {
                        UserRole.DRIVER   -> Screen.Driver.route
                        UserRole.HOSPITAL -> Screen.Hospital.route
                        UserRole.ADMIN    -> Screen.Admin.route
                        else              -> Screen.Home.route
                    }
                    TextButton(onClick = { onNavigate(roleRoute) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open ${user.role.name.lowercase().replaceFirstChar { it.uppercase() }} Dashboard",
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = JeevanRed, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────
// PROFILE EDIT SCREEN
// ─────────────────────────────────────────────────
@Composable
fun ProfileEditScreen(
    currentUser: UserProfile,
    viewModel: JeevanViewModel,
    onBack: () -> Unit,
) {
    var name      by remember { mutableStateOf(currentUser.name) }
    var contact   by remember { mutableStateOf(currentUser.contactNumber) }
    var emergency by remember { mutableStateOf(currentUser.emergencyContact) }
    var age       by remember { mutableStateOf(currentUser.age) }
    var condition by remember { mutableStateOf(currentUser.medicalCondition) }
    var photoUri  by remember { mutableStateOf<Uri?>(currentUser.photoUri?.let { Uri.parse(it) }) }
    var saved     by remember { mutableStateOf(false) }
    val context   = LocalContext.current

    // Gallery launcher — copies picked image to app-private storage for persistence
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Decode and save to internal storage so URI never expires
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                if (bmp != null) {
                    val file = File(context.filesDir, "profile_${currentUser.userId}.jpg")
                    file.outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    photoUri = Uri.fromFile(file)
                    saved = false
                }
            } catch (e: Exception) {
                // fallback: just use raw URI
                photoUri = uri
                saved = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(JeevanBackground).verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = JeevanRed)
            }
            Spacer(Modifier.width(8.dp))
            Text("Edit Profile", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        // Photo section
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(100.dp)) {
                    // Show actual selected photo or fallback icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(JeevanAvatarBg),
                    ) {
                        val bitmap = remember(photoUri) {
                            photoUri?.let { uri ->
                                try {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }
                                } catch (_: Exception) { null }
                            }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(52.dp))
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(JeevanRed),
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                    Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(16.dp), tint = JeevanRed)
                    Spacer(Modifier.width(4.dp))
                    val label = if (photoUri != null) "Photo selected ✓" else "Change Photo"
                    Text(label, color = if (photoUri != null) JeevanGreen else JeevanRed,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Personal Information", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)

                AuthField("Name", name, Icons.Filled.Person) { name = it; saved = false }
                AuthField("Contact Number", contact, Icons.Filled.Phone, keyboardType = KeyboardType.Phone) { contact = it; saved = false }
                AuthField("Emergency Contact", emergency, Icons.Filled.ContactPhone, keyboardType = KeyboardType.Phone) { emergency = it; saved = false }
                AuthField("Age", age, Icons.Filled.Cake, keyboardType = KeyboardType.Number) { age = it; saved = false }
                AuthField("Medical Condition", condition, Icons.Filled.MedicalServices) { condition = it; saved = false }

                if (saved) {
                    Surface(shape = RoundedCornerShape(10.dp), color = JeevanGreenSoft) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = JeevanGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Profile saved successfully!", color = JeevanGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            currentUser.copy(
                                name             = name,
                                contactNumber    = contact,
                                emergencyContact = emergency,
                                age              = age,
                                medicalCondition = condition,
                                photoUri         = photoUri?.toString() ?: currentUser.photoUri,
                            )
                        )
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = JeevanRed),
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Changes", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────
// HOSPITAL PROFILE SCREEN (no photo, show hospital info)
// ─────────────────────────────────────────────────
@Composable
fun HospitalProfileScreen(
    user: UserProfile,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(JeevanBackground).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("JEEVAN", color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 1.sp)
            }
            OutlinedButton(
                onClick = onLogout,
                shape   = RoundedCornerShape(50),
                border  = BorderStroke(1.dp, JeevanGreen),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = JeevanGreen),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Logout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Hospital avatar icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp).clip(CircleShape).background(JeevanGreenSoft)
        ) { Text("🏥", fontSize = 52.sp) }

        Spacer(Modifier.height(12.dp))
        Text(user.name.ifEmpty { user.userId }, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(50), color = JeevanGreenSoft) {
            Text(
                "HOSPITAL",
                color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Hospital Info Card
        Surface(
            shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Hospital Information", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                HorizontalDivider(color = JeevanBorder)
                HospInfoItemRow(Icons.Filled.Badge, "Hospital ID", user.userId)
                HospInfoItemRow(Icons.Filled.LocalHospital, "Name", user.name.ifEmpty { "—" })
                HospInfoItemRow(Icons.Filled.MedicalServices, "Speciality",
                    user.medicalCondition.ifEmpty { "General / Emergency" })
                HospInfoItemRow(Icons.Filled.Phone, "Contact", user.contactNumber.ifEmpty { "—" })
                HospInfoItemRow(Icons.Filled.AddCircle, "Available Beds",
                    user.age.ifEmpty { "—" })
                HospInfoItemRow(Icons.Filled.LocationOn, "Address",
                    user.emergencyContact.ifEmpty { "—" })
            }
        }

        Spacer(Modifier.height(20.dp))

        // Status card
        Surface(
            shape = RoundedCornerShape(20.dp), color = JeevanGreenSoft, shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(JeevanGreen))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Status: Operational", color = JeevanGreen, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("Receiving SOS emergencies", color = JeevanGreen.copy(0.7f), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HospInfoItemRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = JeevanGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = JeevanSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
