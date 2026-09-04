package com.alazzi.grocery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val totalSales = remember(state.invoices) {
        state.invoices.sumOf { it.totalAmount }
    }
    val totalCashCollected = remember(state.invoices, state.debtPayments) {
        val invoicePaid = state.invoices.sumOf { it.paidAmount }
        val debtPaid = state.debtPayments.sumOf { it.amount }
        invoicePaid + debtPaid
    }
    val totalCustomerDebt = remember(state.customers) {
        state.customers.sumOf { it.balanceDebt }
    }

    val filteredInvoices = remember(state.invoices, searchQuery) {
        state.invoices.filter { inv ->
            searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    (inv.customerName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Store and Database Quick Management Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.showEditStoreDialog(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بيانات المتجر", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = { viewModel.showDbManagementDialog(true) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد ونسخ البيانات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Financial Dashboard KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إجمالي المبيعات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalSales)} ${state.storeInfo.currency}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("النقدية المحصلة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalCashCollected)} ${state.storeInfo.currency}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DebtRedContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إجمالي الديون المعلقة", style = MaterialTheme.typography.bodySmall, color = OnDebtRed)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalCustomerDebt)} ${state.storeInfo.currency}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DebtRed
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("عدد الفواتير", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${state.invoices.size} فاتورة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: Invoices vs Debt Payments
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("سجل الفواتير (${state.invoices.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سدادات الديون (${state.debtPayments.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Payments, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Invoices Ledger
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برقم الفاتورة أو اسم العميل...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فواتير مسجلة", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            currency = state.storeInfo.currency,
                            onClick = { viewModel.showReceiptDialog(invoice) },
                            onDelete = { viewModel.deleteInvoice(invoice.id) }
                        )
                    }
                }
            }
        } else {
            // Debt Payments Ledger
            if (state.debtPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد عمليات سداد مسجلة بعد", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(state.debtPayments, key = { it.id }) { payment ->
                        DebtPaymentItemCard(payment = payment, currency = state.storeInfo.currency)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItemCard(
    invoice: Invoice,
    currency: String = "ر.ي",
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (invoice.paymentType == PaymentType.CASH) MaterialTheme.colorScheme.primaryContainer else DebtRedContainer
                ) {
                    Text(
                        text = invoice.paymentType.titleAr,
                        color = if (invoice.paymentType == PaymentType.CASH) MaterialTheme.colorScheme.primary else DebtRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "العميل: ${invoice.customerName ?: "عميل نقدي عام"} • ${ThermalReceiptHelper.formatDate(invoice.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإجمالي: ${String.format(Locale.US, "%.2f", invoice.totalAmount)} $currency",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (invoice.remainingDebt > 0) {
                        Text(
                            text = "المتبقي دين: ${String.format(Locale.US, "%.2f", invoice.remainingDebt)} $currency",
                            style = MaterialTheme.typography.bodySmall,
                            color = DebtRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row {
                    FilledTonalButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("عرض وطباعة", fontSize = 12.sp)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = DebtRed)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد حذف الفاتورة") },
            text = { Text("هل أنت متأكد من حذف الفاتورة ${invoice.invoiceNumber}؟ سيتم التراجع عن الديون المرتبطة بها.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun DebtPaymentItemCard(
    payment: DebtPayment,
    currency: String = "ر.ي"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(payment.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${ThermalReceiptHelper.formatDate(payment.timestamp)} • ${payment.notes.ifBlank { "سداد نقدي" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "+ ${String.format(Locale.US, "%.2f", payment.amount)} $currency",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
