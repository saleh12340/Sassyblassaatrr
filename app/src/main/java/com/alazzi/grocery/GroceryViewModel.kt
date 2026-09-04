package com.alazzi.grocery

import android.app.Application
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
    val isFreeInvoiceMode: Boolean = false,
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
    val toastMessage: String? = null
)

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = GroceryDbHelper(application)

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
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
}
