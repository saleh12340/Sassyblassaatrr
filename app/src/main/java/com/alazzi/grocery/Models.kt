package com.alazzi.grocery

const val APP_CURRENCY = "ر.ي"

enum class ProductCategory(val titleAr: String) {
    DAIRY("الألبان والأجبان"),
    BEVERAGES("المشروبات والعصائر"),
    GRAINS("الحبوب والبقوليات"),
    CANNED("المعلبات والزيوت"),
    BAKERY("المخبوزات والحلويات"),
    CLEANING("المنظفات والمنزل"),
    OTHER("سلع متنوعة")
}

data class Product(
    val id: Long = 0,
    val name: String,
    val category: ProductCategory,
    val barcode: String = "",
    val costPrice: Double,
    val sellPrice: Double,
    val stockQty: Double,
    val unit: String = "حبة"
)

data class CartItem(
    val product: Product,
    var quantity: Double
) {
    val total: Double get() = product.sellPrice * quantity
}

data class InvoiceRowDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var price: Double = 0.0,
    var quantity: Double = 1.0
) {
    val subtotal: Double get() = price * quantity
}

data class Customer(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val balanceDebt: Double = 0.0,
    val notes: String = ""
)

enum class PaymentType(val titleAr: String) {
    CASH("نقداً"),
    CREDIT("آجل (دين)")
}

data class Invoice(
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long,
    val customerId: Long? = null,
    val customerName: String? = null,
    val paymentType: PaymentType,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingDebt: Double,
    val items: List<InvoiceItem> = emptyList()
)

data class InvoiceItem(
    val id: Long = 0,
    val invoiceId: Long = 0,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val quantity: Double,
    val subtotal: Double
)

data class DebtPayment(
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val timestamp: Long,
    val notes: String = ""
)

data class StoreInfo(
    val name: String = "بقالة العزي للمواد الغذائية",
    val ownerName: String = "العزي",
    val phone: String = "771234567",
    val activity: String = "مواد غذائية، معلبات، مشروبات وتموينات",
    val address: String = "الشارع العام",
    val country: String = "اليمن",
    val currency: String = "ريال يمني (ر.ي)",
    val invoiceFooter: String = "شكراً لزيارتكم ونسعد بخدمتكم دائماً"
)

data class DatabaseImportSummary(
    val productsCount: Int,
    val customersCount: Int,
    val invoicesCount: Int,
    val paymentsCount: Int,
    val dbSizeBytes: Long
)

