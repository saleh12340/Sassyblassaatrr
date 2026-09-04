package com.alazzi.grocery

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class GroceryNavDestination(val titleAr: String, val icon: ImageVector) {
    POS("نقاط البيع", Icons.Default.PointOfSale),
    CUSTOMERS("العملاء والديون", Icons.Default.People),
    PRODUCTS("المخزون والسلع", Icons.Default.Inventory2),
    REPORTS("التقارير والسجلات", Icons.Default.Assessment)
}

class MainActivity : ComponentActivity() {

    private val viewModel: GroceryViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AlAzziGroceryTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val state by viewModel.uiState.collectAsState()
                    val context = LocalContext.current

                    LaunchedEffect(state.toastMessage) {
                        state.toastMessage?.let { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }

                    var currentDestination by remember { mutableStateOf(GroceryNavDestination.POS) }
                    var showSplashScreen by remember { mutableStateOf(true) }
                    var showStoreInfoDialog by remember { mutableStateOf(false) }

                    if (showSplashScreen) {
                        SplashScreen(
                            onDismiss = { showSplashScreen = false }
                        )
                    } else {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    navigationIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .padding(start = 12.dp, end = 4.dp)
                                                .size(40.dp)
                                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                .clip(CircleShape)
                                                .clickable { showStoreInfoDialog = true }
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_splash_bg),
                                                contentDescription = "صورة صاحب البقالة",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    },
                                    title = {
                                        Column(modifier = Modifier.clickable { showStoreInfoDialog = true }) {
                                            Text(
                                                text = "بقالة العزي للمواد الغذائية",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            )
                                            Text(
                                                text = currentDestination.titleAr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { showStoreInfoDialog = true }) {
                                            Icon(Icons.Default.Storefront, contentDescription = "معلومات البقالة والمالك")
                                        }
                                        IconButton(onClick = { viewModel.loadAllData() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "تحديث البيانات")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    GroceryNavDestination.values().forEach { dest ->
                                        NavigationBarItem(
                                            selected = currentDestination == dest,
                                            onClick = { currentDestination = dest },
                                            icon = {
                                                Icon(dest.icon, contentDescription = dest.titleAr)
                                            },
                                            label = {
                                                Text(dest.titleAr, fontWeight = if (currentDestination == dest) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Subtle ambient background of the grocery store
                                Image(
                                    painter = painterResource(id = R.drawable.img_splash_bg),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.05f),
                                    contentScale = ContentScale.Crop
                                )

                                when (currentDestination) {
                                    GroceryNavDestination.POS -> PosScreen(
                                        viewModel = viewModel,
                                        onOpenStoreInfo = { showStoreInfoDialog = true }
                                    )
                                    GroceryNavDestination.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                                    GroceryNavDestination.PRODUCTS -> ProductsScreen(viewModel = viewModel)
                                    GroceryNavDestination.REPORTS -> ReportsScreen(viewModel = viewModel)
                                }
                            }
                        }

                        // Store Info Dialog
                        if (showStoreInfoDialog) {
                            StoreInfoDialog(
                                onDismiss = { showStoreInfoDialog = false },
                                onShowSplash = {
                                    showStoreInfoDialog = false
                                    showSplashScreen = true
                                }
                            )
                        }
                    }

                    // Thermal Receipt Dialog preview
                    if (state.isShowingReceiptDialog && state.currentReceiptInvoice != null) {
                        ThermalReceiptDialog(
                            invoice = state.currentReceiptInvoice!!,
                            onDismiss = { viewModel.showReceiptDialog(null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreInfoDialog(
    onDismiss: () -> Unit,
    onShowSplash: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Store photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_splash_bg),
                        contentDescription = "صورة بقالة العزي",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                    startY = 100f
                                )
                            )
                    )

                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "بقالة العزي للمواد الغذائية",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "مرحباً بكم دائماً • خدمة موثوقة",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Store info pills
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المالك والإدارة: العزي", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("رقم التواصل: 0501112233", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("النشاط: مواد غذائية، معلبات، مشروبات وتموينات", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            ThermalReceiptHelper.shareViaWhatsApp(context, "0501112233", "السلام عليكم، بقالة العزي للمواد الغذائية")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFDCFCE7),
                            contentColor = Color(0xFF15803D)
                        )
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onShowSplash,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("شاشة البداية", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
