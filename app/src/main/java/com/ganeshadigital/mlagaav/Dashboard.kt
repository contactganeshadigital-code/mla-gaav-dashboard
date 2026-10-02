package com.ganeshadigital.mlagaav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Blue = Color(0xFF1E88E5)
private val Pink = Color(0xFFEC407A)
private val Purple = Color(0xFF7B1FA2)
private val Teal = Color(0xFF00897B)
private val Orange = Color(0xFFEF6C00)

private fun pct(d: Double) = if (d <= 0.0) "-" else "%.1f%%".format(d)
private fun num(i: Int) = if (i <= 0) "-" else i.toString()
private fun ha(d: Double) = if (d <= 0.0) "-" else "%.0f हे.".format(d)

@Composable
fun Tile(en: String, mr: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.padding(10.dp).fillMaxWidth()) {
            Text(en, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Text(mr, fontSize = 11.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionCard(title: String, mr: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Brand, fontSize = 15.sp)
            Text(mr, fontSize = 12.sp, color = Brand)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ContactRow(label: String, name: String, phone: String) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable(enabled = phone.isNotBlank()) {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            },
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp)
            if (name.isNotBlank()) Text(name, fontWeight = FontWeight.Medium)
        }
        Text(if (phone.isBlank()) "-" else "📞 $phone", color = Green, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VillageDashboard(s: AppState, v: Village, onEditInfo: () -> Unit, onEditGp: () -> Unit) {
    val i = s.infos[v.id] ?: Info()
    val gc = s.gpOf(v)
    // If families are registered, population numbers are computed from them automatically
    val fams = s.families.filter { it.villageId == v.id }
    val mem = fams.flatMap { it.members }
    val hasF = fams.isNotEmpty()
    val famCount = if (hasF) fams.size else v.households
    val male = if (hasF) mem.count { it.gender == 0 } else i.male
    val female = if (hasF) mem.count { it.gender == 1 } else if (i.female > 0) i.female else (v.population - i.male).coerceAtLeast(0)
    val total = if (hasF) mem.size else if (v.population > 0) v.population else male + female

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painterResource(R.drawable.banner), contentDescription = "My Village Data",
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        )
        // Header strip
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Brand)) {
            Column(Modifier.padding(12.dp)) {
                Text("गावाचे नाव / Village", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                Text(v.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("तालुका: ${v.taluka}   जिल्हा: ${v.district}", fontSize = 12.sp, color = Color.White)
                Text("शेवटची अपडेट: ${i.updated.ifBlank { "-" }}", fontSize = 12.sp, color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Total Population", "एकूण लोकसंख्या", num(total), Blue, Modifier.weight(1f))
            Tile("Total Families", "एकूण कुटुंबे", num(famCount), Green, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Male", "पुरुष", num(male), Blue, Modifier.weight(1f))
            Tile("Female", "महिला", num(female), Pink, Modifier.weight(1f))
            Tile("Literacy", "साक्षरता", pct(i.literacy), Orange, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Houses", "एकूण घरे", num(i.houses), Purple, Modifier.weight(1f))
            Tile("Water", "पाणी सुविधा", if (i.water > 0) "${i.water}%" else "-", Teal, Modifier.weight(1f))
            Tile("Electricity", "वीज सुविधा", if (i.elec > 0) "${i.elec}%" else "-", Orange, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Schools / Anganwadi", "शाळा / अंगणवाडी", "${i.schools} / ${i.anganwadi}", Green, Modifier.weight(1f))
            Tile("Health Facilities", "आरोग्य सुविधा", num(i.health), Red, Modifier.weight(1f))
        }

        // Gender donut
        SectionCard("Population by Gender", "लिंगानुसार लोकसंख्या") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 28f
                        val sz = Size(size.width - sw, size.height - sw)
                        val tl = Offset(sw / 2, sw / 2)
                        val t = (male + female).toFloat()
                        if (t <= 0f) {
                            drawArc(Color.LightGray, 0f, 360f, false, topLeft = tl, size = sz, style = Stroke(sw))
                        } else {
                            val m = 360f * male / t
                            drawArc(Blue, -90f, m, false, topLeft = tl, size = sz, style = Stroke(sw))
                            drawArc(Pink, -90f + m, 360f - m, false, topLeft = tl, size = sz, style = Stroke(sw))
                        }
                    }
                    Text(num(male + female), fontWeight = FontWeight.Bold)
                }
                Column {
                    val t = (male + female).coerceAtLeast(1)
                    Text("● पुरुष: $male (${"%.1f".format(100.0 * male / t)}%)", color = Blue)
                    Text("● महिला: $female (${"%.1f".format(100.0 * female / t)}%)", color = Pink)
                }
            }
        }

        // Education bars
        SectionCard("Education Level", "शिक्षण स्तर") {
            val bars = listOf(
                Triple("साक्षर", i.literacy, Green), Triple("प्राथमिक", i.eduPrimary, Blue),
                Triple("माध्यमिक", i.eduSecondary, Orange), Triple("उच्च", i.eduHigher, Purple)
            )
            Row(Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.Bottom) {
                bars.forEach { (label, value, c) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(pct(value), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Box(
                            Modifier.fillMaxWidth(0.55f).height((value.coerceIn(0.0, 100.0) * 1.0).dp.coerceAtLeast(2.dp))
                                .background(c, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
        }

        // Overview + agriculture
        SectionCard("Village Overview", "गावाचा आढावा") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("एकूण क्षेत्रफळ", fontSize = 11.sp); Text(ha(i.areaHa), fontWeight = FontWeight.Bold) }
                Column { Text("शेती जमीन", fontSize = 11.sp); Text(ha(i.agriHa), fontWeight = FontWeight.Bold) }
                Column { Text("वनक्षेत्र", fontSize = 11.sp); Text(ha(i.forestHa), fontWeight = FontWeight.Bold) }
                Column { Text("जलस्रोत", fontSize = 11.sp); Text(num(i.waterBodies), fontWeight = FontWeight.Bold) }
            }
        }
        SectionCard("Agriculture & Land", "शेती व जमीन") {
            Text("कसुती जमीन: ${ha(i.agriHa)}")
            Text("सिंचन: ${if (i.irrigation > 0) "${i.irrigation}%" else "-"}")
            Text("मुख्य पिके: ${i.crops.ifBlank { "-" }}")
        }

        // Contacts (per Gram Panchayat)
        SectionCard("Important Contacts", "महत्त्वाचे संपर्क") {
            Text("ग्रामपंचायत: ${v.gp}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            ContactRow("सरपंच / Sarpanch", gc?.sarpanch ?: "", gc?.sarpanchPhone ?: "")
            ContactRow("ग्रामसेवक / Gram Sevak", gc?.gramsevak ?: "", gc?.gramsevakPhone ?: "")
            ContactRow("पोलीस / Police", "", "100")
            ContactRow("रुग्णवाहिका / Ambulance", "", "108")
            ContactRow("आपत्कालीन / Emergency", "", "112")
            if (gc == null) Text("या ग्रामपंचायतीचे संपर्क अजून भरलेले नाहीत.", fontSize = 12.sp, color = Red)
            OutlinedButton(onClick = onEditGp, modifier = Modifier.fillMaxWidth()) {
                Text("✏️ ग्रामपंचायत संपर्क भरा / बदला")
            }
            Text("हे संपर्क '${v.gp}' मधील सर्व गावांना आपोआप दिसतात.", fontSize = 11.sp)
        }
        Button(onClick = onEditInfo, modifier = Modifier.fillMaxWidth()) { Text("✏️ गावाची विस्तृत माहिती भरा / बदला") }
        VillageActions(s, v)
        Box(Modifier.fillMaxWidth().background(Brand, RoundedCornerShape(10.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
            Text("🌱 Developed Village  ✦  Prosperous Village  ✦  Happy Village 🌱", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun GpDialog(s: AppState, v: Village, onDismiss: () -> Unit) {
    val old = s.gpOf(v)
    var sp by remember { mutableStateOf(old?.sarpanch ?: "") }
    var spp by remember { mutableStateOf(old?.sarpanchPhone ?: "") }
    var gs by remember { mutableStateOf(old?.gramsevak ?: "") }
    var gsp by remember { mutableStateOf(old?.gramsevakPhone ?: "") }
    FormDialog("${v.gp} – संपर्क", onDismiss, onSave = {
        s.upsertGp(GpContact(gpKey(v.state, v.district, v.taluka, v.gp), sp.trim(), spp.trim(), gs.trim(), gsp.trim()))
        onDismiss()
    }) {
        Text("हे फक्त एकदा भरा. '${v.gp}' मधील सर्व गावांना लागू होईल.", fontSize = 12.sp)
        Field("सरपंच नाव", sp, { sp = it })
        Field("सरपंच मोबाईल", spp, { if (it.length <= 10 && it.all(Char::isDigit)) spp = it }, number = true)
        Field("ग्रामसेवक नाव", gs, { gs = it })
        Field("ग्रामसेवक मोबाईल", gsp, { if (it.length <= 10 && it.all(Char::isDigit)) gsp = it }, number = true)
    }
}

@Composable
fun GpImportDialog(s: AppState, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    FormDialog("सरपंच / ग्रामसेवक यादी इंपोर्ट", onDismiss, onSave = {
        val n = s.importGp(text)
        if (n > 0) onDismiss() else msg = "एकही ओळ वाचता आली नाही. फॉरमॅट तपासा."
    }) {
        Text("Excel मधून कॉपी करून पेस्ट करा. प्रत्येक ओळ एक ग्रामपंचायत, 8 रकाने (स्वल्पविराम किंवा टॅबने वेगळे):", fontSize = 12.sp)
        Text("राज्य, जिल्हा, तालुका, ग्रामपंचायत, सरपंच नाव, सरपंच मोबाईल, ग्रामसेवक नाव, ग्रामसेवक मोबाईल", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("उदा: Maharashtra, Nashik, Surgana, Alangun, नाव, 9876543210, नाव, 9876543211", fontSize = 11.sp)
        Text("ग्रामपंचायतीचे स्पेलिंग गाव निवडताना दिसते तसेच (English) असावे.", fontSize = 11.sp)
        Field("यादी पेस्ट करा", text, { text = it; msg = "" }, lines = 6)
        if (msg.isNotEmpty()) Text(msg, color = Red)
    }
}

@Composable
fun InfoDialog(s: AppState, v: Village, onDismiss: () -> Unit) {
    val o = s.infos[v.id] ?: Info()
    fun i(x: Int) = if (x == 0) "" else x.toString()
    fun d(x: Double) = if (x == 0.0) "" else x.toString()
    var male by remember { mutableStateOf(i(o.male)) }
    var female by remember { mutableStateOf(i(o.female)) }
    var lit by remember { mutableStateOf(d(o.literacy)) }
    var prim by remember { mutableStateOf(d(o.eduPrimary)) }
    var sec by remember { mutableStateOf(d(o.eduSecondary)) }
    var high by remember { mutableStateOf(d(o.eduHigher)) }
    var houses by remember { mutableStateOf(i(o.houses)) }
    var water by remember { mutableStateOf(i(o.water)) }
    var elec by remember { mutableStateOf(i(o.elec)) }
    var schools by remember { mutableStateOf(i(o.schools)) }
    var angan by remember { mutableStateOf(i(o.anganwadi)) }
    var health by remember { mutableStateOf(i(o.health)) }
    var area by remember { mutableStateOf(d(o.areaHa)) }
    var agri by remember { mutableStateOf(d(o.agriHa)) }
    var forest by remember { mutableStateOf(d(o.forestHa)) }
    var wb by remember { mutableStateOf(i(o.waterBodies)) }
    var irr by remember { mutableStateOf(i(o.irrigation)) }
    var crops by remember { mutableStateOf(o.crops) }
    var lat by remember { mutableStateOf(d(o.lat)) }
    var lon by remember { mutableStateOf(d(o.lon)) }
    FormDialog("${v.name} – विस्तृत माहिती", onDismiss, onSave = {
        s.upsertInfo(v.id, Info(
            male.toIntOrNull() ?: 0, female.toIntOrNull() ?: 0, lit.toDoubleOrNull() ?: 0.0,
            prim.toDoubleOrNull() ?: 0.0, sec.toDoubleOrNull() ?: 0.0, high.toDoubleOrNull() ?: 0.0,
            houses.toIntOrNull() ?: 0, (water.toIntOrNull() ?: 0).coerceIn(0, 100), (elec.toIntOrNull() ?: 0).coerceIn(0, 100),
            schools.toIntOrNull() ?: 0, angan.toIntOrNull() ?: 0, health.toIntOrNull() ?: 0,
            area.toDoubleOrNull() ?: 0.0, agri.toDoubleOrNull() ?: 0.0, forest.toDoubleOrNull() ?: 0.0,
            wb.toIntOrNull() ?: 0, (irr.toIntOrNull() ?: 0).coerceIn(0, 100), crops.trim(), today(),
            lat.toDoubleOrNull() ?: 0.0, lon.toDoubleOrNull() ?: 0.0
        ))
        onDismiss()
    }) {
        Text("एकूण लोकसंख्या आणि कुटुंबे गाव संपादित करा मधून बदला.", fontSize = 11.sp)
        Field("पुरुष लोकसंख्या", male, { male = it }, number = true)
        Field("महिला लोकसंख्या", female, { female = it }, number = true)
        Field("साक्षरता दर (%)", lit, { lit = it }, decimal = true)
        Field("प्राथमिक शिक्षण (%)", prim, { prim = it }, decimal = true)
        Field("माध्यमिक शिक्षण (%)", sec, { sec = it }, decimal = true)
        Field("उच्च शिक्षण (%)", high, { high = it }, decimal = true)
        Field("एकूण घरे", houses, { houses = it }, number = true)
        Field("पाणी सुविधा (%)", water, { water = it }, number = true)
        Field("वीज सुविधा (%)", elec, { elec = it }, number = true)
        Field("शाळा संख्या", schools, { schools = it }, number = true)
        Field("अंगणवाडी संख्या", angan, { angan = it }, number = true)
        Field("आरोग्य केंद्र संख्या", health, { health = it }, number = true)
        Field("एकूण क्षेत्रफळ (हेक्टर)", area, { area = it }, decimal = true)
        Field("शेती जमीन (हेक्टर)", agri, { agri = it }, decimal = true)
        Field("वनक्षेत्र (हेक्टर)", forest, { forest = it }, decimal = true)
        Field("जलस्रोत संख्या", wb, { wb = it }, number = true)
        Field("सिंचन (%)", irr, { irr = it }, number = true)
        Field("मुख्य पिके", crops, { crops = it })
        Text("नकाशासाठी (ऐच्छिक): Google Maps मध्ये गावावर दाबून धरा, वर दिसणारे आकडे कॉपी करा.", fontSize = 11.sp)
        Field("अक्षांश (Latitude)", lat, { lat = it }, decimal = true)
        Field("रेखांश (Longitude)", lon, { lon = it }, decimal = true)
    }
}
