package com.alazzi.grocery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: GroceryViewModel,
    onOpenStoreInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val filteredProducts = remember(state.products, state.selectedCategory, state.searchQuery) {
        state.products.filter { p ->
            val matchCat = state.selectedCategory == null || p.category == state.selectedCategory
            val matchQuery = state.searchQuery.isBlank() ||
                    p.name.contains(state.searchQuery, ignoreCase = true) ||
                    p.barcode.contains(state.searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    val cartTotal = remember(state.cartItems) {
        state.cartItems.sumOf { it.total }
    }
    val cartCount = remember(state.cartItems) {
        state.cartItems.sumOf { it.quantity }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Store Banner Card with Owner Image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenStoreInfo() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_splash_bg),
                            contentDescription = "صورة بقالة العزي",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "بقالة العزي للمواد الغذائية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "خدمة ممتازة • اضغط لعرض بطاقة المحل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onOpenStoreInfo,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = "بطاقة المحل",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Toggle: Detailed vs Free Invoice
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = !state.isFreeInvoiceMode,
                    onClick = { viewModel.setFreeInvoiceMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("فاتورة أصناف (مخزون)", fontWeight = FontWeight.Bold)
                }

                SegmentedButton(
                    selected = state.isFreeInvoiceMode,
                    onClick = { viewModel.setFreeInvoiceMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("فاتورة حرة سريعة", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isFreeInvoiceMode) {
                // Free Invoice View
                FreeInvoiceCard(viewModel = viewModel, state = state)
            } else {
                // Detailed Invoice with Catalog
                // Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("ابحث عن صنف بالاسم أو الباركود...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) },
                            label = { Text("الكل") },
                            leadingIcon = if (state.selectedCategory == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    items(ProductCategory.values()) { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(if (state.selectedCategory == cat) null else cat) },
                            label = { Text(cat.titleAr) },
                            leadingIcon = if (state.selectedCategory == cat) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Products List
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد أصناف مطابقة للبحث", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            val inCart = state.cartItems.find { it.product.id == product.id }
                            ProductSaleCard(
                                product = product,
                                inCartQuantity = inCart?.quantity ?: 0.0,
                                onAddToCart = { viewModel.addToCart(product) },
                                onIncrement = { viewModel.updateCartItemQuantity(product.id, 1.0) },
                                onDecrement = { viewModel.updateCartItemQuantity(product.id, -1.0) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Docked Cart Bar for Detailed Mode
        if (!state.isFreeInvoiceMode && state.cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "%.0f".format(cartCount),
                                modifier = Modifier.padding(4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "المجموع المطلوب",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", cartTotal)} ر.ي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.showCartSheet(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("متابعة الفاتورة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Cart and Checkout Bottom Sheet
    if (state.isShowingCartSheet) {
        CheckoutBottomSheet(viewModel = viewModel, state = state, cartTotal = cartTotal)
    }
}

@Composable
fun ProductSaleCard(
    product: Product,
    inCartQuantity: Double,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", product.sellPrice)} ر.ي / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• المتبقي: ${product.stockQty} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stockQty <= 5) DebtRed else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (inCartQuantity > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "تقليل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f", inCartQuantity),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "زيادة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onAddToCart,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة")
                }
            }
        }
    }
}

@Composable
fun FreeInvoiceCard(viewModel: GroceryViewModel, state: PosUiState) {
    var showCustomerPicker by remember { mutableStateOf(false) }
    var isPartialPaid by remember { mutableStateOf(false) }
    var partialAmountText by remember { mutableStateOf("") }

    val totalAmount = state.freeInvoiceAmount.toDoubleOrNull() ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = AmberSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "إصدار فاتورة حرة سريعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "مناسبة للبيع السريع بمبلغ مباشر دون اختيار أصناف من المخزن",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.freeInvoiceDesc,
                onValueChange = { viewModel.setFreeInvoiceDesc(it) },
                label = { Text("بيان / وصف الفاتورة") },
                placeholder = { Text("مثال: خضار ومواد تموينية متنوعة") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.freeInvoiceAmount,
                onValueChange = { viewModel.setFreeInvoiceAmount(it) },
                label = { Text("المبلغ الإجمالي (ر.ي) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Selector
            Text("العميل:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedCard(
                onClick = { showCustomerPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.selectedCustomer?.name ?: "عميل نقدي عام (افتراضي)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("تغيير", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Customer Debt Alert if Indebted
            if (state.selectedCustomer != null && state.selectedCustomer!!.balanceDebt > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DebtRedContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DebtRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنبيه: على هذا العميل دين سابق بقيمة ${String.format(Locale.US, "%.2f", state.selectedCustomer!!.balanceDebt)} ر.ي",
                            color = OnDebtRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Type Selector
            Text("طريقة السداد:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.paymentType == PaymentType.CASH && !isPartialPaid,
                    onClick = {
                        viewModel.setPaymentType(PaymentType.CASH)
                        isPartialPaid = false
                    },
                    label = { Text("نقداً (كاش)") },
                    leadingIcon = { Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = state.paymentType == PaymentType.CREDIT && !isPartialPaid,
                    onClick = {
                        if (state.selectedCustomer == null) {
                            showCustomerPicker = true
                        } else {
                            viewModel.setPaymentType(PaymentType.CREDIT)
                            isPartialPaid = false
                        }
                    },
                    label = { Text("آجل (دين)") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isPartialPaid,
                    onClick = {
                        if (state.selectedCustomer == null) {
                            showCustomerPicker = true
                        } else {
                            viewModel.setPaymentType(PaymentType.CREDIT)
                            isPartialPaid = true
                        }
                    },
                    label = { Text("دفعة + متبقي") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isPartialPaid) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = partialAmountText,
                    onValueChange = { partialAmountText = it },
                    label = { Text("المبلغ المدفوع كاش الآن") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                val paid = partialAmountText.toDoubleOrNull() ?: 0.0
                val remaining = maxOf(0.0, totalAmount - paid)
                Text(
                    text = "المتبقي كدين على العميل: ${String.format(Locale.US, "%.2f", remaining)} ر.ي",
                    color = DebtRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val partialVal = partialAmountText.toDoubleOrNull() ?: 0.0
                    viewModel.completeCheckout(isPartialPaid = isPartialPaid, partialPaidAmount = partialVal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = totalAmount > 0
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إصدار وطباعة الفاتورة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            selected = state.selectedCustomer,
            onSelect = {
                viewModel.setSelectedCustomer(it)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutBottomSheet(
    viewModel: GroceryViewModel,
    state: PosUiState,
    cartTotal: Double
) {
    var showCustomerPicker by remember { mutableStateOf(false) }
    var isPartialPaid by remember { mutableStateOf(false) }
    var partialAmountText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { viewModel.showCartSheet(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سلة المشتريات وتأكيد الفاتورة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.clearCart() }) {
                    Text("إفراغ السلة", color = DebtRed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.cartItems, key = { it.product.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${item.quantity} × ${String.format(Locale.US, "%.2f", item.product.sellPrice)} = ${String.format(Locale.US, "%.2f", item.total)} ر.ي",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.updateCartItemQuantity(item.product.id, -1.0) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "نقص")
                                }
                                Text("${item.quantity.toInt()}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.updateCartItemQuantity(item.product.id, 1.0) }) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "زيادة")
                                }
                                IconButton(onClick = { viewModel.removeFromCart(item.product.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = DebtRed)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Selector
            OutlinedCard(
                onClick = { showCustomerPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("العميل المحدد:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            state.selectedCustomer?.name ?: "عميل نقدي عام",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("اختيار عميل", color = MaterialTheme.colorScheme.primary)
                }
            }

            if (state.selectedCustomer != null && state.selectedCustomer!!.balanceDebt > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ على هذا العميل دين سابق بقيمة: ${String.format(Locale.US, "%.2f", state.selectedCustomer!!.balanceDebt)} ر.ي",
                    color = DebtRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Type
            Text("طريقة السداد:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = state.paymentType == PaymentType.CASH && !isPartialPaid,
                    onClick = {
                        viewModel.setPaymentType(PaymentType.CASH)
                        isPartialPaid = false
                    },
                    label = { Text("كاش كامل") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.paymentType == PaymentType.CREDIT && !isPartialPaid,
                    onClick = {
                        if (state.selectedCustomer == null) {
                            showCustomerPicker = true
                        } else {
                            viewModel.setPaymentType(PaymentType.CREDIT)
                            isPartialPaid = false
                        }
                    },
                    label = { Text("آجل كامل") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isPartialPaid,
                    onClick = {
                        if (state.selectedCustomer == null) {
                            showCustomerPicker = true
                        } else {
                            viewModel.setPaymentType(PaymentType.CREDIT)
                            isPartialPaid = true
                        }
                    },
                    label = { Text("دفعة + متبقي") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isPartialPaid) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = partialAmountText,
                    onValueChange = { partialAmountText = it },
                    label = { Text("المبلغ المدفوع نقداً الآن") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Summary & Submit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المجموع النهائي:", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${String.format(Locale.US, "%.2f", cartTotal)} ر.ي",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val pVal = partialAmountText.toDoubleOrNull() ?: 0.0
                    viewModel.completeCheckout(isPartialPaid = isPartialPaid, partialPaidAmount = pVal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ وطباعة الفاتورة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            selected = state.selectedCustomer,
            onSelect = {
                viewModel.setSelectedCustomer(it)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    selected: Customer?,
    onSelect: (Customer?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(customers, query) {
        customers.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر العميل", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("بحث بالاسم أو الهاتف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option: Cash General Customer
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عميل نقدي عام (بدون تسجيل دين)", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { c ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(c) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected?.id == c.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(c.name, fontWeight = FontWeight.Bold)
                                    if (c.phone.isNotBlank()) {
                                        Text(c.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                if (c.balanceDebt > 0) {
                                    Surface(
                                        color = DebtRedContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "دين: ${String.format(Locale.US, "%.0f", c.balanceDebt)} ر.ي",
                                            color = OnDebtRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
