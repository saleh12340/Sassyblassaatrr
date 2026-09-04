package com.alazzi.grocery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PosUiState(
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val debtPayments: List<DebtPayment> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val selectedCustomer: Customer? = null,
    val selectedCategory: ProductCategory? = null,
    val searchQuery: String = "",
    val isFreeInvoiceMode: Boolean = true,
    val freeInvoiceDesc: String = "",
    val freeInvoiceAmount: String = "",
    val paymentType: PaymentType = PaymentType.CASH,
    val paidAmountText: String = "",
    val currentReceiptInvoice: Invoice? = null,
    val isShowingReceiptDialog: Boolean = false,
    val isShowingCartSheet: Boolean = false,
    val isShowingAddProductDialog: Boolean = false,
    val editingProduct: Product? = null,
    val isShowingAddCustomerDialog: Boolean = false,
    val editingCustomer: Customer? = null,
    val debtPaymentCustomer: Customer? = null,
    val toastMessage: String? = null,
    val storeInfo: StoreInfo = StoreInfo(),
    val isDatabaseLoading: Boolean = false,
    val databaseStats: DatabaseImportSummary? = null,
    val isShowingEditStoreDialog: Boolean = false,
    val isShowingDbManagementDialog: Boolean = false,
    val isShowingCustomerStatementDialog: Boolean = false,
    val statementCustomer: Customer? = null,
    val statementInvoices: List<Invoice> = emptyList(),
    val statementPayments: List<DebtPayment> = emptyList(),
    val draftTableRows: List<InvoiceRowDraft> = emptyList()
)

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = GroceryDbHelper(application)
    private val storePrefs = StorePreferences(application)

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        val initialStoreInfo = storePrefs.getStoreInfo()
        _uiState.value = _uiState.value.copy(storeInfo = initialStoreInfo)
        loadAllData()
        loadDatabaseStats()
    }

    fun loadAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val products = dbHelper.getAllProducts()
            val customers = dbHelper.getAllCustomers()
            val invoices = dbHelper.getAllInvoices()
            val payments = dbHelper.getPayments()

            _uiState.value = _uiState.value.copy(
                products = products,
                customers = customers,
                invoices = invoices,
                debtPayments = payments
            )
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    // POS & Cart Operations
    fun addToCart(product: Product) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val item = currentCart[index]
            currentCart[index] = item.copy(quantity = item.quantity + 1.0)
        } else {
            currentCart.add(CartItem(product = product, quantity = 1.0))
        }
        _uiState.value = _uiState.value.copy(cartItems = currentCart)
    }

    fun updateCartItemQuantity(productId: Long, delta: Double) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentCart[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentCart.removeAt(index)
            } else {
                currentCart[index] = item.copy(quantity = newQty)
            }
            _uiState.value = _uiState.value.copy(cartItems = currentCart)
        }
    }

    fun removeFromCart(productId: Long) {
        val currentCart = _uiState.value.cartItems.filterNot { it.product.id == productId }
        _uiState.value = _uiState.value.copy(cartItems = currentCart)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cartItems = emptyList())
    }

    fun setFreeInvoiceMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isFreeInvoiceMode = enabled)
    }

    fun setFreeInvoiceDesc(desc: String) {
        _uiState.value = _uiState.value.copy(freeInvoiceDesc = desc)
    }

    fun setFreeInvoiceAmount(amount: String) {
        _uiState.value = _uiState.value.copy(freeInvoiceAmount = amount)
    }

    fun setSelectedCustomer(customer: Customer?) {
        _uiState.value = _uiState.value.copy(selectedCustomer = customer)
    }

    fun setSelectedCategory(category: ProductCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setPaymentType(paymentType: PaymentType) {
        _uiState.value = _uiState.value.copy(paymentType = paymentType)
    }

    fun setPaidAmountText(text: String) {
        _uiState.value = _uiState.value.copy(paidAmountText = text)
    }

    fun showCartSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingCartSheet = show)
    }

    fun showReceiptDialog(invoice: Invoice?) {
        _uiState.value = _uiState.value.copy(
            currentReceiptInvoice = invoice,
            isShowingReceiptDialog = invoice != null
        )
    }

    // Checkout & Invoice Generation
    fun completeCheckout(isPartialPaid: Boolean = false, partialPaidAmount: Double = 0.0) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            var createdInvoiceId: Long = -1

            if (state.isFreeInvoiceMode) {
                val total = state.freeInvoiceAmount.toDoubleOrNull() ?: 0.0
                if (total <= 0) {
                    _uiState.value = _uiState.value.copy(toastMessage = "يرجى إدخال مبلغ صحيح للفاتورة")
                    return@launch
                }

                val paid = when {
                    state.paymentType == PaymentType.CASH -> total
                    isPartialPaid -> partialPaidAmount
                    else -> 0.0
                }

                createdInvoiceId = dbHelper.createFreeSaleInvoice(
                    description = state.freeInvoiceDesc.ifBlank { "مبيعات حرة" },
                    totalAmount = total,
                    paymentType = state.paymentType,
                    customerId = state.selectedCustomer?.id,
                    customerName = state.selectedCustomer?.name,
                    paidAmount = paid
                )
            } else {
                if (state.cartItems.isEmpty()) {
                    _uiState.value = _uiState.value.copy(toastMessage = "السلة فارغة، يرجى إضافة منتجات")
                    return@launch
                }

                val total = state.cartItems.sumOf { it.total }
                val paid = when {
                    state.paymentType == PaymentType.CASH -> total
                    isPartialPaid -> partialPaidAmount
                    else -> 0.0
                }

                createdInvoiceId = dbHelper.createSaleInvoice(
                    cartItems = state.cartItems,
                    paymentType = state.paymentType,
                    customerId = state.selectedCustomer?.id,
                    customerName = state.selectedCustomer?.name,
                    paidAmount = paid
                )
            }

            if (createdInvoiceId != -1L) {
                val updatedInvoices = dbHelper.getAllInvoices()
                val updatedCustomers = dbHelper.getAllCustomers()
                val updatedProducts = dbHelper.getAllProducts()
                val newInvoice = updatedInvoices.firstOrNull { it.id == createdInvoiceId }

                _uiState.value = _uiState.value.copy(
                    invoices = updatedInvoices,
                    customers = updatedCustomers,
                    products = updatedProducts,
                    cartItems = emptyList(),
                    freeInvoiceDesc = "",
                    freeInvoiceAmount = "",
                    paidAmountText = "",
                    selectedCustomer = null,
                    isShowingCartSheet = false,
                    currentReceiptInvoice = newInvoice,
                    isShowingReceiptDialog = true,
                    toastMessage = "تم إصدار الفاتورة بنجاح"
                )
            } else {
                _uiState.value = _uiState.value.copy(toastMessage = "حدث خطأ أثناء حفظ الفاتورة")
            }
        }
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteInvoice(invoiceId)
            loadAllData()
            _uiState.value = _uiState.value.copy(
                isShowingReceiptDialog = false,
                currentReceiptInvoice = null,
                toastMessage = "تم حذف الفاتورة"
            )
        }
    }

    // Customer Operations
    fun showAddCustomerDialog(show: Boolean, customer: Customer? = null) {
        _uiState.value = _uiState.value.copy(
            isShowingAddCustomerDialog = show,
            editingCustomer = customer
        )
    }

    fun saveCustomer(name: String, phone: String, balanceDebt: Double, notes: String) {
        val editing = _uiState.value.editingCustomer
        viewModelScope.launch(Dispatchers.IO) {
            if (editing != null) {
                val updated = editing.copy(
                    name = name,
                    phone = phone,
                    balanceDebt = balanceDebt,
                    notes = notes
                )
                dbHelper.updateCustomer(updated)
            } else {
                val newCustomer = Customer(
                    name = name,
                    phone = phone,
                    balanceDebt = balanceDebt,
                    notes = notes
                )
                dbHelper.insertCustomer(newCustomer)
            }
            loadAllData()
            _uiState.value = _uiState.value.copy(
                isShowingAddCustomerDialog = false,
                editingCustomer = null,
                toastMessage = "تم حفظ بيانات العميل بنجاح"
            )
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteCustomer(customerId)
            loadAllData()
            _uiState.value = _uiState.value.copy(toastMessage = "تم حذف العميل")
        }
    }

    fun showDebtPaymentDialog(customer: Customer?) {
        _uiState.value = _uiState.value.copy(debtPaymentCustomer = customer)
    }

    fun recordCustomerDebtPayment(customerId: Long, customerName: String, amount: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.recordDebtPayment(customerId, customerName, amount, notes)
            loadAllData()
            _uiState.value = _uiState.value.copy(
                debtPaymentCustomer = null,
                toastMessage = "تم تسجيل سداد بقيمة $amount ر.ي بنجاح"
            )
        }
    }

    // Product & Inventory Operations
    fun showAddProductDialog(show: Boolean, product: Product? = null) {
        _uiState.value = _uiState.value.copy(
            isShowingAddProductDialog = show,
            editingProduct = product
        )
    }

    fun saveProduct(
        name: String,
        category: ProductCategory,
        barcode: String,
        costPrice: Double,
        sellPrice: Double,
        stockQty: Double,
        unit: String
    ) {
        val editing = _uiState.value.editingProduct
        viewModelScope.launch(Dispatchers.IO) {
            if (editing != null) {
                val updated = editing.copy(
                    name = name,
                    category = category,
                    barcode = barcode,
                    costPrice = costPrice,
                    sellPrice = sellPrice,
                    stockQty = stockQty,
                    unit = unit
                )
                dbHelper.updateProduct(updated)
            } else {
                val newProd = Product(
                    name = name,
                    category = category,
                    barcode = barcode,
                    costPrice = costPrice,
                    sellPrice = sellPrice,
                    stockQty = stockQty,
                    unit = unit
                )
                dbHelper.insertProduct(newProd)
            }
            loadAllData()
            _uiState.value = _uiState.value.copy(
                isShowingAddProductDialog = false,
                editingProduct = null,
                toastMessage = "تم حفظ الصنف بنجاح"
            )
        }
    }

    fun updateProductStock(productId: Long, delta: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateProductStock(productId, delta)
            loadAllData()
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteProduct(productId)
            loadAllData()
            _uiState.value = _uiState.value.copy(toastMessage = "تم حذف الصنف من المخزون")
        }
    }

    // Store Settings Management
    fun updateStoreInfo(newInfo: StoreInfo) {
        storePrefs.saveStoreInfo(newInfo)
        _uiState.value = _uiState.value.copy(
            storeInfo = newInfo,
            isShowingEditStoreDialog = false,
            toastMessage = "تم حفظ بيانات المتجر بنجاح"
        )
    }

    fun showEditStoreDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingEditStoreDialog = show)
    }

    // Database Management (Backup & Import)
    fun showDbManagementDialog(show: Boolean) {
        if (show) {
            loadDatabaseStats()
        }
        _uiState.value = _uiState.value.copy(isShowingDbManagementDialog = show)
    }

    fun loadDatabaseStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = dbHelper.getDatabaseStats(getApplication())
            _uiState.value = _uiState.value.copy(databaseStats = stats)
        }
    }

    fun importDatabaseFromUri(uri: Uri, onResult: (Boolean, String) -> Unit) {
        _uiState.value = _uiState.value.copy(isDatabaseLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = dbHelper.importDatabase(getApplication(), uri)
            result.fold(
                onSuccess = { summary ->
                    loadAllData()
                    loadDatabaseStats()
                    _uiState.value = _uiState.value.copy(
                        isDatabaseLoading = false,
                        isShowingDbManagementDialog = false,
                        toastMessage = "تم استيراد قاعدة البيانات بنجاح: ${summary.productsCount} صنف، ${summary.customersCount} عميل"
                    )
                    onResult(true, "تم استيراد البيانات بنجاح: ${summary.productsCount} أصناف، ${summary.customersCount} عملاء، ${summary.invoicesCount} فاتورة")
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isDatabaseLoading = false,
                        toastMessage = "فشل الاستيراد: ${err.localizedMessage}"
                    )
                    onResult(false, err.localizedMessage ?: "حدث خطأ غير متوقع أثناء الاستيراد")
                }
            )
        }
    }

    fun exportDatabaseToUri(destinationUri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = dbHelper.exportDatabase(getApplication(), destinationUri)
            result.fold(
                onSuccess = { bytes ->
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "تم تصدير النسخة الاحتياطية بنجاح (${bytes / 1024} KB)"
                    )
                    onResult(true, "تم تصدير قاعدة البيانات وحفظها بنجاح")
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "فشل التصدير: ${err.localizedMessage}"
                    )
                    onResult(false, err.localizedMessage ?: "حدث خطأ أثناء تصدير قاعدة البيانات")
                }
            )
        }
    }

    fun shareDatabaseBackup(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = dbHelper.createBackupFileForSharing(context)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.alazzi.grocery.fileprovider",
                    backupFile
                )
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية لقاعدة بيانات ${uiState.value.storeInfo.name}")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = android.content.Intent.createChooser(shareIntent, "مشاركة نسخة احتياطية للقاعدة").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "تعذر مشاركة الملف: ${e.localizedMessage}")
            }
        }
    }

    // Customer Account Statement
    fun showCustomerStatement(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            val freshCustomer = dbHelper.getAllCustomers().find { it.id == customer.id } ?: customer
            val invoices = dbHelper.getCustomerInvoices(customer.id)
            val payments = dbHelper.getCustomerPayments(customer.id)
            _uiState.value = _uiState.value.copy(
                statementCustomer = freshCustomer,
                statementInvoices = invoices,
                statementPayments = payments,
                isShowingCustomerStatementDialog = true
            )
        }
    }

    fun refreshCustomerStatement(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val freshCustomer = dbHelper.getAllCustomers().find { it.id == customerId }
            if (freshCustomer != null) {
                val invoices = dbHelper.getCustomerInvoices(customerId)
                val payments = dbHelper.getCustomerPayments(customerId)
                _uiState.value = _uiState.value.copy(
                    statementCustomer = freshCustomer,
                    statementInvoices = invoices,
                    statementPayments = payments
                )
            }
        }
    }

    fun closeCustomerStatement() {
        _uiState.value = _uiState.value.copy(
            isShowingCustomerStatementDialog = false,
            statementCustomer = null,
            statementInvoices = emptyList(),
            statementPayments = emptyList()
        )
    }

    // Direct movement matching "له" and "عليه"
    fun addDirectMovement(
        customerId: Long,
        customerName: String,
        isForCustomer: Boolean,
        amount: Double,
        details: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.addDirectCustomerMovement(customerId, customerName, isForCustomer, amount, details, timestamp)
            loadAllData()
            refreshCustomerStatement(customerId)
            val typeStr = if (isForCustomer) "له (سداد)" else "عليه (دين)"
            _uiState.value = _uiState.value.copy(toastMessage = "تم تسجيل حركة $typeStr بمبلغ $amount ر.ي")
        }
    }

    fun updatePaymentRecord(paymentId: Long, customerId: Long, newAmount: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateDebtPayment(paymentId, newAmount, notes)
            loadAllData()
            refreshCustomerStatement(customerId)
            _uiState.value = _uiState.value.copy(toastMessage = "تم تعديل السداد بنجاح")
        }
    }

    fun updateInvoiceRecord(invoiceId: Long, customerId: Long, newTotal: Double, newPaid: Double, desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateInvoiceRemaining(invoiceId, newTotal, newPaid, desc)
            loadAllData()
            refreshCustomerStatement(customerId)
            _uiState.value = _uiState.value.copy(toastMessage = "تم تعديل الفاتورة بنجاح")
        }
    }

    // Table-based Invoice Operations (Prevent Duplicates, Auto-merge, Small Font POS Table)
    fun addDraftTableRow(name: String, price: Double, quantity: Double) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "يرجى إدخال اسم الصنف")
            return
        }
        val currentRows = _uiState.value.draftTableRows.toMutableList()
        val existingIndex = currentRows.indexOfFirst { it.name.trim().equals(cleanName, ignoreCase = true) }
        if (existingIndex >= 0) {
            val existing = currentRows[existingIndex]
            val newQty = existing.quantity + quantity
            currentRows[existingIndex] = existing.copy(quantity = newQty)
            _uiState.value = _uiState.value.copy(
                draftTableRows = currentRows,
                toastMessage = "تمت زيادة كمية $cleanName في الفاتورة إلى $newQty"
            )
        } else {
            currentRows.add(InvoiceRowDraft(name = cleanName, price = price, quantity = quantity))
            _uiState.value = _uiState.value.copy(
                draftTableRows = currentRows,
                toastMessage = "تمت إضافة $cleanName إلى جدول الفاتورة"
            )
        }
    }

    fun addProductToDraftTable(product: Product, quantity: Double = 1.0) {
        addDraftTableRow(product.name, product.sellPrice, quantity)
    }

    fun updateDraftTableRowQty(id: String, delta: Double) {
        val currentRows = _uiState.value.draftTableRows.toMutableList()
        val index = currentRows.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = currentRows[index]
            val newQty = existing.quantity + delta
            if (newQty <= 0) {
                currentRows.removeAt(index)
            } else {
                currentRows[index] = existing.copy(quantity = newQty)
            }
            _uiState.value = _uiState.value.copy(draftTableRows = currentRows)
        }
    }

    fun removeDraftTableRow(id: String) {
        val currentRows = _uiState.value.draftTableRows.filterNot { it.id == id }
        _uiState.value = _uiState.value.copy(draftTableRows = currentRows)
    }

    fun clearDraftTable() {
        _uiState.value = _uiState.value.copy(draftTableRows = emptyList())
    }

    // Save Table-based Invoice (Image 3)
    fun saveTableInvoice(
        rows: List<InvoiceRowDraft>,
        paymentType: PaymentType,
        customerId: Long?,
        customerName: String?,
        paidAmount: Double
    ) {
        if (rows.isEmpty()) {
            _uiState.value = _uiState.value.copy(toastMessage = "الفاتورة فارغة، أضف أصنافاً أولاً")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val invoiceId = dbHelper.createTableInvoice(rows, paymentType, customerId, customerName, paidAmount)
            if (invoiceId != -1L) {
                loadAllData()
                val newInv = dbHelper.getAllInvoices().firstOrNull { it.id == invoiceId }
                _uiState.value = _uiState.value.copy(
                    draftTableRows = emptyList(),
                    currentReceiptInvoice = newInv,
                    isShowingReceiptDialog = true,
                    toastMessage = "تم حفظ الفاتورة بنجاح وتحديث المخزن"
                )
            } else {
                _uiState.value = _uiState.value.copy(toastMessage = "حدث خطأ أثناء حفظ الفاتورة")
            }
        }
    }
}
