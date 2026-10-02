package com.ganeshadigital.mlagaav

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Brand = Color(0xFF0B4DA2)
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
fun Field(label: String, value: String, onChange: (String) -> Unit, number: Boolean = false, lines: Int = 1, decimal: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = lines == 1,
        minLines = lines,
        keyboardOptions = if (decimal) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal) else if (number)
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

const val OTHER_VILLAGE = "✏️ इतर (स्वतः टाका)"

@Composable
fun SearchPicker(label: String, value: String, options: List<String>, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp)
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (value.isBlank()) "निवडा" else value, modifier = Modifier.weight(1f), maxLines = 1)
            Text("▾")
        }
    }
    if (open) {
        var q by remember { mutableStateOf("") }
        val list = remember(q, options) { if (q.isBlank()) options else options.filter { it.contains(q, ignoreCase = true) } }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column {
                    OutlinedTextField(
                        value = q, onValueChange = { q = it }, label = { Text("शोधा") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(list) { o ->
                            Text(
                                o,
                                Modifier.fillMaxWidth().clickable { onSelect(o); open = false }.padding(vertical = 12.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("बंद") } }
        )
    }
}

@Composable
fun VillageDialog(s: AppState, initial: Village?, prefill: Village?, onDismiss: () -> Unit, onSave: (Village) -> Unit) {
    val ctx = LocalContext.current
    // For a new village, State/District/Taluka/GP are pre-filled from the last village added
    val base = initial ?: prefill
    val states = remember { LocationData.states(ctx) }
    var state by remember { mutableStateOf(base?.state ?: "") }
    var district by remember { mutableStateOf(base?.district ?: "") }
    var taluka by remember { mutableStateOf(base?.taluka ?: "") }
    var gp by remember { mutableStateOf(base?.gp ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var manual by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf(initial?.pincode ?: base?.pincode ?: "") }
    var pop by remember { mutableStateOf(initial?.population?.toString() ?: "") }
    var hh by remember { mutableStateOf(initial?.households?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var err by remember { mutableStateOf("") }

    val tree = remember(state) { if (state.isBlank()) null else LocationData.load(ctx, state) }
    val districts = tree?.keys?.toList() ?: emptyList()
    val talukas = tree?.get(district)?.keys?.toList() ?: emptyList()
    val gps = tree?.get(district)?.get(taluka)?.keys?.toList() ?: emptyList()
    val villages = tree?.get(district)?.get(taluka)?.get(gp)?.sorted() ?: emptyList()
    val isManual = manual || (name.isNotBlank() && name !in villages)

    FormDialog(
        title = if (initial == null) "नवीन गाव" else "गाव संपादित करा",
        onDismiss = onDismiss,
        onSave = {
            when {
                state.isBlank() -> err = "राज्य निवडा"
                district.isBlank() -> err = "जिल्हा निवडा"
                taluka.isBlank() -> err = "तालुका निवडा"
                gp.isBlank() -> err = "ग्रामपंचायत निवडा"
                name.isBlank() -> err = "गाव निवडा / नाव टाका"
                pin.length != 6 -> err = "पिन कोड 6 अंकी हवा"
                else -> onSave(
                    Village(initial?.id ?: newId(), state.trim(), district.trim(), taluka.trim(),
                        gp.trim(), name.trim(), pin,
                        pop.toIntOrNull() ?: 0, hh.toIntOrNull() ?: 0,
                        initial?.sarpanch ?: "", initial?.contact ?: "", notes.trim())
                )
            }
        }
    ) {
        SearchPicker("राज्य *", state, states) {
            if (it != state) { state = it; district = ""; taluka = ""; gp = ""; name = ""; manual = false }
            err = ""
        }
        SearchPicker("जिल्हा *", district, districts, enabled = state.isNotBlank()) {
            if (it != district) { district = it; taluka = ""; gp = ""; name = ""; manual = false }
            err = ""
        }
        SearchPicker("तालुका *", taluka, talukas, enabled = district.isNotBlank()) {
            if (it != taluka) { taluka = it; gp = ""; name = ""; manual = false }
            err = ""
        }
        SearchPicker("ग्रामपंचायत *", gp, gps, enabled = taluka.isNotBlank()) {
            if (it != gp) { gp = it; name = ""; manual = false }
            err = ""
        }
        if (gp.isNotBlank()) {
            val gc = s.gps[gpKey(state, district, taluka, gp)]
            if (gc != null) Text("सरपंच: ${gc.sarpanch} ${gc.sarpanchPhone}\nग्रामसेवक: ${gc.gramsevak} ${gc.gramsevakPhone}", fontSize = 12.sp, color = Green)
            else Text("या ग्रामपंचायतीचे सरपंच/ग्रामसेवक संपर्क अजून नाहीत. गाव सेव्ह केल्यावर एकदा टाका.", fontSize = 12.sp)
        }
        SearchPicker(
            "गाव *", if (manual && name.isBlank()) OTHER_VILLAGE else name,
            villages + OTHER_VILLAGE, enabled = gp.isNotBlank()
        ) {
            if (it == OTHER_VILLAGE) { manual = true; name = "" } else { manual = false; name = it }
            err = ""
        }
        if (isManual) Field("गावाचे नाव टाका *", name, { name = it; err = "" })
        Field("पिन कोड *", pin, { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; err = "" } }, number = true)
        Field("लोकसंख्या", pop, { pop = it }, number = true)
        Field("कुटुंबे", hh, { hh = it }, number = true)
        Field("टिपणी", notes, { notes = it }, lines = 3)
        if (err.isNotEmpty()) Text(err, color = Red)
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
        Field("निधी (₹ लाखात)", budget, { budget = it }, decimal = true)
        Picker("स्थिती", WORK_STATUS, st) { st = it }
    }
}
