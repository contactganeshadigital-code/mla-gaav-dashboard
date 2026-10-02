package com.ganeshadigital.mlagaav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
fun Tile(iconRes: Int, en: String, mr: String, value: String, titleColor: Color, bg: Color, modifier: Modifier) {
    Card(
        modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, titleColor.copy(alpha = 0.25f))
    ) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(46.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(en, fontSize = 11.sp, color = titleColor, fontWeight = FontWeight.Bold)
                Text(mr, fontSize = 11.sp, color = Color(0xFF33445A))
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B2A4A))
            }
        }
    }
}

@Composable
fun SectionCard(title: String, mr: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White),
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
fun VillageDashboard(s: AppState, v: Village, onEditInfo: () -> Unit, onEditGp: () -> Unit, onPoster: (() -> Unit)? = null) {
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

    val ctx = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painterResource(R.drawable.banner), contentDescription = "My Village Data",
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            )
            if (onPoster != null && !wide) {
                Button(onClick = onPoster, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF023561))) {
                    Text("📊 पूर्ण डॅशबोर्ड (poster सारखा, zoom करा)")
                }
            }
            // Header strip (village / taluka / district / last updated)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF023561))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (wide) {
                        Row(Modifier.fillMaxWidth()) {
                            HeaderCell(R.drawable.ic_hpin, "गावाचे नाव / Village", v.name, Modifier.weight(1f))
                            HeaderCell(R.drawable.ic_htal, "तालुका / Taluka", v.taluka, Modifier.weight(1f))
                            HeaderCell(R.drawable.ic_hdist, "जिल्हा / District", v.district, Modifier.weight(1f))
                            HeaderCell(R.drawable.ic_hcal, "शेवटची अपडेट / Last Updated", i.updated.ifBlank { "-" }, Modifier.weight(1f))
                        }
                    } else {
                        Row(Modifier.fillMaxWidth()) {
                            HeaderCell(R.drawable.ic_hpin, "गावाचे नाव / Village", v.name, Modifier.weight(1f))
                            HeaderCell(R.drawable.ic_htal, "तालुका / Taluka", v.taluka, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth()) {
                            HeaderCell(R.drawable.ic_hdist, "जिल्हा / District", v.district, Modifier.weight(1f))
                            HeaderCell(R.drawable.ic_hcal, "शेवटची अपडेट", i.updated.ifBlank { "-" }, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Tiles: 5 per row on wide screens, 2 per row on phones
            TileGrid(
                if (wide) 5 else 2,
                listOf<@Composable (Modifier) -> Unit>(
                    { m -> Tile(R.drawable.ic_pop, "Total Population", "एकूण लोकसंख्या", num(total), Color(0xFF1976D2), Color(0xFFEBF5FE), m) },
                    { m -> Tile(R.drawable.ic_fam, "Total Families", "एकूण कुटुंबे", num(famCount), Color(0xFF2E7D32), Color(0xFFE4FCEA), m) },
                    { m -> Tile(R.drawable.ic_male, "Male Population", "पुरुष लोकसंख्या", num(male), Color(0xFF1565C0), Color(0xFFE8F4FD), m) },
                    { m -> Tile(R.drawable.ic_female, "Female Population", "महिला लोकसंख्या", num(female), Color(0xFFD81B60), Color(0xFFFDEBF4), m) },
                    { m -> Tile(R.drawable.ic_lit, "Literacy Rate", "साक्षरता दर", pct(i.literacy), Color(0xFFB26A00), Color(0xFFFEF7D9), m) },
                    { m -> Tile(R.drawable.ic_house, "Total Houses", "एकूण घरे", num(i.houses), Color(0xFF6A1B9A), Color(0xFFF6EEFD), m) },
                    { m -> Tile(R.drawable.ic_water, "Water Facilities", "पाणी सुविधा", if (i.water > 0) "${i.water}%" else "-", Color(0xFF00838F), Color(0xFFE7FAF9), m) },
                    { m -> Tile(R.drawable.ic_elec, "Electricity Facilities", "वीज सुविधा", if (i.elec > 0) "${i.elec}%" else "-", Color(0xFFE65100), Color(0xFFFEF6E6), m) },
                    { m -> Tile(R.drawable.ic_school, "Schools & Anganwadi", "शाळा व अंगणवाडी", "${i.schools} / ${i.anganwadi}", Color(0xFF00796B), Color(0xFFE8FBFB), m) },
                    { m -> Tile(R.drawable.ic_health, "Health Facilities", "आरोग्य सुविधा", num(i.health), Color(0xFFC62828), Color(0xFFFDEEEE), m) }
                )
            )

            // Gender | Education | Map
            if (wide) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderCard(male, female, Modifier.weight(1f).fillMaxHeight())
                    EduCard(i, Modifier.weight(1f).fillMaxHeight())
                    MapCard(v, i, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                GenderCard(male, female, Modifier)
                EduCard(i, Modifier)
                MapCard(v, i, Modifier)
            }

            // Overview | Agriculture | Contacts
            if (wide) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverviewCard(i, Modifier.weight(1f).fillMaxHeight())
                    AgriCard(i, Modifier.weight(1f).fillMaxHeight())
                    ContactsCard(v, gc, onEditGp, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                OverviewCard(i, Modifier)
                AgriCard(i, Modifier)
                ContactsCard(v, gc, onEditGp, Modifier)
            }

            Button(onClick = onEditInfo, modifier = Modifier.fillMaxWidth()) { Text("✏️ गावाची विस्तृत माहिती भरा / बदला") }
            VillageActions(s, v, wide)
            Box(Modifier.fillMaxWidth().background(Brand, RoundedCornerShape(10.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                Text("🌱 Developed Village  ✦  Prosperous Village  ✦  Happy Village 🌱", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HeaderCell(icon: Int, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun TileGrid(cols: Int, items: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { it(Modifier.weight(1f)) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GenderCard(male: Int, female: Int, modifier: Modifier) {
    SectionCard("Population by Gender", "लिंगानुसार लोकसंख्या", modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 30f
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 11.sp)
                    Text(num(male + female), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            val t = (male + female).coerceAtLeast(1)
            Text("● Male / पुरुष: $male (${"%.1f".format(100.0 * male / t)}%)", color = Blue, fontSize = 13.sp)
            Text("● Female / महिला: $female (${"%.1f".format(100.0 * female / t)}%)", color = Pink, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EduCard(i: Info, modifier: Modifier) {
    SectionCard("Education Level", "शिक्षण स्तर", modifier) {
        val bars = listOf(
            Triple("Literate\nसाक्षर", i.literacy, Green), Triple("Primary\nप्राथमिक", i.eduPrimary, Blue),
            Triple("Secondary\nमाध्यमिक", i.eduSecondary, Orange), Triple("Higher\nउच्च", i.eduHigher, Purple)
        )
        Row(Modifier.fillMaxWidth().height(170.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEach { (label, value, c) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(pct(value), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier.fillMaxWidth(0.55f).height((value.coerceIn(0.0, 100.0) * 1.0).dp.coerceAtLeast(2.dp))
                            .background(c, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                    Text(label, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun MapCard(v: Village, i: Info, modifier: Modifier) {
    val ctx = LocalContext.current
    val query = if (i.lat != 0.0 && i.lon != 0.0) "${i.lat},${i.lon}" else "${v.name}, ${v.taluka}, ${v.district}, ${v.state}"
    SectionCard("Village Map", "गावाचा नकाशा", modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📍", fontSize = 44.sp)
            Text(v.name, fontWeight = FontWeight.Bold)
            Text("ता. ${v.taluka}, जि. ${v.district}", fontSize = 12.sp)
            Button(onClick = { openUrl(ctx, "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)) }) {
                Text("🗺 Google Maps मध्ये उघडा", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun OverviewCard(i: Info, modifier: Modifier) {
    SectionCard("Village Overview", "गावाचा आढावा", modifier) {
        Row(Modifier.fillMaxWidth()) {
            OvItem("🌳", "एकूण क्षेत्रफळ", ha(i.areaHa), Modifier.weight(1f))
            OvItem("🌾", "शेती जमीन", ha(i.agriHa), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            OvItem("🌲", "वनक्षेत्र", ha(i.forestHa), Modifier.weight(1f))
            OvItem("💧", "जलस्रोत", num(i.waterBodies), Modifier.weight(1f))
        }
    }
}

@Composable
private fun OvItem(icon: String, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(6.dp))
        Column { Text(label, fontSize = 11.sp); Text(value, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AgriCard(i: Info, modifier: Modifier) {
    SectionCard("Agriculture & Land", "शेती व जमीन", modifier) {
        Text("• कसुती जमीन: ${ha(i.agriHa)}")
        Text("• सिंचन: ${if (i.irrigation > 0) "${i.irrigation}%" else "-"}")
        Text("• मुख्य पिके: ${i.crops.ifBlank { "-" }}")
    }
}

@Composable
private fun ContactsCard(v: Village, gc: GpContact?, onEditGp: () -> Unit, modifier: Modifier) {
    SectionCard("Important Contacts", "महत्त्वाचे संपर्क", modifier) {
        Text("ग्रामपंचायत: ${v.gp}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        ContactRow("सरपंच / Sarpanch", gc?.sarpanch ?: "", gc?.sarpanchPhone ?: "")
        ContactRow("ग्रामसेवक / Gram Sevak", gc?.gramsevak ?: "", gc?.gramsevakPhone ?: "")
        ContactRow("पोलीस / Police", "", "100")
        ContactRow("रुग्णवाहिका / Ambulance", "", "108")
        ContactRow("आपत्कालीन / Emergency", "", "112")
        if (gc == null) Text("या ग्रामपंचायतीचे संपर्क अजून भरलेले नाहीत.", fontSize = 12.sp, color = Red)
        OutlinedButton(onClick = onEditGp, modifier = Modifier.fillMaxWidth()) {
            Text("✏️ ग्रामपंचायत संपर्क भरा / बदला", fontSize = 12.sp)
        }
        Text("हे संपर्क '${v.gp}' मधील सर्व गावांना आपोआप दिसतात.", fontSize = 11.sp)
    }
}

@Composable
fun PosterScreen(s: AppState, v: Village, onBack: () -> Unit) {
    val designW = 760.dp
    Column(Modifier.fillMaxSize().background(Color(0xFFF2F6FB))) {
        var resetKey by remember { mutableIntStateOf(0) }
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF023561)).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("←", fontSize = 22.sp, color = Color.White) }
            Text(v.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { resetKey++ }) { Text("⟲ रीसेट", color = Color.White) }
        }
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
            val density = LocalDensity.current
            val viewPx = with(density) { maxWidth.toPx() }
            val designPx = with(density) { designW.toPx() }
            val scale0 = viewPx / designPx
            var scale by remember(resetKey) { mutableFloatStateOf(scale0) }
            var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(resetKey) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val ns = (scale * zoom).coerceIn(scale0, 4f)
                            val z = ns / scale
                            var o = centroid - (centroid - offset) * z + pan
                            val minX = minOf(0f, viewPx - designPx * ns)
                            o = Offset(o.x.coerceIn(minX, 0f), o.y.coerceAtMost(0f))
                            scale = ns
                            offset = o
                        }
                    }
            ) {
                Box(
                    Modifier
                        .requiredWidth(designW)
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offset.x; translationY = offset.y
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    VillageDashboard(s, v, {}, {}, null)
                }
            }
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
