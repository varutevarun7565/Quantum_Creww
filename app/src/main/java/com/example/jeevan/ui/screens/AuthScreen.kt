@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jeevan.ui.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeevan.JeevanViewModel
import com.example.jeevan.Screen
import com.example.jeevan.data.JeevanRepository
import com.example.jeevan.data.UserProfile
import com.example.jeevan.data.UserRole
import com.example.jeevan.ui.theme.*

// ─────────────────────────────────────────────────
// AUTH LANDING — Sign In / Sign Up choice
// ─────────────────────────────────────────────────
@Composable
fun AuthLandingScreen(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(JeevanBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo
            Text("✦", color = JeevanRed, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("JEEVAN", color = JeevanRed, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text("Emergency SOS System", color = JeevanSubtext, fontSize = 14.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(48.dp))

            // Sign In button
            Button(
                onClick   = { onNavigate(Screen.Login.route) },
                modifier  = Modifier.fillMaxWidth().height(54.dp),
                shape     = RoundedCornerShape(14.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = JeevanRed),
            ) {
                Icon(Icons.Filled.Login, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign In", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(14.dp))

            // Sign Up button
            OutlinedButton(
                onClick  = { onNavigate(Screen.Signup.route) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(2.dp, JeevanRed),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = JeevanRed),
            ) {
                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign Up", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text("Sign up is available for User role only",
                color = JeevanSubtext, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────
// LOGIN SCREEN
// ─────────────────────────────────────────────────
@Composable
fun LoginScreen(viewModel: JeevanViewModel, onSuccess: (UserRole) -> Unit, onBack: () -> Unit) {

    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var userId   by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var error    by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(false) }

    val roles = listOf(
        Triple(UserRole.USER,     "👤 User",              "Regular emergency user"),
        Triple(UserRole.DRIVER,   "🚑 Ambulance Driver",  "Respond to SOS calls"),
        Triple(UserRole.HOSPITAL, "🏥 Hospital Authority","Manage incoming patients"),
    )

    Column(
        modifier = Modifier.fillMaxSize().background(JeevanBackground).verticalScroll(rememberScrollState())
    ) {
        // Back button
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = JeevanRed)
            }
            Spacer(Modifier.width(8.dp))
            Text("Sign In", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = JeevanOnBackground)
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = JeevanSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {

                // ── Role selector ──
                Text("Select Role", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = roles.first { it.first == selectedRole }.second,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = JeevanRed,
                            unfocusedBorderColor = JeevanBorder,
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        roles.forEach { (role, label, desc) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(label, fontWeight = FontWeight.SemiBold)
                                        Text(desc, color = JeevanSubtext, fontSize = 12.sp)
                                    }
                                },
                                onClick = { selectedRole = role; dropdownExpanded = false },
                            )
                        }
                    }
                }

                // ── User ID ──
                AuthField("User ID", userId, Icons.Filled.Person) { userId = it; error = "" }

                // ── Password ──
                AuthField(
                    label    = "Password",
                    value    = password,
                    icon     = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingContent = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null, tint = JeevanSubtext, modifier = Modifier.size(20.dp))
                        }
                    }
                ) { password = it; error = "" }

                // ── Error ──
                AnimatedVisibility(visible = error.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = JeevanRedSoft) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = JeevanRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = JeevanRed, fontSize = 13.sp)
                        }
                    }
                }

                // ── Login Button ──
                Button(
                    onClick = {
                        when {
                            userId.isBlank()   -> error = "Please enter your User ID"
                            password.isBlank() -> error = "Please enter your password"
                            else -> {
                                loading = true
                                viewModel.login(userId.trim(), password) { ok ->
                                    loading = false
                                    if (ok) onSuccess(selectedRole)
                                    else error = "Invalid User ID or password"
                                }
                            }
                        }
                    },
                    enabled  = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = JeevanRed),
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Login, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Login", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }

                // Quick credentials hint
                Surface(shape = RoundedCornerShape(10.dp), color = JeevanSurface2) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Demo credentials:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = JeevanSubtext)
                        Spacer(Modifier.height(4.dp))
                        Text("Driver → driver1 / driver123", fontSize = 11.sp, color = JeevanSubtext)
                        Text("Hospital → hospital1 / hosp123", fontSize = 11.sp, color = JeevanSubtext)
                        Text("Register a new User account via Sign Up", fontSize = 11.sp, color = JeevanSubtext)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────
// SIGNUP SCREEN (User role only)
// ─────────────────────────────────────────────────
@Composable
fun SignupScreen(viewModel: JeevanViewModel, onSuccess: () -> Unit, onBack: () -> Unit) {

    var userId      by remember { mutableStateOf("") }
    var contact     by remember { mutableStateOf("") }
    var emergency   by remember { mutableStateOf("") }
    var bloodGroup  by remember { mutableStateOf("") }
    var age         by remember { mutableStateOf("") }
    var condition   by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf("") }
    var loading     by remember { mutableStateOf(false) }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    var bgExpanded  by remember { mutableStateOf(false) }

    fun validate(): String? {
        if (userId.isBlank())    return "User ID is required"
        if (userId.length < 4)   return "User ID must be at least 4 characters"
        if (contact.isBlank())   return "Contact number is required"
        if (contact.length < 10) return "Enter a valid 10-digit contact number"
        if (emergency.isBlank()) return "Emergency contact is required"
        if (bloodGroup.isBlank()) return "Please select a blood group"
        if (age.isBlank())       return "Age is required"
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isUpperCase() }) return "Password must have at least 1 uppercase letter"
        if (!password.any { it.isDigit() })     return "Password must have at least 1 number"
        if (password != confirmPass)            return "Passwords do not match"
        return null
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
            Column {
                Text("Create Account", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("User role registration", color = JeevanSubtext, fontSize = 12.sp)
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp), color = JeevanSurface, shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                AuthField("User ID", userId, Icons.Filled.Badge) { userId = it; error = "" }
                AuthField("Contact Number", contact, Icons.Filled.Phone, keyboardType = KeyboardType.Phone) { contact = it; error = "" }
                AuthField("Emergency Contact", emergency, Icons.Filled.ContactPhone, keyboardType = KeyboardType.Phone) { emergency = it; error = "" }

                // Blood Group dropdown
                Text("Blood Group", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                ExposedDropdownMenuBox(expanded = bgExpanded, onExpandedChange = { bgExpanded = it }) {
                    OutlinedTextField(
                        value = bloodGroup.ifEmpty { "Select" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bgExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JeevanRed, unfocusedBorderColor = JeevanBorder),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = bgExpanded, onDismissRequest = { bgExpanded = false }) {
                        bloodGroups.forEach { bg ->
                            DropdownMenuItem(text = { Text(bg, fontWeight = FontWeight.SemiBold) },
                                onClick = { bloodGroup = bg; bgExpanded = false })
                        }
                    }
                }

                AuthField("Age", age, Icons.Filled.Cake, keyboardType = KeyboardType.Number) { age = it; error = "" }
                AuthField("Medical Condition (optional)", condition, Icons.Filled.MedicalServices) { condition = it }

                HorizontalDivider(color = JeevanBorder)
                Text("Security", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = JeevanOnBackground)

                AuthField(
                    "Password",
                    password,
                    Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingContent = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null, tint = JeevanSubtext, modifier = Modifier.size(20.dp))
                        }
                    }
                ) { password = it; error = "" }

                AuthField(
                    "Confirm Password",
                    confirmPass,
                    Icons.Filled.LockClock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                ) { confirmPass = it; error = "" }

                // Password rules
                PasswordRuleRow("At least 8 characters",    password.length >= 8)
                PasswordRuleRow("At least 1 uppercase",      password.any { it.isUpperCase() })
                PasswordRuleRow("At least 1 number",         password.any { it.isDigit() })
                PasswordRuleRow("Passwords match",           password == confirmPass && confirmPass.isNotEmpty())

                AnimatedVisibility(visible = error.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = JeevanRedSoft) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = JeevanRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = JeevanRed, fontSize = 13.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        val err = validate()
                        if (err != null) { error = err; return@Button }
                        loading = true
                        val profile = UserProfile(
                            userId = userId.trim(), name = userId.trim(),
                            role = UserRole.USER, contactNumber = contact,
                            emergencyContact = emergency, bloodGroup = bloodGroup,
                            age = age, medicalCondition = condition, password = password,
                        )
                        viewModel.register(profile) { result ->
                            loading = false
                            when (result) {
                                JeevanRepository.RegisterResult.SUCCESS ->
                                    onSuccess()
                                JeevanRepository.RegisterResult.DUPLICATE ->
                                    error = "This User ID is already taken. Please choose a different one."
                                JeevanRepository.RegisterResult.NETWORK_ERROR ->
                                    error = "Server error. Please try again or check your connection."
                            }
                        }
                    },
                    enabled  = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JeevanRed),
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.HowToReg, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Register", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────
@Composable
fun AuthField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = JeevanOnBackground)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            leadingIcon = { Icon(icon, null, tint = JeevanSubtext, modifier = Modifier.size(18.dp)) },
            trailingIcon = trailingContent,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = JeevanRed,
                unfocusedBorderColor = JeevanBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PasswordRuleRow(label: String, passed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (passed) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            null,
            tint = if (passed) JeevanGreen else JeevanBorder,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = if (passed) JeevanGreen else JeevanSubtext)
    }
}
