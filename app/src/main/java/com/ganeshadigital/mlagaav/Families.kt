package com.ganeshadigital.mlagaav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GENDERS = listOf("पुरुष", "महिला", "इतर")
val RELATIONS = listOf(
    "कुटुंब प्रमुख", "पती / पत्नी", "मुलगा", "मुलगी", "वडील", "आई",
    "भाऊ", "बहीण", "नातू / नात", "सून / जावई", "इतर"
)

@Composable
fun FamilyCard(s: AppState, f: Family, onMembers: () -> Unit, onEdit: () -> Unit) {
    var del by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { onMembers() }) {
        Column(Modifier.padding(14.dp)) {
            Text(f.head, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (f.status != 1) Text("स्थिती: ${REG_STATUS[f.status.coerceIn(0, 2)]}", fontSize = 12.sp, color = statusColor(if (f.status == 0) 1 else 0))
            Text(
                "सदस्य: ${f.members.size}  •  पुरुष ${f.members.count { it.gender == 0 }}  •  महिला ${f.members.count { it.gender == 1 }}",
                fontSize = 12.sp
            )
            if (f.mobile.isNotBlank()) Text("📞 ${f.mobile}", fontSize = 12.sp)
            if (f.address.isNotBlank()) Text("📍 ${f.address}", fontSize = 12.sp)
            if (f.notes.isNotBlank()) Text("टिपणी: ${f.notes}", fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onMembers) { Text("👥 सदस्य") }
                TextButton(onClick = onEdit) { Text("✏️ संपादित") }
                TextButton(onClick = { del = true }) { Text("🗑", color = Red) }
            }
        }
    }
    if (del) ConfirmDialog("${f.head} यांचे कुटुंब आणि सर्व सदस्य हटवायचे?", { s.deleteFamily(f.id); del = false }, { del = false })
}

@Composable
fun FamilyDialog(s: AppState, villageId: Long, initial: Family?, onDismiss: () -> Unit) {
    var head by remember { mutableStateOf(initial?.head ?: "") }
    var mobile by remember { mutableStateOf(initial?.mobile ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var gender by remember { mutableIntStateOf(0) }
    var age by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    FormDialog(if (initial == null) "नवीन कुटुंब" else "कुटुंब संपादित करा", onDismiss, onSave = {
        when {
            head.isBlank() -> err = "कुटुंब प्रमुखाचे नाव टाका"
            mobile.isNotEmpty() && mobile.length != 10 -> err = "मोबाईल 10 अंकी हवा"
            else -> {
                val members = initial?.members ?: listOf(
                    Member(newId(), head.trim(), RELATIONS[0], gender, age.toIntOrNull() ?: 0, "")
                )
                s.upsertFamily(
                    initial?.copy(head = head.trim(), mobile = mobile, address = address.trim(), notes = notes.trim())
                        ?: Family(newId(), villageId, head.trim(), mobile, address.trim(), notes.trim(), members)
                )
                onDismiss()
            }
        }
    }) {
        Field("कुटुंब प्रमुखाचे नाव *", head, { head = it; err = "" })
        if (initial == null) {
            Picker("लिंग", GENDERS, gender) { gender = it }
            Field("वय", age, { if (it.length <= 3 && it.all(Char::isDigit)) age = it }, number = true)
        }
        Field("मोबाईल नंबर", mobile, { if (it.length <= 10 && it.all(Char::isDigit)) { mobile = it; err = "" } }, number = true)
        Field("पत्ता / वाडी / वस्ती", address, { address = it })
        Field("टिपणी", notes, { notes = it }, lines = 3)
        if (initial == null) Text("कुटुंब प्रमुख पहिला सदस्य म्हणून आपोआप जोडला जाईल. बाकी सदस्य नंतर 'सदस्य' मधून जोडा.", fontSize = 11.sp)
        if (err.isNotEmpty()) Text(err, color = Red)
    }
}

@Composable
fun FamilyMembersDialog(s: AppState, familyId: Long, v: Village, onDismiss: () -> Unit) {
    val f = s.families.find { it.id == familyId }
    if (f == null) { onDismiss(); return }
    val gc = s.gpOf(v)
    var name by remember { mutableStateOf("") }
    var rel by remember { mutableIntStateOf(1) }
    var gender by remember { mutableIntStateOf(0) }
    var age by remember { mutableStateOf("") }
    var occ by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var delMember by remember { mutableStateOf<Member?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${f.head} – कुटुंब") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("एकूण सदस्य: ${f.members.size}  (पुरुष ${f.members.count { it.gender == 0 }}, महिला ${f.members.count { it.gender == 1 }})",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (f.mobile.isNotBlank()) ContactRow("कुटुंब मोबाईल", "", f.mobile)
                if (f.notes.isNotBlank()) Text("टिपणी: ${f.notes}", fontSize = 12.sp)
                if (gc != null) {
                    ContactRow("सरपंच", gc.sarpanch, gc.sarpanchPhone)
                    ContactRow("ग्रामसेवक", gc.gramsevak, gc.gramsevakPhone)
                } else Text("ग्रामपंचायत संपर्क गावाच्या पानावर भरा.", fontSize = 11.sp)
                HorizontalDivider()
                f.members.forEach { m ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${m.relation} • ${GENDERS.getOrElse(m.gender) { "" }}" +
                                    (if (m.age > 0) " • ${m.age} वर्षे" else "") +
                                    (if (m.occupation.isNotBlank()) " • ${m.occupation}" else ""),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = { delMember = m }) { Text("🗑", color = Red) }
                    }
                    HorizontalDivider()
                }
                Text("नवीन सदस्य जोडा", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Field("नाव *", name, { name = it; err = "" })
                Picker("नाते", RELATIONS, rel) { rel = it }
                Picker("लिंग", GENDERS, gender) { gender = it }
                Field("वय", age, { if (it.length <= 3 && it.all(Char::isDigit)) age = it }, number = true)
                Field("व्यवसाय / शिक्षण", occ, { occ = it })
                if (err.isNotEmpty()) Text(err, color = Red)
                Button(
                    onClick = {
                        if (name.isBlank()) err = "सदस्याचे नाव टाका"
                        else {
                            s.addMember(f.id, Member(newId(), name.trim(), RELATIONS[rel], gender, age.toIntOrNull() ?: 0, occ.trim()))
                            name = ""; age = ""; occ = ""; err = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ सदस्य जोडा") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )
    delMember?.let { m ->
        ConfirmDialog("${m.name} यांना कुटुंबातून हटवायचे?", { s.removeMember(f.id, m.id); delMember = null }, { delMember = null })
    }
}
