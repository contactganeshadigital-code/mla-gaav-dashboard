package com.ganeshadigital.mlagaav

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Saffron = Color(0xFFE65100)
val Green = Color(0xFF2E7D32)
val Amber = Color(0xFFF9A825)
val Red = Color(0xFFC62828)

fun statusColor(status: Int) = when (status) { 0 -> Red; 1 -> Amber; else -> Green }

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(i, "शेअर करा"))
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 13.sp)
        }
    }
}

@Composable
fun StatusChip(label: String, status: Int, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, color = statusColor(status)) }
    )
}

@Composable
fun Picker(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(options.getOrElse(selected) { "निवडा" })
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEachIndexed { i, o ->
                    DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(i); open = false })
                }
            }
        }
    }
}

@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("सेव्ह") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द") } }
    )
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, number: Boolean = false, lines: Int = 1) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = lines == 1,
        minLines = lines,
        keyboardOptions = if (number)
            androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        else androidx.compose.foundation.text.KeyboardOptions.Default
    )
}

@Composable
fun ConfirmDialog(text: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text("खात्री आहे का?") },
        text = { Text(text) },
        confirmButton = { Button(onClick = onYes, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("हो, हटवा") } },
        dismissButton = { TextButton(onClick = onNo) { Text("नाही") } }
    )
}

@Composable
fun VillageDialog(initial: Village?, onDismiss: () -> Unit, onSave: (Village) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var taluka by remember { mutableStateOf(initial?.taluka ?: "") }
    var pop by remember { mutableStateOf(initial?.population?.toString() ?: "") }
    var hh by remember { mutableStateOf(initial?.households?.toString() ?: "") }
    var sarpanch by remember { mutableStateOf(initial?.sarpanch ?: "") }
    var contact by remember { mutableStateOf(initial?.contact ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    FormDialog(
        title = if (initial == null) "नवीन गाव" else "गाव संपादित करा",
        onDismiss = onDismiss,
        onSave = {
            if (name.isNotBlank()) onSave(
                Village(initial?.id ?: newId(), name.trim(), taluka.trim(),
                    pop.toIntOrNull() ?: 0, hh.toIntOrNull() ?: 0,
                    sarpanch.trim(), contact.trim(), notes.trim())
            )
        }
    ) {
        Field("गावाचे नाव *", name, { name = it })
        Field("तालुका", taluka, { taluka = it })
        Field("लोकसंख्या", pop, { pop = it }, number = true)
        Field("कुटुंबे", hh, { hh = it }, number = true)
        Field("सरपंच / ग्रामसेवक", sarpanch, { sarpanch = it })
        Field("संपर्क क्रमांक", contact, { contact = it }, number = true)
        Field("टिपणी", notes, { notes = it }, lines = 3)
    }
}

@Composable
fun IssueDialog(s: AppState, presetVillage: Long?, onDismiss: () -> Unit) {
    var vi by remember { mutableIntStateOf(s.villages.indexOfFirst { it.id == presetVillage }.coerceAtLeast(0)) }
    var title by remember { mutableStateOf("") }
    var cat by remember { mutableIntStateOf(0) }
    var pr by remember { mutableIntStateOf(1) }
    FormDialog("नवीन समस्या", onDismiss, onSave = {
        if (title.isNotBlank() && s.villages.isNotEmpty()) {
            s.addIssue(Issue(newId(), s.villages[vi].id, title.trim(), CATEGORIES[cat], pr, 0, today()))
            onDismiss()
        }
    }) {
        if (s.villages.isEmpty()) Text("आधी गाव जोडा.", color = Red)
        else Picker("गाव", s.villages.map { it.name }, vi) { vi = it }
        Field("समस्या *", title, { title = it }, lines = 2)
        Picker("प्रकार", CATEGORIES, cat) { cat = it }
        Picker("प्राधान्य", PRIORITY, pr) { pr = it }
    }
}

@Composable
fun WorkDialog(s: AppState, presetVillage: Long?, onDismiss: () -> Unit) {
    var vi by remember { mutableIntStateOf(s.villages.indexOfFirst { it.id == presetVillage }.coerceAtLeast(0)) }
    var title by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var st by remember { mutableIntStateOf(0) }
    FormDialog("नवीन काम", onDismiss, onSave = {
        if (title.isNotBlank() && s.villages.isNotEmpty()) {
            s.addWork(Work(newId(), s.villages[vi].id, title.trim(), budget.toDoubleOrNull() ?: 0.0, st, today()))
            onDismiss()
        }
    }) {
        if (s.villages.isEmpty()) Text("आधी गाव जोडा.", color = Red)
        else Picker("गाव", s.villages.map { it.name }, vi) { vi = it }
        Field("कामाचे नाव *", title, { title = it }, lines = 2)
        Field("निधी (₹ लाखात)", budget, { budget = it }, number = true)
        Picker("स्थिती", WORK_STATUS, st) { st = it }
    }
}
