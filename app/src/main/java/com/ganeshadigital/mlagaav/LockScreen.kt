package com.ganeshadigital.mlagaav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
    val setup = remember { !s.hasPin() }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var wait by remember { mutableIntStateOf(s.waitSeconds()) }

    LaunchedEffect(wait) {
        if (wait > 0) { delay(1000); wait = s.waitSeconds() }
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔒", fontSize = 48.sp)
        Text("आमचे गाव डॅशबोर्ड", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Saffron)
        Spacer(Modifier.height(6.dp))
        Text(if (setup) "सुरक्षेसाठी 4 ते 6 अंकी PIN सेट करा" else "PIN टाका", fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        PinField(if (setup) "नवीन PIN" else "PIN", pin) { pin = it; msg = "" }
        if (setup) {
            Spacer(Modifier.height(8.dp))
            PinField("PIN पुन्हा टाका", pin2) { pin2 = it; msg = "" }
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
                        else -> { s.setPin(pin); onUnlock() }
                    }
                } else {
                    if (s.checkPin(pin)) onUnlock()
                    else { pin = ""; wait = s.waitSeconds(); msg = "चुकीचा PIN" }
                }
            }
        ) { Text(if (setup) "PIN सेव्ह करा" else "उघडा") }
        if (setup) Text(
            "हा PIN विसरू नका. विसरल्यास डेटा उघडता येणार नाही (अॅप अनइन्स्टॉल केल्यास डेटा जातो – आधी बॅकअप घ्या).",
            fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun ChangePinDialog(s: AppState, onDismiss: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    FormDialog("PIN बदला", onDismiss, onSave = {
        when {
            s.waitSeconds() > 0 -> msg = "थोडा वेळ थांबा"
            !s.checkPin(old) -> msg = "जुना PIN चुकीचा"
            n1.length < 4 -> msg = "नवीन PIN किमान 4 अंकी हवा"
            n1 != n2 -> msg = "नवीन PIN जुळत नाहीत"
            else -> { s.setPin(n1); onDismiss() }
        }
    }) {
        PinField("जुना PIN", old) { old = it }
        PinField("नवीन PIN", n1) { n1 = it }
        PinField("नवीन PIN पुन्हा", n2) { n2 = it }
        if (msg.isNotEmpty()) Text(msg, color = Red)
    }
}
