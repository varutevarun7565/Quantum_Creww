package com.example.jeevan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.jeevan.ui.theme.*

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isTyping: Boolean = false,
    val timestamp: String = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
    val recommendation: RecommendationData? = null
)

data class RecommendationData(
    val type: String, // "HOSPITAL" or "AMBULANCE"
    val title: String,
    val distance: String,
    val extraInfo: String,
    val rating: String = "",
    val phone: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack: () -> Unit) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Initial Greeting
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                text = "Hello! I am JEEVAN AI, your emergency medical assistant. Please tell me about your symptoms, or select a quick emergency action below so I can find the right help for you immediately.",
                isUser = false
            )
        )
    }

    val chips = listOf(
        "Chest Pain", "Accident", "Breathing Issue", "Pregnancy Emergency", "Heart Attack", "Fever", "Bleeding", "Stroke Symptoms"
    )

    fun handleSend(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage(text = text, isUser = true)
        messages = messages + userMsg
        inputText = ""
        
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            
            // Add typing indicator
            val typingMsgId = java.util.UUID.randomUUID().toString()
            messages = messages + ChatMessage(id = typingMsgId, text = "", isUser = false, isTyping = true)
            listState.animateScrollToItem(messages.size - 1)
            
            delay(1500) // Simulate network delay
            
            // Remove typing indicator
            messages = messages.filter { it.id != typingMsgId }
            
            // Generate response
            val responseText = when {
                text.contains("Chest Pain", ignoreCase = true) || text.contains("Heart Attack", ignoreCase = true) -> 
                    "This could be a cardiac emergency. I strongly advise chewing aspirin if available and not allergic, and resting immediately. I have found the nearest Cardiac Centers and dispatched an ALS Ambulance."
                text.contains("Accident", ignoreCase = true) || text.contains("Bleeding", ignoreCase = true) -> 
                    "Please apply direct, firm pressure to any bleeding wounds with a clean cloth. Do not move anyone who might have head or spinal injuries. I am locating the nearest Trauma Center."
                text.contains("Breathing", ignoreCase = true) -> 
                    "Sit upright and try to stay calm. Loosen any tight clothing. I am locating the nearest hospital with emergency respiratory support."
                else -> 
                    "I understand. Please try to remain calm. Let me provide you with the closest medical assistance available based on your current location."
            }
            
            val recommendation = when {
                text.contains("Accident", ignoreCase = true) -> RecommendationData(
                    type = "AMBULANCE", title = "ALS Trauma Ambulance - MH 12 AB 1001", distance = "1.2 km away", extraInfo = "ETA: 4 mins", rating = "Driver: 4.8★", phone = "108"
                )
                else -> RecommendationData(
                    type = "HOSPITAL", title = "City General Specialty Hospital", distance = "2.5 km away", extraInfo = "Specialty: Emergency & Cardiac", rating = "4.5★", phone = "011-23456789"
                )
            }

            val botMsg = ChatMessage(text = responseText, isUser = false, recommendation = recommendation)
            messages = messages + botMsg
            
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("JEEVAN AI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = JeevanOnBackground)
                            Text("Online | Medical Assistant", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = JeevanOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JeevanBackground),
                actions = {
                    IconButton(onClick = { /* Escalate to SOS */ }) {
                        Icon(Icons.Filled.Warning, contentDescription = "Escalate SOS", tint = JeevanRed)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = JeevanSurface, shadowElevation = 8.dp) {
                Column {
                    // Quick Action Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chips) { chip ->
                            AssistChip(
                                onClick = { handleSend(chip) },
                                label = { Text(chip) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = JeevanRedSoft, labelColor = JeevanRedDark),
                                border = null,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                    
                    // Input Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Describe emergency...") },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF0F0F0),
                                unfocusedContainerColor = Color(0xFFF0F0F0),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (inputText.isBlank()) {
                            IconButton(onClick = { /* Voice input */ }, modifier = Modifier.background(JeevanRed, CircleShape)) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice Input", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { handleSend(inputText) }, modifier = Modifier.background(JeevanRed, CircleShape)) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.isUser
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF9E6))
                ) {
                    Text("🤖", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            if (msg.isTyping) {
                TypingIndicator()
            } else {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    ),
                    color = if (isUser) JeevanRed else Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = msg.text,
                            color = if (isUser) Color.White else JeevanOnBackground,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        
                        // Recommendations Card
                        msg.recommendation?.let { rec ->
                            Spacer(Modifier.height(12.dp))
                            RecommendationCard(rec)
                        }
                    }
                }
            }
        }
        if (!msg.isTyping) {
            Text(
                text = msg.timestamp,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, start = 40.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun RecommendationCard(rec: RecommendationData) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF3F4F6),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (rec.type == "HOSPITAL") Icons.Filled.LocalHospital else Icons.Filled.DirectionsCar
                val tint = if (rec.type == "HOSPITAL") Color(0xFFE53935) else Color(0xFF1E88E5)
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = JeevanOnBackground)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(rec.distance, fontSize = 12.sp, color = JeevanRed)
                if (rec.rating.isNotEmpty()) {
                    Text(rec.rating, fontSize = 12.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(rec.extraInfo, fontSize = 12.sp, color = JeevanSubtext)
            
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rec.type == "HOSPITAL") {
                    OutlinedButton(
                        onClick = { /* Call */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { /* Directions */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JeevanRed)
                    ) {
                        Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Route", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = { /* Track */ },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JeevanRed)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Track Ambulance", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(scale)
                    .background(Color.Gray, CircleShape)
            )
        }
    }
}
