package com.ganeshadigital.mlagaav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val RECOVERY_QUESTIONS = listOf(
    "तुमचे जन्मगाव कोणते?",
    "तुमच्या आईचे नाव काय?",
    "तुमच्या पहिल्या शाळेचे नाव काय?",
    "तुमचा आवडता रंग कोणता?",
    "तुमच्या जवळच्या मित्राचे नाव काय?"
)

@Composable
fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun LockScreen(s: AppState, onUnlock: () -> Unit) {
    var setup by remember { mutableStateOf(!s.hasPin()) }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var qIdx by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var wait by remember { mutableIntStateOf(s.waitSeconds()) }
    var forgot by remember { mutableStateOf(false) }

    LaunchedEffect(wait) {
        if (wait > 0) { delay(1000); wait = s.waitSeconds() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.logo), contentDescription = "My Village Data", modifier = Modifier.size(150.dp))
        Text("My Village Data", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Brand)
        Spacer(Modifier.height(6.dp))
        Text(if (setup) "सुरक्षेसाठी 4 ते 6 अंकी PIN सेट करा" else "PIN टाका", fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        PinField(if (setup) "नवीन PIN" else "PIN", pin) { pin = it; msg = "" }
        if (setup) {
            Spacer(Modifier.height(8.dp))
            PinField("PIN पुन्हा टाका", pin2) { pin2 = it; msg = "" }
            Spacer(Modifier.height(12.dp))
            Text("PIN विसरल्यास उपयोगी: रिकव्हरी प्रश्न", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Picker("प्रश्न", RECOVERY_QUESTIONS, qIdx) { qIdx = it }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = answer, onValueChange = { answer = it; msg = "" },
                label = { Text("उत्तर") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        if (wait > 0) Text("खूप चुकीचे प्रयत्न. $wait सेकंद थांबा.", color = Red, modifier = Modifier.padding(top = 8.dp))
        else if (msg.isNotEmpty()) Text(msg, color = Red, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = wait == 0,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (setup) {
                    when {
                        pin.length < 4 -> msg = "PIN किमान 4 अंकी हवा"
                        pin != pin2 -> msg = "दोन्ही PIN जुळत नाहीत"
                        answer.isBlank() -> msg = "रिकव्हरी प्रश्नाचे उत्तर टाका"
                        else -> { s.setPin(pin); s.setRecovery(RECOVERY_QUESTIONS[qIdx], answer); onUnlock() }
                    }
                } else {
                    if (s.checkPin(pin)) onUnlock()
                    else { pin = ""; wait = s.waitSeconds(); msg = "चुकीचा PIN" }
                }
            }
        ) { Text(if (setup) "PIN सेव्ह करा" else "उघडा") }

        if (setup) {
            Text(
                "उत्तर लक्षात ठेवा. PIN विसरल्यास या उत्तराने नवीन PIN सेट करता येईल.",
                fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            TextButton(onClick = { forgot = true }) { Text("PIN विसरलात?") }
        }
    }

    if (forgot) {
        ForgotPinDialog(
            s,
            onDismiss = { forgot = false },
            onRecovered = { forgot = false; onUnlock() },
            onWiped = { forgot = false; setup = true; pin = ""; pin2 = ""; answer = ""; msg = "" }
        )
    }
}

@Composable
fun ForgotPinDialog(s: AppState, onDismiss: () -> Unit, onRecovered: () -> Unit, onWiped: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var wait by remember { mutableIntStateOf(s.recoveryWaitSeconds()) }
    var confirmWipe by remember { mutableStateOf(false) }

    LaunchedEffect(wait) {
        if (wait > 0) { delay(1000); wait = s.recoveryWaitSeconds() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN विसरलात?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.hasRecovery()) {
                    Text("रिकव्हरी प्रश्न:", fontSize = 12.sp)
                    Text(s.recoveryQuestion(), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = answer, onValueChange = { answer = it; msg = "" },
                        label = { Text("उत्तर") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    PinField("नवीन PIN", n1) { n1 = it; msg = "" }
                    PinField("नवीन PIN पुन्हा", n2) { n2 = it; msg = "" }
                    if (wait > 0) Text("खूप चुकीचे उत्तर. $wait सेकंद थांबा.", color = Red)
                    else if (msg.isNotEmpty()) Text(msg, color = Red)
                    Button(
                        enabled = wait == 0,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            when {
                                answer.isBlank() -> msg = "उत्तर टाका"
                                n1.length < 4 -> msg = "नवीन PIN किमान 4 अंकी हवा"
                                n1 != n2 -> msg = "दोन्ही PIN जुळत नाहीत"
                                s.checkRecovery(answer) -> { s.setPin(n1); onRecovered() }
                                else -> { wait = s.recoveryWaitSeconds(); msg = "चुकीचे उत्तर" }
                            }
                        }
                    ) { Text("नवीन PIN सेट करा") }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                } else {
                    Text("या अॅपमध्ये रिकव्हरी प्रश्न सेट केलेला नाही, त्यामुळे PIN रिकव्हर करता येत नाही.")
                }
                Text("उत्तर आठवत नसेल तर अॅप रीसेट करा. यामुळे सर्व गावे, समस्या आणि कामे कायमची पुसली जातील.", fontSize = 13.sp)
                OutlinedButton(
                    onClick = { confirmWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                ) { Text("🗑 सर्व डेटा पुसून रीसेट करा") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("सर्व डेटा पुसायचा?") },
            text = { Text("सर्व गावे, समस्या, कामे आणि PIN कायमचे डिलीट होतील. हे परत मिळणार नाही.") },
            confirmButton = {
                Button(onClick = { s.resetAll(); confirmWipe = false; onWiped() },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("हो, सर्व पुसा") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("नाही") } }
        )
    }
}

@Composable
fun ChangePinDialog(s: AppState, onDismiss: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var changeQ by remember { mutableStateOf(!s.hasRecovery()) }
    var qIdx by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    FormDialog("PIN / रिकव्हरी बदला", onDismiss, onSave = {
        when {
            s.waitSeconds() > 0 -> msg = "थोडा वेळ थांबा"
            !s.checkPin(old) -> msg = "जुना PIN चुकीचा"
            n1.isNotEmpty() && n1.length < 4 -> msg = "नवीन PIN किमान 4 अंकी हवा"
            n1 != n2 -> msg = "नवीन PIN जुळत नाहीत"
            changeQ && answer.isBlank() -> msg = "रिकव्हरी प्रश्नाचे उत्तर टाका"
            n1.isEmpty() && !changeQ -> msg = "काहीतरी बदला"
            else -> {
                if (n1.isNotEmpty()) s.setPin(n1)
                if (changeQ) s.setRecovery(RECOVERY_QUESTIONS[qIdx], answer)
                onDismiss()
            }
        }
    }) {
        PinField("सध्याचा PIN *", old) { old = it; msg = "" }
        Text("नवीन PIN (बदलायचा नसेल तर रिकामा ठेवा)", fontSize = 12.sp)
        PinField("नवीन PIN", n1) { n1 = it; msg = "" }
        PinField("नवीन PIN पुन्हा", n2) { n2 = it; msg = "" }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = changeQ, onCheckedChange = { changeQ = it; msg = "" })
            Text(if (s.hasRecovery()) "रिकव्हरी प्रश्न बदला" else "रिकव्हरी प्रश्न सेट करा")
        }
        if (changeQ) {
            Picker("प्रश्न", RECOVERY_QUESTIONS, qIdx) { qIdx = it }
            OutlinedTextField(
                value = answer, onValueChange = { answer = it; msg = "" },
                label = { Text("उत्तर") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        if (msg.isNotEmpty()) Text(msg, color = Red)
    }
}
