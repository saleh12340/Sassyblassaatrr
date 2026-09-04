package com.alazzi.grocery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

enum class StatementFilterPeriod(val titleAr: String) {
    ALL("الكل"),
    MONTHLY("شهري"),
    YEARLY("سنوي"),
    PREVIOUS_BALANCE("رصيد سابق")
}

data class StatementRecord(
    val id: Long,
    val opNumber: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val remaining: Double,
    val isPayment: Boolean, // true = له (دفع وسدد), false = عليه (دين)
    val timestamp: Long,
    var cumulativeBalance: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerStatementDialog(
    customer: Customer,
    invoices: List<Invoice>,
    payments: List<DebtPayment>,
    storeInfo: StoreInfo,
    onDismiss: () -> Unit,
    onAddMovement: (isForCustomer: Boolean, amount: Double, details: String) -> Unit = { _, _, _ -> },
    onUpdatePayment: (paymentId: Long, amount: Double, notes: String) -> Unit = { _, _, _ -> },
    onUpdateInvoice: (invoiceId: Long, total: Double, paid: Double, desc: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    // State variables
    var selectedPeriod by remember { mutableStateOf(StatementFilterPeriod.ALL) }
    var selectedCurrency by remember { mutableStateOf("يمني") } // يمني or سعودي
    var searchQuery by remember { mutableStateOf("") }
    var showSearchRow by remember { mutableStateOf(false) }

    // Dialogs
    var showAddMovementDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<StatementRecord?>(null) }
    var showExportOptionsSheet by remember { mutableStateOf(false) }

    // Build timeline and calculate cumulative balance
    val allRecords = remember(invoices, payments, selectedPeriod, searchQuery) {
        val invRecords = invoices.map { inv ->
            StatementRecord(
                id = inv.id,
                opNumber = inv.invoiceNumber.replace("INV-", "").replace("OP-", ""),
                title = if (inv.items.isNotEmpty()) inv.items.joinToString("، ") { it.productName } else "مشتريات",
                subtitle = "فاتورة مشتريات",
                amount = inv.totalAmount,
                remaining = inv.remainingDebt,
                isPayment = false,
                timestamp = inv.timestamp
            )
        }
        val payRecords = payments.map { pay ->
            StatementRecord(
                id = pay.id,
                opNumber = pay.id.toString(),
                title = pay.notes.ifBlank { "دفعة نقدية مسددة" },
                subtitle = "سداد نقدي",
                amount = pay.amount,
                remaining = 0.0,
                isPayment = true,
                timestamp = pay.timestamp
            )
        }

        // Sort chronologically ascending to compute running balances correctly
        val sortedAsc = (invRecords + payRecords).sortedBy { it.timestamp }
        var runningBal = 0.0
        val withBalance = sortedAsc.map { r ->
            if (r.isPayment) {
                runningBal -= r.amount // payment reduces debit
            } else {
                runningBal += r.amount // invoice increases debit
            }
            r.copy(cumulativeBalance = runningBal)
        }

        // Apply filter period & search
        val filtered = withBalance.filter { r ->
            val matchQuery = searchQuery.isBlank() ||
                    r.title.contains(searchQuery, ignoreCase = true) ||
                    r.opNumber.contains(searchQuery, ignoreCase = true)
            matchQuery
        }

        filtered.sortedByDescending { it.timestamp }
    }

    val totalDebit = remember(invoices) { invoices.sumOf { it.totalAmount } }
    val totalCredit = remember(payments) { payments.sumOf { it.amount } }
    val netBalance = totalDebit - totalCredit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                // TopBar styled exactly as Screenshot 1
                TopAppBar(
                    title = {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchRow = !showSearchRow }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        }
                        IconButton(onClick = { showExportOptionsSheet = true }) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة ومشاركة")
                        }
                        IconButton(onClick = {
                            selectedPeriod = when (selectedPeriod) {
                                StatementFilterPeriod.ALL -> StatementFilterPeriod.MONTHLY
                                StatementFilterPeriod.MONTHLY -> StatementFilterPeriod.YEARLY
                                StatementFilterPeriod.YEARLY -> StatementFilterPeriod.PREVIOUS_BALANCE
                                StatementFilterPeriod.PREVIOUS_BALANCE -> StatementFilterPeriod.ALL
                            }
                        }) {
                            Icon(Icons.Default.FilterList, contentDescription = "تصفية")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                // Bottom summary bar matching Screenshot 1
                Surface(
                    color = Color(0xFF1E5B94),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "عليه : ${String.format(Locale.US, "%,.0f", totalDebit)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "|",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "له : ${String.format(Locale.US, "%,.0f", totalCredit)}",
                                color = Color(0xFF86EFAC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val balLabel = if (netBalance > 0) "الرصيد عليه : ${String.format(Locale.US, "%,.0f", netBalance)}"
                        else "الرصيد له : ${String.format(Locale.US, "%,.0f", -netBalance)}"

                        Text(
                            text = balLabel,
                            color = if (netBalance > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            },
            floatingActionButton = {
                // FAB for adding payment or debit movement
                FloatingActionButton(
                    onClick = { showAddMovementDialog = true },
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة حركة جديدة", modifier = Modifier.size(28.dp))
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search row if opened
                if (showSearchRow) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في العمليات برقم العملية أو البيان...", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Currency selector bar (Screenshot 1: "العملة: يمني")
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("العملة: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            listOf("يمني", "سعودي").forEach { cur ->
                                FilterChip(
                                    selected = selectedCurrency == cur,
                                    onClick = { selectedCurrency = cur },
                                    label = { Text(cur, fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        // Quick Share Full Account Button
                        TextButton(
                            onClick = { showExportOptionsSheet = true },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة الكشف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Filter Period Row (شهري / الكل / رصيد سابق / سنوي) matching Screenshot 1
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatementFilterPeriod.values().forEach { period ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { selectedPeriod = period }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedPeriod == period,
                                    onClick = { selectedPeriod = period },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = period.titleAr,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPeriod == period) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Date Range Bar with Navigation Arrows (من 16/5/2026 إلى 27/5/2026)
                Surface(
                    color = Color(0xFF1E5B94).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "السابق",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )

                        val todayStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())
                        Text(
                            text = "سجل العمليات حتى اليوم ($todayStr)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "التالي",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Table Header Bar (Screenshot 1: الرصيد | التفاصيل | المبلغ | النوع)
                Surface(
                    color = Color(0xFF1E5B94),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الرصيد",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = "التفاصيل / البيان",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "المبلغ",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = "النوع",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }

                // Transactions List
                if (allRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "لا توجد عمليات مسجلة لهذا العميل",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(allRecords, key = { "${if (it.isPayment) "P" else "I"}_${it.id}" }) { record ->
                            CustomerMovementRow(
                                record = record,
                                currency = storeInfo.currency,
                                onEdit = { editingRecord = record },
                                onShareWhatsApp = {
                                    // Send single operation via WhatsApp
                                    FileExportHelper.shareSingleOperationViaWhatsApp(
                                        context = context,
                                        customerPhone = customer.phone,
                                        operationNumber = record.opNumber,
                                        customerName = customer.name,
                                        operationType = if (record.isPayment) "سداد دفعة نقدية (له)" else "مشتريات على الحساب (عليه)",
                                        amount = record.amount,
                                        date = ThermalReceiptHelper.formatDate(record.timestamp),
                                        details = record.title,
                                        balance = record.cumulativeBalance,
                                        storeInfo = storeInfo
                                    )
                                },
                                onExportPdf = {
                                    // Export single operation as PDF
                                    val row = ExportRow(
                                        operationNum = record.opNumber,
                                        date = ThermalReceiptHelper.formatDate(record.timestamp),
                                        type = if (record.isPayment) "له" else "عليه",
                                        details = record.title,
                                        amount = record.amount,
                                        balance = record.cumulativeBalance
                                    )
                                    val file = FileExportHelper.createCustomerStatementPdf(
                                        context = context,
                                        customer = customer,
                                        storeInfo = storeInfo,
                                        rows = listOf(row),
                                        totalDebit = if (!record.isPayment) record.amount else 0.0,
                                        totalCredit = if (record.isPayment) record.amount else 0.0,
                                        finalBalance = record.cumulativeBalance
                                    )
                                    if (file != null) {
                                        FileExportHelper.shareFile(context, file, "application/pdf", "فاتورة عملية #${record.opNumber}")
                                    } else {
                                        Toast.makeText(context, "فشل إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onExportImage = {
                                    // Export single operation as Image
                                    val row = ExportRow(
                                        operationNum = record.opNumber,
                                        date = ThermalReceiptHelper.formatDate(record.timestamp),
                                        type = if (record.isPayment) "له" else "عليه",
                                        details = record.title,
                                        amount = record.amount,
                                        balance = record.cumulativeBalance
                                    )
                                    val file = FileExportHelper.createStatementImage(
                                        context = context,
                                        customerName = customer.name,
                                        storeInfo = storeInfo,
                                        rows = listOf(row),
                                        totalDebit = if (!record.isPayment) record.amount else 0.0,
                                        totalCredit = if (record.isPayment) record.amount else 0.0,
                                        finalBalance = record.cumulativeBalance
                                    )
                                    if (file != null) {
                                        FileExportHelper.shareFile(context, file, "image/jpeg", "سند عملية #${record.opNumber}")
                                    } else {
                                        Toast.makeText(context, "فشل إنشاء صورة العملية", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // "إضافة مبلغ" Dialog styled exactly as Screenshot 2
    if (showAddMovementDialog) {
        AddMovementDialog(
            customer = customer,
            recentRecords = allRecords.take(5),
            currency = storeInfo.currency,
            onDismiss = { showAddMovementDialog = false },
            onConfirm = { isForCustomer, amount, details ->
                onAddMovement(isForCustomer, amount, details)
                showAddMovementDialog = false
            }
        )
    }

    // Edit Transaction Dialog
    if (editingRecord != null) {
        EditTransactionDialog(
            record = editingRecord!!,
            onDismiss = { editingRecord = null },
            onSavePayment = { newAmt, notes ->
                onUpdatePayment(editingRecord!!.id, newAmt, notes)
                editingRecord = null
            },
            onSaveInvoice = { newTotal, newPaid, desc ->
                onUpdateInvoice(editingRecord!!.id, newTotal, newPaid, desc)
                editingRecord = null
            }
        )
    }

    // Export & Share Sheet (WhatsApp, PDF, Excel, Image)
    if (showExportOptionsSheet) {
        ExportStatementSheet(
            customer = customer,
            records = allRecords,
            storeInfo = storeInfo,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            finalBalance = netBalance,
            onDismiss = { showExportOptionsSheet = false }
        )
    }
}

/**
 * Individual Transaction Row Matching Screenshot 1
 */
@Composable
fun CustomerMovementRow(
    record: StatementRecord,
    currency: String,
    onEdit: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/M/yyyy h:mm a", Locale.US).format(Date(record.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Balance column (الرصيد)
            Text(
                text = String.format(Locale.US, "%,.0f", record.cumulativeBalance),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (record.cumulativeBalance > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                modifier = Modifier.width(70.dp)
            )

            // Details & Operation # (التفاصيل)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${record.opNumber} • ${record.subtitle}",
                    fontSize = 10.sp,
                    color = Color(0xFFDC2626), // Red operation tag as in screenshot
                    fontWeight = FontWeight.Bold
                )
            }

            // Amount column (المبلغ) + Date/Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(90.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%,.0f", record.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (record.isPayment) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
                Text(
                    text = dateStr,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Type badge (له / عليه)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (record.isPayment) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                modifier = Modifier.width(40.dp)
            ) {
                Text(
                    text = if (record.isPayment) "له" else "عليه",
                    color = if (record.isPayment) Color(0xFF16A34A) else Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }

        // Action Icons Row: Edit, PDF/Doc, Image, Share (Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "تعديل",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onExportPdf, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onExportImage, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "صورة",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onShareWhatsApp, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "مشاركة واتساب",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * "إضافة مبلغ" Dialog Styled Exactly As Screenshot 2
 */
@Composable
fun AddMovementDialog(
    customer: Customer,
    recentRecords: List<StatementRecord>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (isForCustomer: Boolean, amount: Double, details: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var detailsText by remember { mutableStateOf("") }
    var selectedCur by remember { mutableStateOf("يمني") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header (إضافة مبلغ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة مبلغ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer Name Row with clear button
                Surface(
                    color = Color(0xFFFEE2E2).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount field with calculator icon
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date display
                val today = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التاريخ: $today", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Details / Note
                OutlinedTextField(
                    value = detailsText,
                    onValueChange = { detailsText = it },
                    label = { Text("التفاصيل والبيان") },
                    placeholder = { Text("مثال: نقداً، دبة زيت، كشف حساب...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Currency Radio buttons (يمني / سعودي)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("يمني", "سعودي").forEach { cur ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedCur = cur }
                                .padding(horizontal = 8.dp)
                        ) {
                            RadioButton(selected = selectedCur == cur, onClick = { selectedCur = cur })
                            Text(cur, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Two large Action buttons: "له" (Green) and "عليه" (Blue/Red) matching Screenshot 2
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val isEnabled = amt > 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button "له" (سداد دفعة)
                    Button(
                        onClick = { onConfirm(true, amt, detailsText) },
                        enabled = isEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            disabledContainerColor = Color(0xFF16A34A).copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("له (سداد)", fontWeight = FontWeight.Bold)
                    }

                    // Button "عليه" (إضافة دين / مشتريات)
                    Button(
                        onClick = { onConfirm(false, amt, detailsText) },
                        enabled = isEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E5B94),
                            disabledContainerColor = Color(0xFF1E5B94).copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عليه (دين)", fontWeight = FontWeight.Bold)
                    }
                }

                // Recent preview of movements
                if (recentRecords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("آخر الحركات المسجلة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    recentRecords.take(3).forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(r.title, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(
                                "${if (r.isPayment) "له" else "عليه"}: ${String.format(Locale.US, "%,.0f", r.amount)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (r.isPayment) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Edit Transaction / Invoice Dialog
 */
@Composable
fun EditTransactionDialog(
    record: StatementRecord,
    onDismiss: () -> Unit,
    onSavePayment: (newAmount: Double, notes: String) -> Unit,
    onSaveInvoice: (newTotal: Double, newPaid: Double, desc: String) -> Unit
) {
    var amountText by remember { mutableStateOf(record.amount.toString()) }
    var notesText by remember { mutableStateOf(record.title) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تعديل العملية #${record.opNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("البيان / الوصف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: record.amount
                            if (record.isPayment) {
                                onSavePayment(amt, notesText)
                            } else {
                                onSaveInvoice(amt, 0.0, notesText)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ التعديل")
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet / Dialog for Exporting Full Statement: WhatsApp, PDF, Excel, Image
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStatementSheet(
    customer: Customer,
    records: List<StatementRecord>,
    storeInfo: StoreInfo,
    totalDebit: Double,
    totalCredit: Double,
    finalBalance: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val exportRows = remember(records) {
        records.map { r ->
            ExportRow(
                operationNum = r.opNumber,
                date = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(r.timestamp)),
                type = if (r.isPayment) "له" else "عليه",
                details = r.title,
                amount = r.amount,
                balance = r.cumulativeBalance
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "خيارات مشاركة وتصدير كشف الحساب",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "العميل: ${customer.name} • الرصيد: ${String.format(Locale.US, "%,.0f", finalBalance)} ${storeInfo.currency}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // 1. WhatsApp Text
            FilledTonalButton(
                onClick = {
                    FileExportHelper.shareFullStatementViaWhatsApp(
                        context = context,
                        customer = customer,
                        storeInfo = storeInfo,
                        rows = exportRows,
                        totalDebit = totalDebit,
                        totalCredit = totalCredit,
                        finalBalance = finalBalance
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFDCFCE7), contentColor = Color(0xFF16A34A))
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشاركة نص كشف الحساب عبر الواتساب", fontWeight = FontWeight.Bold)
            }

            // 2. Export PDF
            FilledTonalButton(
                onClick = {
                    val pdfFile = FileExportHelper.createCustomerStatementPdf(
                        context = context,
                        customer = customer,
                        storeInfo = storeInfo,
                        rows = exportRows,
                        totalDebit = totalDebit,
                        totalCredit = totalCredit,
                        finalBalance = finalBalance
                    )
                    if (pdfFile != null) {
                        FileExportHelper.shareFile(context, pdfFile, "application/pdf", "كشف حساب ${customer.name}.pdf")
                    } else {
                        Toast.makeText(context, "تعذر إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626))
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصدير ومشاركة كملف PDF", fontWeight = FontWeight.Bold)
            }

            // 3. Export Excel CSV
            FilledTonalButton(
                onClick = {
                    val csvFile = FileExportHelper.createCustomerStatementExcelCsv(
                        context = context,
                        customer = customer,
                        storeInfo = storeInfo,
                        rows = exportRows,
                        totalDebit = totalDebit,
                        totalCredit = totalCredit,
                        finalBalance = finalBalance
                    )
                    if (csvFile != null) {
                        FileExportHelper.shareFile(context, csvFile, "text/csv", "كشف حساب ${customer.name}.csv")
                    } else {
                        Toast.makeText(context, "تعذر إنشاء ملف Excel", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE0E7FF), contentColor = Color(0xFF4338CA))
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصدير كملف إكسل Excel (.csv)", fontWeight = FontWeight.Bold)
            }

            // 4. Export as Image
            FilledTonalButton(
                onClick = {
                    val imgFile = FileExportHelper.createStatementImage(
                        context = context,
                        customerName = customer.name,
                        storeInfo = storeInfo,
                        rows = exportRows,
                        totalDebit = totalDebit,
                        totalCredit = totalCredit,
                        finalBalance = finalBalance
                    )
                    if (imgFile != null) {
                        FileExportHelper.shareFile(context, imgFile, "image/jpeg", "كشف حساب ${customer.name}.jpg")
                    } else {
                        Toast.makeText(context, "تعذر إنشاء الصورة", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشاركة كشف الحساب كصورة", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
