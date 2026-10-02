package com.ganeshadigital.mlagaav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class NavEntry(val key: String, val icon: String, val label: String)

val MENU_MAIN = listOf(
    NavEntry("home", "🏠", "मुख्यपृष्ठ"),
    NavEntry("myvillage", "🏡", "माझे गाव"),
    NavEntry("info", "ℹ️", "गावाची माहिती"),
    NavEntry("families", "👨‍👩‍👧", "कुटुंब नोंदणी"),
    NavEntry("gp", "🏛", "ग्रामपंचायत"),
    NavEntry("schemes", "📜", "ग्रामपंचायत योजना"),
    NavEntry("apply", "📨", "ऑनलाईन अर्ज"),
    NavEntry("news", "📢", "सूचना / बातम्या"),
    NavEntry("contacts", "📞", "महत्त्वाचे संपर्क"),
    NavEntry("services", "🛠", "गावातील सेवा"),
    NavEntry("map", "📍", "गावाचा नकाशा"),
    NavEntry("gallery", "🖼", "गावाचे फोटो / गॅलरी"),
    NavEntry("help", "❓", "मदत / FAQ"),
    NavEntry("contactus", "☎️", "संपर्क")
)

val MENU_LOGIN = listOf(
    NavEntry("login", "👤", "कुटुंब प्रमुख Login"),
    NavEntry("register", "📝", "नवीन कुटुंब नोंदणी"),
    NavEntry("myinfo", "🪪", "माझी माहिती"),
    NavEntry("members", "👥", "कुटुंबातील सदस्य"),
    NavEntry("appstatus", "📋", "अर्जांची स्थिती"),
    NavEntry("request", "🛠", "दुरुस्ती विनंती"),
    NavEntry("logout", "🚪", "Logout")
)

val MENU_ADMIN = listOf(
    NavEntry("a_dash", "📊", "Dashboard"),
    NavEntry("a_verify", "✅", "कुटुंब नोंदणी तपासणी"),
    NavEntry("a_members", "👥", "सदस्य माहिती"),
    NavEntry("a_status", "📋", "मंजूर / प्रलंबित नोंदी"),
    NavEntry("a_schemes", "📜", "योजना व्यवस्थापन"),
    NavEntry("a_apps", "📨", "ऑनलाइन अर्ज"),
    NavEntry("a_notices", "📢", "सूचना व्यवस्थापन"),
    NavEntry("a_reports", "📈", "अहवाल / Reports"),
    NavEntry("a_users", "🔑", "User Management"),
    NavEntry("a_backup", "💾", "Data Backup"),
    NavEntry("a_settings", "⚙️", "Settings")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(s: AppState) {
    var page by remember { mutableStateOf("home") }
    var tab by remember { mutableIntStateOf(0) }
    var openId by remember { mutableStateOf<Long?>(null) }
    var adminAsk by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val opened = openId?.let { id -> s.villages.find { it.id == id } }

    fun go(p: String) {
        when {
            p == "logout" -> { s.logoutAll(); page = "home"; openId = null }
            p.startsWith("a_") && !s.adminMode -> adminAsk = p
            else -> { page = p; openId = null }
        }
    }
    fun menuTap(p: String) { scope.launch { drawer.close() }; go(p) }

    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(!drawer.isOpen && page != "home") { page = "home" }
    BackHandler(!drawer.isOpen && page == "home" && opened != null) { openId = null }

    val title = (MENU_MAIN + MENU_LOGIN + MENU_ADMIN).find { it.key == page }?.label ?: "My Village Data"

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = drawer.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("My Village Data", fontWeight = FontWeight.Bold, color = Brand, fontSize = 17.sp)
                            val fam = s.myFamily()
                            Text(
                                when {
                                    s.adminMode -> "🔐 ग्रामपंचायत Admin"
                                    fam != null -> "👤 ${fam.head}"
                                    else -> "पाहुणा (Login नाही)"
                                }, fontSize = 12.sp
                            )
                        }
                    }
                    HorizontalDivider()
                    DrawerGroup("मुख्य मेनू", MENU_MAIN, page, { menuTap(it) })
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    DrawerGroup("👤 Login / Admin Menu", MENU_LOGIN, page, { menuTap(it) })
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    DrawerGroup("🔐 ग्रामपंचायत Admin", MENU_ADMIN, page, { menuTap(it) })
                }
            }
        }
    ) {
        if (page == "home" && opened != null) {
            VillageDetail(s, opened) { openId = null }
        } else {
            val tabs = listOf("🏠" to "डॅशबोर्ड", "🏘" to "गावे", "⚠️" to "समस्या", "🏗" to "कामे")
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            TextButton(onClick = { scope.launch { drawer.open() } }) { Text("☰", fontSize = 24.sp, color = Color.White) }
                        },
                        title = {
                            if (page == "home") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("My Village Data", fontWeight = FontWeight.Bold)
                                }
                            } else Text(title, fontWeight = FontWeight.Bold)
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand, titleContentColor = Color.White)
                    )
                },
                bottomBar = {
                    if (page == "home") NavigationBar {
                        tabs.forEachIndexed { i, (icon, label) ->
                            NavigationBarItem(
                                selected = tab == i, onClick = { tab = i },
                                icon = { Text(icon, fontSize = 20.sp) }, label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    val nav: (String) -> Unit = { go(it) }
                    when (page) {
                        "home" -> when (tab) {
                            0 -> Dashboard(s) { openId = it }
                            1 -> VillagesScreen(s) { openId = it }
                            2 -> IssuesScreen(s)
                            else -> WorksScreen(s)
                        }
                        "myvillage" -> MyVillagePage(s)
                        "info" -> VillageInfoPage(s)
                        "families" -> FamiliesPage(s, nav)
                        "gp" -> GpPage(s)
                        "schemes" -> SchemesPage(s, nav)
                        "apply" -> ApplyPage(s, nav)
                        "news" -> NewsPage(s)
                        "contacts" -> ContactsPage(s)
                        "services" -> ServicesPage(s)
                        "map" -> MapPage(s)
                        "gallery" -> GalleryPage(s)
                        "help" -> HelpPage()
                        "contactus" -> ContactUsPage(s)
                        "login" -> LoginPage(s, nav)
                        "register" -> RegisterPage(s, nav)
                        "myinfo" -> MyInfoPage(s, nav)
                        "members" -> MembersPage(s, nav)
                        "appstatus" -> AppStatusPage(s, nav)
                        "request" -> RequestPage(s, nav)
                        else -> if (!s.adminMode) {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("हे पान पाहण्यासाठी ग्रामपंचायत Admin Login आवश्यक आहे.")
                            }
                        } else when (page) {
                            "a_dash" -> AdminDashPage(s, nav)
                            "a_verify" -> AdminVerifyPage(s)
                            "a_members" -> AdminMembersPage(s)
                            "a_status" -> AdminStatusPage(s)
                            "a_schemes" -> AdminSchemesPage(s)
                            "a_apps" -> AdminAppsPage(s)
                            "a_notices" -> AdminNoticesPage(s)
                            "a_reports" -> AdminReportsPage(s)
                            "a_users" -> AdminUsersPage(s)
                            "a_backup" -> AdminBackupPage(s)
                            "a_settings" -> AdminSettingsPage(s, nav)
                            else -> Text("पान सापडले नाही")
                        }
                    }
                }
            }
        }
    }

    adminAsk?.let { target ->
        AdminLoginDialog(s, onDismiss = { adminAsk = null }, onOk = { page = target; openId = null; adminAsk = null })
    }
}

@Composable
private fun DrawerGroup(title: String, items: List<NavEntry>, current: String, onTap: (String) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, color = Brand, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp))
    items.forEach { m ->
        NavigationDrawerItem(
            label = { Text(m.label) },
            icon = { Text(m.icon, fontSize = 18.sp) },
            selected = current == m.key,
            onClick = { onTap(m.key) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
