package com.alazzi.grocery

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
                    var showExitConfirmDialog by remember { mutableStateOf(false) }

                    // Exit confirmation interceptor
                    BackHandler(enabled = !showSplashScreen) {
                        when {
                            state.isShowingCustomerStatementDialog -> viewModel.closeCustomerStatement()
                            state.isShowingDbManagementDialog -> viewModel.showDbManagementDialog(false)
                            state.isShowingEditStoreDialog -> viewModel.showEditStoreDialog(false)
                            state.isShowingReceiptDialog -> viewModel.showReceiptDialog(null)
                            showStoreInfoDialog -> showStoreInfoDialog = false
                            currentDestination != GroceryNavDestination.POS -> currentDestination = GroceryNavDestination.POS
                            else -> showExitConfirmDialog = true
                        }
                    }

                    if (showExitConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitConfirmDialog = false },
                            icon = {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text("الخروج من التطبيق", fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Text(
                                    "هل أنت متأكد من رغبتك في إغلاق نظام ${state.storeInfo.name}؟ لن تفقد أي بيانات تم حفظها.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showExitConfirmDialog = false
                                        (context as? ComponentActivity)?.finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                                ) {
                                    Text("نعم، إغلاق التطبيق")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showExitConfirmDialog = false }) {
                                    Text("إلغاء واستمرار")
                                }
                            }
                        )
                    }

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
                                                text = state.storeInfo.name,
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
                                        IconButton(onClick = { viewModel.showDbManagementDialog(true) }) {
                                            Icon(Icons.Default.Storage, contentDescription = "إدارة واستيراد قاعدة البيانات")
                                        }
                                        IconButton(onClick = { viewModel.showEditStoreDialog(true) }) {
                                            Icon(Icons.Default.EditNote, contentDescription = "تعديل بيانات المتجر")
                                        }
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
                                storeInfo = state.storeInfo,
                                onEditStore = {
                                    showStoreInfoDialog = false
                                    viewModel.showEditStoreDialog(true)
                                },
                                onManageDb = {
                                    showStoreInfoDialog = false
                                    viewModel.showDbManagementDialog(true)
                                },
                                onDismiss = { showStoreInfoDialog = false },
                                onShowSplash = {
                                    showStoreInfoDialog = false
                                    showSplashScreen = true
                                }
                            )
                        }

                        // Edit Store Dialog
                        if (state.isShowingEditStoreDialog) {
                            EditStoreDialog(
                                initialStoreInfo = state.storeInfo,
                                onSave = { updated ->
                                    viewModel.updateStoreInfo(updated)
                                },
                                onDismiss = { viewModel.showEditStoreDialog(false) }
                            )
                        }

                        // Database Management & Import Dialog
                        if (state.isShowingDbManagementDialog) {
                            DatabaseManagementDialog(
                                stats = state.databaseStats,
                                isLoading = state.isDatabaseLoading,
                                onImport = { uri ->
                                    viewModel.importDatabaseFromUri(uri) { _, _ -> }
                                },
                                onExport = { uri ->
                                    viewModel.exportDatabaseToUri(uri) { _, _ -> }
                                },
                                onShare = {
                                    viewModel.shareDatabaseBackup(context)
                                },
                                onDismiss = { viewModel.showDbManagementDialog(false) }
                            )
                        }

                        // Customer Statement Dialog
                        if (state.isShowingCustomerStatementDialog && state.statementCustomer != null) {
                            val cust = state.statementCustomer!!
                            CustomerStatementDialog(
                                customer = cust,
                                invoices = state.statementInvoices,
                                payments = state.statementPayments,
                                storeInfo = state.storeInfo,
                                onDismiss = { viewModel.closeCustomerStatement() },
                                onAddMovement = { isForCustomer, amount, details ->
                                    viewModel.addDirectMovement(cust.id, cust.name, isForCustomer, amount, details)
                                },
                                onUpdatePayment = { paymentId, amount, notes ->
                                    viewModel.updatePaymentRecord(paymentId, cust.id, amount, notes)
                                },
                                onUpdateInvoice = { invoiceId, total, paid, desc ->
                                    viewModel.updateInvoiceRecord(invoiceId, cust.id, total, paid, desc)
                                }
                            )
                        }
                    }

                    // Thermal Receipt Dialog preview
                    if (state.isShowingReceiptDialog && state.currentReceiptInvoice != null) {
                        ThermalReceiptDialog(
                            invoice = state.currentReceiptInvoice!!,
                            storeInfo = state.storeInfo,
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
    storeInfo: StoreInfo,
    onEditStore: () -> Unit,
    onManageDb: () -> Unit,
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
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_splash_bg),
                        contentDescription = "صورة المتجر",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 80f
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
                            text = storeInfo.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = storeInfo.activity,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Store info details card
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
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المالك / المسؤول: ${storeInfo.ownerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("رقم الهاتف: ${storeInfo.phone}", fontSize = 13.sp)
                        }
                        if (storeInfo.address.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("العنوان: ${storeInfo.address}", fontSize = 13.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("العملة المعتمدة: ${storeInfo.currency}", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Management Action Buttons
                Button(
                    onClick = onEditStore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل اسم وبيانات المتجر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onManageDb,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استيراد ونسخ قاعدة البيانات", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            ThermalReceiptHelper.shareViaWhatsApp(context, storeInfo.phone, "السلام عليكم، مرحباً بكم في ${storeInfo.name}")
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
                        Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShowSplash,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("شاشة البداية", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
