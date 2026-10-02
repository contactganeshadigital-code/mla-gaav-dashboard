package com.ganeshadigital.mlagaav

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val SvBlue = Color(0xFF0B4DA2)
private val SvPurple = Color(0xFF6A1B9A)
private val SvTeal = Color(0xFF00897B)

data class GovLink(val en: String, val mr: String, val url: String)

val GOV_LINKS = listOf(
    GovLink("Aaple Sarkar", "आपले सरकार (दाखले / सेवा)", "https://aaplesarkar.mahaonline.gov.in"),
    GovLink("Mahabhulekh", "महाभूलेख (7/12, 8अ)", "https://bhulekh.mahabhumi.gov.in"),
    GovLink("PM-Kisan", "पी.एम. किसान सन्मान निधी", "https://pmkisan.gov.in"),
    GovLink("e-Shram", "ई-श्रम कार्ड", "https://eshram.gov.in"),
    GovLink("Ayushman Bharat (PM-JAY)", "आयुष्मान भारत", "https://pmjay.gov.in"),
    GovLink("MahaDBT", "महाडीबीटी (शिष्यवृत्ती / योजना)", "https://mahadbt.maharashtra.gov.in"),
    GovLink("MGNREGA", "मनरेगा (रोजगार हमी)", "https://nrega.nic.in"),
    GovLink("PMAY-G", "प्रधानमंत्री आवास योजना (ग्रामीण)", "https://pmayg.nic.in"),
    GovLink("e-GramSwaraj", "ई-ग्रामस्वराज (ग्रामपंचायत)", "https://egramswaraj.gov.in"),
    GovLink("Mahafood", "शिधापत्रिका / रेशन", "https://mahafood.gov.in"),
    GovLink("DigiLocker", "डिजिलॉकर", "https://www.digilocker.gov.in"),
    GovLink("UMANG", "उमंग (सर्व शासकीय सेवा)", "https://web.umang.gov.in"),
    GovLink("Maharashtra Government", "महाराष्ट्र शासन", "https://maharashtra.gov.in")
)

fun openUrl(ctx: Context, url: String) {
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
}

@Composable
private fun ActionBtn(en: String, mr: String, icon: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("$icon $en", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(mr, fontSize = 12.sp)
        }
    }
}

@Composable
fun VillageActions(s: AppState, v: Village) {
    var dlg by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBtn("Government Services", "शासकीय सेवा", "🏛", SvBlue, Modifier.weight(1f)) { dlg = 1 }
            ActionBtn("Village Development", "गाव विकास", "📈", Green, Modifier.weight(1f)) { dlg = 2 }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBtn("Village Map", "गावाचा नकाशा", "📍", SvPurple, Modifier.weight(1f)) { dlg = 3 }
            ActionBtn("Important Contacts", "महत्त्वाचे संपर्क", "👥", SvTeal, Modifier.weight(1f)) { dlg = 4 }
        }
    }
    when (dlg) {
        1 -> GovDialog { dlg = 0 }
        2 -> DevDialog(s, v) { dlg = 0 }
        3 -> MapDialog(s, v) { dlg = 0 }
        4 -> ContactsDialog(s, v) { dlg = 0 }
    }
}

@Composable
fun GovDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("शासकीय सेवा") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("दाबल्यावर अधिकृत वेबसाइट फोनच्या ब्राउझरमध्ये उघडेल (इंटरनेट लागेल).", fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                GOV_LINKS.forEach { g ->
                    Column(Modifier.fillMaxWidth().clickable { openUrl(ctx, g.url) }.padding(vertical = 8.dp)) {
                        Text(g.mr, fontWeight = FontWeight.Medium)
                        Text(g.en, fontSize = 12.sp, color = SvBlue)
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )
}

@Composable
fun DevDialog(s: AppState, v: Village, onDismiss: () -> Unit) {
    val ws = s.works.filter { it.villageId == v.id }
    val done = ws.count { it.status == 2 }
    val p = if (ws.isEmpty()) 0f else done.toFloat() / ws.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("गाव विकास – ${v.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ws.isEmpty()) {
                    Text("या गावासाठी अजून कामे नोंदवलेली नाहीत. गावाच्या पानावर 'कामे' मध्ये '+ काम जोडा' दाबा.")
                } else {
                    Text("एकूण कामे: ${ws.size}   पूर्ण: $done   सुरू: ${ws.count { it.status == 1 }}   प्रस्तावित: ${ws.count { it.status == 0 }}", fontSize = 13.sp)
                    Text("एकूण निधी: ₹ ${"%.2f".format(ws.sumOf { it.budgetLakh })} लाख", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth(), color = Green)
                    Text("पूर्ण झालेली कामे: ${(p * 100).toInt()}%", fontSize = 12.sp)
                    HorizontalDivider()
                    ws.forEach { w ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(w.title, fontWeight = FontWeight.Medium)
                            Text("${WORK_STATUS[w.status]}  •  ₹ ${"%.2f".format(w.budgetLakh)} लाख  •  ${w.date}",
                                fontSize = 12.sp, color = statusColor(w.status))
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )
}

@Composable
fun MapDialog(s: AppState, v: Village, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val i = s.infos[v.id] ?: Info()
    val hasCoord = i.lat != 0.0 && i.lon != 0.0
    val query = if (hasCoord) "${i.lat},${i.lon}" else "${v.name}, ${v.taluka}, ${v.district}, ${v.state}"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("गावाचा नकाशा") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(v.name, fontWeight = FontWeight.Bold)
                Text("ता. ${v.taluka}, जि. ${v.district}, ${v.state} – ${v.pincode}", fontSize = 13.sp)
                if (hasCoord) Text("अक्षांश/रेखांश: ${i.lat}, ${i.lon}", fontSize = 12.sp)
                else Text("अचूक ठिकाणासाठी 'गावाची विस्तृत माहिती' मध्ये अक्षांश व रेखांश भरा. सध्या गावाच्या नावाने शोध होईल.", fontSize = 12.sp)
                Button(
                    onClick = { openUrl(ctx, "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗺 Google Maps मध्ये उघडा") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )
}

@Composable
fun ContactsDialog(s: AppState, v: Village, onDismiss: () -> Unit) {
    val gc = s.gpOf(v)
    val mine = s.contacts.filter { it.villageId == v.id }
    var label by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("महत्त्वाचे संपर्क") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ग्रामपंचायत: ${v.gp}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                ContactRow("सरपंच", gc?.sarpanch ?: "", gc?.sarpanchPhone ?: "")
                ContactRow("ग्रामसेवक", gc?.gramsevak ?: "", gc?.gramsevakPhone ?: "")
                if (gc == null) Text("सरपंच/ग्रामसेवक संपर्क गावाच्या पानावर 'ग्रामपंचायत संपर्क भरा' मधून भरा.", fontSize = 11.sp, color = Red)
                mine.forEach { c ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { ContactRow(c.label, c.name, c.phone) }
                        TextButton(onClick = { s.deleteContact(c.id) }) { Text("🗑", color = Red) }
                    }
                }
                HorizontalDivider()
                ContactRow("पोलीस / Police", "", "100")
                ContactRow("रुग्णवाहिका / Ambulance", "", "108")
                ContactRow("आपत्कालीन / Emergency", "", "112")
                ContactRow("अग्निशमन / Fire", "", "101")
                ContactRow("महिला हेल्पलाइन", "", "181")
                ContactRow("चाइल्डलाइन", "", "1098")
                HorizontalDivider()
                Text("नवीन संपर्क जोडा (आरोग्य केंद्र, तलाठी, शाळा इ.)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Field("पद / विभाग *", label, { label = it; err = "" })
                Field("नाव", name, { name = it })
                Field("मोबाईल *", phone, { if (it.length <= 10 && it.all(Char::isDigit)) { phone = it; err = "" } }, number = true)
                if (err.isNotEmpty()) Text(err, color = Red)
                Button(
                    onClick = {
                        if (label.isBlank() || phone.length < 3) err = "पद आणि मोबाईल टाका"
                        else {
                            s.addContact(VContact(newId(), v.id, label.trim(), name.trim(), phone))
                            label = ""; name = ""; phone = ""; err = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ संपर्क जोडा") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )
}
