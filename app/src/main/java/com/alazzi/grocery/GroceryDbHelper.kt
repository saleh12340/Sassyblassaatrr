package com.alazzi.grocery

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class GroceryDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "alazzi_grocery.db"
        const val DATABASE_VERSION = 1

        const val TABLE_PRODUCTS = "products"
        const val TABLE_CUSTOMERS = "customers"
        const val TABLE_INVOICES = "invoices"
        const val TABLE_INVOICE_ITEMS = "invoice_items"
        const val TABLE_DEBT_PAYMENTS = "debt_payments"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PRODUCTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                barcode TEXT,
                cost_price REAL NOT NULL,
                sell_price REAL NOT NULL,
                stock_qty REAL NOT NULL,
                unit TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CUSTOMERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                balance_debt REAL NOT NULL DEFAULT 0.0,
                notes TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_INVOICES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_number TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                customer_id INTEGER,
                customer_name TEXT,
                payment_type TEXT NOT NULL,
                total_amount REAL NOT NULL,
                paid_amount REAL NOT NULL,
                remaining_debt REAL NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_INVOICE_ITEMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                unit_price REAL NOT NULL,
                quantity REAL NOT NULL,
                subtotal REAL NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DEBT_PAYMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                customer_name TEXT NOT NULL,
                amount REAL NOT NULL,
                timestamp INTEGER NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DEBT_PAYMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INVOICE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INVOICES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CUSTOMERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        val initialProducts = listOf(
            Product(name = "أرز الشعلان سيلا 5 كجم", category = ProductCategory.GRAINS, barcode = "6281001", costPrice = 38.0, sellPrice = 45.0, stockQty = 25.0, unit = "كيس"),
            Product(name = "حليب المراعي كامل الدسم 1 لتر", category = ProductCategory.DAIRY, barcode = "6281002", costPrice = 5.0, sellPrice = 6.5, stockQty = 60.0, unit = "حبة"),
            Product(name = "زيت عافية ذرة نقي 1.5 لتر", category = ProductCategory.CANNED, barcode = "6281003", costPrice = 19.5, sellPrice = 24.0, stockQty = 30.0, unit = "حبة"),
            Product(name = "سكر الأسرة ناعم 5 كجم", category = ProductCategory.GRAINS, barcode = "6281004", costPrice = 20.0, sellPrice = 25.0, stockQty = 40.0, unit = "كيس"),
            Product(name = "تونة ألوها لحم خفيف 185 جم", category = ProductCategory.CANNED, barcode = "6281005", costPrice = 5.5, sellPrice = 7.5, stockQty = 85.0, unit = "علبة"),
            Product(name = "طبق بيض طازج 30 حبة", category = ProductCategory.DAIRY, barcode = "6281006", costPrice = 14.0, sellPrice = 17.5, stockQty = 20.0, unit = "طبق"),
            Product(name = "شاي ليبتون العلامة الصفراء 100 كيس", category = ProductCategory.BEVERAGES, barcode = "6281007", costPrice = 13.0, sellPrice = 16.0, stockQty = 35.0, unit = "علبة"),
            Product(name = "جبنة كيري 24 حبة", category = ProductCategory.DAIRY, barcode = "6281008", costPrice = 16.0, sellPrice = 19.5, stockQty = 18.0, unit = "علبة"),
            Product(name = "مياه نقى كرتون 40 × 330 مل", category = ProductCategory.BEVERAGES, barcode = "6281009", costPrice = 12.0, sellPrice = 15.0, stockQty = 50.0, unit = "كرتون"),
            Product(name = "صابون فيري ليمون 1 لتر", category = ProductCategory.CLEANING, barcode = "6281010", costPrice = 9.5, sellPrice = 13.0, stockQty = 45.0, unit = "حبة"),
            Product(name = "مناديل فاين كلاسيك عبوة 10 علب", category = ProductCategory.CLEANING, barcode = "6281011", costPrice = 18.0, sellPrice = 23.0, stockQty = 30.0, unit = "شدة"),
            Product(name = "خبز توست أبيض لوزين", category = ProductCategory.BAKERY, barcode = "6281012", costPrice = 4.0, sellPrice = 5.5, stockQty = 15.0, unit = "كيس")
        )

        for (p in initialProducts) {
            val cv = ContentValues().apply {
                put("name", p.name)
                put("category", p.category.name)
                put("barcode", p.barcode)
                put("cost_price", p.costPrice)
                put("sell_price", p.sellPrice)
                put("stock_qty", p.stockQty)
                put("unit", p.unit)
            }
            db.insert(TABLE_PRODUCTS, null, cv)
        }

        val initialCustomers = listOf(
            Customer(name = "أبو محمد اليافعي", phone = "0501112233", balanceDebt = 145.0, notes = "حساب البيت الشهري"),
            Customer(name = "سالم باعباد", phone = "0554443322", balanceDebt = 80.0, notes = "يسدد كل جمعة"),
            Customer(name = "المهندس فهد العتيبي", phone = "0567778899", balanceDebt = 0.0, notes = "عميل نقدي دائم")
        )

        for (c in initialCustomers) {
            val cv = ContentValues().apply {
                put("name", c.name)
                put("phone", c.phone)
                put("balance_debt", c.balanceDebt)
                put("notes", c.notes)
            }
            db.insert(TABLE_CUSTOMERS, null, cv)
        }
    }

    // Product Queries
    fun getAllProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS ORDER BY name ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Product(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        category = try {
                            ProductCategory.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("category")))
                        } catch (_: Exception) {
                            ProductCategory.OTHER
                        },
                        barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode")) ?: "",
                        costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price")),
                        sellPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")),
                        stockQty = cursor.getDouble(cursor.getColumnIndexOrThrow("stock_qty")),
                        unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")) ?: "حبة"
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertProduct(product: Product): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", product.name)
            put("category", product.category.name)
            put("barcode", product.barcode)
            put("cost_price", product.costPrice)
            put("sell_price", product.sellPrice)
            put("stock_qty", product.stockQty)
            put("unit", product.unit)
        }
        return db.insert(TABLE_PRODUCTS, null, cv)
    }

    fun updateProduct(product: Product) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", product.name)
            put("category", product.category.name)
            put("barcode", product.barcode)
            put("cost_price", product.costPrice)
            put("sell_price", product.sellPrice)
            put("stock_qty", product.stockQty)
            put("unit", product.unit)
        }
        db.update(TABLE_PRODUCTS, cv, "id = ?", arrayOf(product.id.toString()))
    }

    fun updateProductStock(productId: Long, delta: Double) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_PRODUCTS SET stock_qty = MAX(0, stock_qty + ?) WHERE id = ?", arrayOf(delta, productId))
    }

    fun deleteProduct(productId: Long) {
        val db = writableDatabase
        db.delete(TABLE_PRODUCTS, "id = ?", arrayOf(productId.toString()))
    }

    // Customer Queries
    fun getAllCustomers(): List<Customer> {
        val list = mutableListOf<Customer>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CUSTOMERS ORDER BY balance_debt DESC, name ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Customer(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")) ?: "",
                        balanceDebt = cursor.getDouble(cursor.getColumnIndexOrThrow("balance_debt")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertCustomer(customer: Customer): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", customer.name)
            put("phone", customer.phone)
            put("balance_debt", customer.balanceDebt)
            put("notes", customer.notes)
        }
        return db.insert(TABLE_CUSTOMERS, null, cv)
    }

    fun recordDebtPayment(customerId: Long, customerName: String, amount: Double, notes: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Deduct debt
            db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = MAX(0, balance_debt - ?) WHERE id = ?", arrayOf(amount, customerId))

            // Record payment log
            val cv = ContentValues().apply {
                put("customer_id", customerId)
                put("customer_name", customerName)
                put("amount", amount)
                put("timestamp", System.currentTimeMillis())
                put("notes", notes)
            }
            db.insert(TABLE_DEBT_PAYMENTS, null, cv)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // Invoices
    fun createSaleInvoice(
        cartItems: List<CartItem>,
        paymentType: PaymentType,
        customerId: Long?,
        customerName: String?,
        paidAmount: Double
    ): Long {
        val db = writableDatabase
        var invoiceId: Long = -1
        db.beginTransaction()
        try {
            val totalAmount = cartItems.sumOf { it.total }
            val remainingDebt = if (paymentType == PaymentType.CREDIT) {
                totalAmount - paidAmount
            } else 0.0

            val invNum = "INV-" + (System.currentTimeMillis() % 100000)

            val cvInv = ContentValues().apply {
                put("invoice_number", invNum)
                put("timestamp", System.currentTimeMillis())
                put("customer_id", customerId)
                put("customer_name", customerName)
                put("payment_type", paymentType.name)
                put("total_amount", totalAmount)
                put("paid_amount", paidAmount)
                put("remaining_debt", remainingDebt)
            }
            invoiceId = db.insert(TABLE_INVOICES, null, cvInv)

            for (item in cartItems) {
                val cvItem = ContentValues().apply {
                    put("invoice_id", invoiceId)
                    put("product_id", item.product.id)
                    put("product_name", item.product.name)
                    put("unit_price", item.product.sellPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.total)
                }
                db.insert(TABLE_INVOICE_ITEMS, null, cvItem)

                // Deduct stock
                db.execSQL("UPDATE $TABLE_PRODUCTS SET stock_qty = MAX(0, stock_qty - ?) WHERE id = ?", arrayOf(item.quantity, item.product.id))
            }

            // Update customer debt if credit
            if (paymentType == PaymentType.CREDIT && customerId != null && remainingDebt > 0) {
                db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt + ? WHERE id = ?", arrayOf(remainingDebt, customerId))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return invoiceId
    }

    fun getAllInvoices(): List<Invoice> {
        val invoices = mutableListOf<Invoice>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_INVOICES ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val items = getInvoiceItems(id)
                invoices.add(
                    Invoice(
                        id = id,
                        invoiceNumber = cursor.getString(cursor.getColumnIndexOrThrow("invoice_number")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        customerId = if (cursor.isNull(cursor.getColumnIndexOrThrow("customer_id"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("customer_id")),
                        customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                        paymentType = try {
                            PaymentType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("payment_type")))
                        } catch (_: Exception) {
                            PaymentType.CASH
                        },
                        totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount")),
                        paidAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("paid_amount")),
                        remainingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow("remaining_debt")),
                        items = items
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return invoices
    }

    private fun getInvoiceItems(invoiceId: Long): List<InvoiceItem> {
        val items = mutableListOf<InvoiceItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_INVOICE_ITEMS WHERE invoice_id = ?", arrayOf(invoiceId.toString()))
        if (cursor.moveToFirst()) {
            do {
                items.add(
                    InvoiceItem(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        invoiceId = invoiceId,
                        productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id")),
                        productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                        unitPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("unit_price")),
                        quantity = cursor.getDouble(cursor.getColumnIndexOrThrow("quantity")),
                        subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return items
    }


    fun updateCustomer(customer: Customer) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", customer.name)
            put("phone", customer.phone)
            put("balance_debt", customer.balanceDebt)
            put("notes", customer.notes)
        }
        db.update(TABLE_CUSTOMERS, cv, "id = ?", arrayOf(customer.id.toString()))
    }

    fun deleteCustomer(customerId: Long) {
        val db = writableDatabase
        db.delete(TABLE_CUSTOMERS, "id = ?", arrayOf(customerId.toString()))
    }

    fun deleteInvoice(invoiceId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT customer_id, remaining_debt FROM $TABLE_INVOICES WHERE id = ?", arrayOf(invoiceId.toString()))
            if (cursor.moveToFirst()) {
                val custId = if (cursor.isNull(0)) null else cursor.getLong(0)
                val remDebt = cursor.getDouble(1)
                if (custId != null && remDebt > 0) {
                    db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = MAX(0, balance_debt - ?) WHERE id = ?", arrayOf(remDebt, custId))
                }
            }
            cursor.close()

            db.delete(TABLE_INVOICE_ITEMS, "invoice_id = ?", arrayOf(invoiceId.toString()))
            db.delete(TABLE_INVOICES, "id = ?", arrayOf(invoiceId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun createFreeSaleInvoice(
        description: String,
        totalAmount: Double,
        paymentType: PaymentType,
        customerId: Long?,
        customerName: String?,
        paidAmount: Double
    ): Long {
        val db = writableDatabase
        var invoiceId: Long = -1
        db.beginTransaction()
        try {
            val remainingDebt = if (paymentType == PaymentType.CREDIT) {
                maxOf(0.0, totalAmount - paidAmount)
            } else 0.0

            val invNum = "INV-" + (System.currentTimeMillis() % 100000)

            val cvInv = ContentValues().apply {
                put("invoice_number", invNum)
                put("timestamp", System.currentTimeMillis())
                put("customer_id", customerId)
                put("customer_name", customerName)
                put("payment_type", paymentType.name)
                put("total_amount", totalAmount)
                put("paid_amount", paidAmount)
                put("remaining_debt", remainingDebt)
            }
            invoiceId = db.insert(TABLE_INVOICES, null, cvInv)

            val cvItem = ContentValues().apply {
                put("invoice_id", invoiceId)
                put("product_id", 0L)
                put("product_name", description.ifBlank { "مبيعات حرة" })
                put("unit_price", totalAmount)
                put("quantity", 1.0)
                put("subtotal", totalAmount)
            }
            db.insert(TABLE_INVOICE_ITEMS, null, cvItem)

            if (paymentType == PaymentType.CREDIT && customerId != null && remainingDebt > 0) {
                db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt + ? WHERE id = ?", arrayOf(remainingDebt, customerId))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return invoiceId
    }

    fun createTableInvoice(
        rows: List<InvoiceRowDraft>,
        paymentType: PaymentType,
        customerId: Long?,
        customerName: String?,
        paidAmount: Double
    ): Long {
        val db = writableDatabase
        var invoiceId: Long = -1
        db.beginTransaction()
        try {
            val totalAmount = rows.sumOf { it.subtotal }
            val remainingDebt = if (paymentType == PaymentType.CREDIT) {
                totalAmount - paidAmount
            } else 0.0

            val invNum = "INV-" + (System.currentTimeMillis() % 100000)

            val cvInv = ContentValues().apply {
                put("invoice_number", invNum)
                put("timestamp", System.currentTimeMillis())
                put("customer_id", customerId)
                put("customer_name", customerName)
                put("payment_type", paymentType.name)
                put("total_amount", totalAmount)
                put("paid_amount", paidAmount)
                put("remaining_debt", remainingDebt)
            }
            invoiceId = db.insert(TABLE_INVOICES, null, cvInv)

            for (row in rows) {
                val cleanName = row.name.trim().ifBlank { "صنف عام" }
                // Check if product exists in inventory; if not, add it without duplicates
                var prodId = 0L
                val cursor = db.rawQuery(
                    "SELECT id FROM $TABLE_PRODUCTS WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) LIMIT 1",
                    arrayOf(cleanName)
                )
                if (cursor.moveToFirst()) {
                    prodId = cursor.getLong(0)
                } else {
                    val cvNewProd = ContentValues().apply {
                        put("name", cleanName)
                        put("category", ProductCategory.OTHER.name)
                        put("barcode", "")
                        put("cost_price", row.price * 0.85)
                        put("sell_price", row.price)
                        put("stock_qty", 100.0)
                        put("unit", "حبة")
                    }
                    prodId = db.insert(TABLE_PRODUCTS, null, cvNewProd)
                }
                cursor.close()

                val cvItem = ContentValues().apply {
                    put("invoice_id", invoiceId)
                    put("product_id", prodId)
                    put("product_name", cleanName)
                    put("unit_price", row.price)
                    put("quantity", row.quantity)
                    put("subtotal", row.subtotal)
                }
                db.insert(TABLE_INVOICE_ITEMS, null, cvItem)
            }

            if (paymentType == PaymentType.CREDIT && customerId != null && remainingDebt > 0) {
                db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt + ? WHERE id = ?", arrayOf(remainingDebt, customerId))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return invoiceId
    }

    // Direct movement matching "له" and "عليه" in Screenshot 2
    fun addDirectCustomerMovement(
        customerId: Long,
        customerName: String,
        isForCustomer: Boolean, // true = له (دفع وسدد), false = عليه (دين/مشتريات)
        amount: Double,
        details: String,
        timestamp: Long
    ): Long {
        val db = writableDatabase
        var resultId: Long = -1
        db.beginTransaction()
        try {
            if (isForCustomer) {
                // له = سداد دفعة نقدية تقلل الدين
                val cv = ContentValues().apply {
                    put("customer_id", customerId)
                    put("customer_name", customerName)
                    put("amount", amount)
                    put("timestamp", timestamp)
                    put("notes", details.ifBlank { "دفعة نقدية (له)" })
                }
                resultId = db.insert(TABLE_DEBT_PAYMENTS, null, cv)
                db.execSQL(
                    "UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt - ? WHERE id = ?",
                    arrayOf(amount, customerId)
                )
            } else {
                // عليه = إضافة دين / فاتورة مشتريات تزيد الدين
                val invNum = "OP-" + (System.currentTimeMillis() % 100000)
                val cvInv = ContentValues().apply {
                    put("invoice_number", invNum)
                    put("timestamp", timestamp)
                    put("customer_id", customerId)
                    put("customer_name", customerName)
                    put("payment_type", PaymentType.CREDIT.name)
                    put("total_amount", amount)
                    put("paid_amount", 0.0)
                    put("remaining_debt", amount)
                }
                resultId = db.insert(TABLE_INVOICES, null, cvInv)

                val cvItem = ContentValues().apply {
                    put("invoice_id", resultId)
                    put("product_id", 0L)
                    put("product_name", details.ifBlank { "مشتريات على الحساب (عليه)" })
                    put("unit_price", amount)
                    put("quantity", 1.0)
                    put("subtotal", amount)
                }
                db.insert(TABLE_INVOICE_ITEMS, null, cvItem)

                db.execSQL(
                    "UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt + ? WHERE id = ?",
                    arrayOf(amount, customerId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return resultId
    }

    fun updateDebtPayment(id: Long, newAmount: Double, notes: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var oldAmount = 0.0
            var custId = 0L
            val cursor = db.rawQuery("SELECT customer_id, amount FROM $TABLE_DEBT_PAYMENTS WHERE id = ?", arrayOf(id.toString()))
            if (cursor.moveToFirst()) {
                custId = cursor.getLong(0)
                oldAmount = cursor.getDouble(1)
            }
            cursor.close()

            val cv = ContentValues().apply {
                put("amount", newAmount)
                put("notes", notes)
            }
            db.update(TABLE_DEBT_PAYMENTS, cv, "id = ?", arrayOf(id.toString()))

            val diff = newAmount - oldAmount
            if (diff != 0.0 && custId != 0L) {
                // Paying more decreases customer debt further
                db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt - ? WHERE id = ?", arrayOf(diff, custId))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateInvoiceRemaining(id: Long, newTotal: Double, newPaid: Double, newDesc: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var oldRemaining = 0.0
            var custId: Long? = null
            val cursor = db.rawQuery("SELECT customer_id, remaining_debt FROM $TABLE_INVOICES WHERE id = ?", arrayOf(id.toString()))
            if (cursor.moveToFirst()) {
                if (!cursor.isNull(0)) custId = cursor.getLong(0)
                oldRemaining = cursor.getDouble(1)
            }
            cursor.close()

            val newRemaining = (newTotal - newPaid).coerceAtLeast(0.0)
            val cv = ContentValues().apply {
                put("total_amount", newTotal)
                put("paid_amount", newPaid)
                put("remaining_debt", newRemaining)
            }
            db.update(TABLE_INVOICES, cv, "id = ?", arrayOf(id.toString()))

            if (newDesc.isNotBlank()) {
                val cvItem = ContentValues().apply {
                    put("product_name", newDesc)
                    put("unit_price", newTotal)
                    put("subtotal", newTotal)
                }
                db.update(TABLE_INVOICE_ITEMS, cvItem, "invoice_id = ?", arrayOf(id.toString()))
            }

            if (custId != null) {
                val debtDiff = newRemaining - oldRemaining
                if (debtDiff != 0.0) {
                    db.execSQL("UPDATE $TABLE_CUSTOMERS SET balance_debt = balance_debt + ? WHERE id = ?", arrayOf(debtDiff, custId))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getPayments(): List<DebtPayment> {
        val list = mutableListOf<DebtPayment>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEBT_PAYMENTS ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    DebtPayment(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        customerId = cursor.getLong(cursor.getColumnIndexOrThrow("customer_id")),
                        customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                        amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // Customer Account Statement Queries
    fun getCustomerInvoices(customerId: Long): List<Invoice> {
        return getAllInvoices().filter { it.customerId == customerId }
    }

    fun getCustomerPayments(customerId: Long): List<DebtPayment> {
        val list = mutableListOf<DebtPayment>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEBT_PAYMENTS WHERE customer_id = ? ORDER BY timestamp DESC", arrayOf(customerId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    DebtPayment(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        customerId = cursor.getLong(cursor.getColumnIndexOrThrow("customer_id")),
                        customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                        amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // Database Statistics
    fun getDatabaseStats(context: Context): DatabaseImportSummary {
        val dbPath = context.getDatabasePath(DATABASE_NAME)
        val size = if (dbPath.exists()) dbPath.length() else 0L
        return DatabaseImportSummary(
            productsCount = getAllProducts().size,
            customersCount = getAllCustomers().size,
            invoicesCount = getAllInvoices().size,
            paymentsCount = getPayments().size,
            dbSizeBytes = size
        )
    }

    // Database Export to File (SAF)
    fun exportDatabase(context: Context, destinationUri: Uri): Result<Long> = runCatching {
        try {
            writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        } catch (_: Exception) {}

        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            throw IllegalStateException("ملف قاعدة البيانات غير موجود")
        }

        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            FileInputStream(dbFile).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("تعذر الكتابة في مسار الملف المحدد")

        dbFile.length()
    }

    // Prepare Backup File for Direct Sharing (WhatsApp / Drive / Bluetooth)
    fun createBackupFileForSharing(context: Context): File {
        try {
            writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        } catch (_: Exception) {}

        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "alazzi_grocery_backup_${System.currentTimeMillis()}.db")
        FileInputStream(dbFile).use { input ->
            FileOutputStream(backupFile).use { output ->
                input.copyTo(output)
            }
        }
        return backupFile
    }

    // Database Import from File on Phone (SAF)
    fun importDatabase(context: Context, sourceUri: Uri): Result<DatabaseImportSummary> = runCatching {
        val tempFile = File(context.cacheDir, "imported_temp_${System.currentTimeMillis()}.db")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("تعذر فتح وقراءة ملف قاعدة البيانات المحدد")

        // Verify SQLite Header
        val header = ByteArray(16)
        FileInputStream(tempFile).use { it.read(header) }
        val headerStr = String(header)
        if (!headerStr.startsWith("SQLite format 3")) {
            tempFile.delete()
            throw IllegalArgumentException("الملف المختار ليس قاعدة بيانات SQLite صالحة!")
        }

        // Test SQLite integrity
        val testDb = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        testDb.close()

        // Close current database connection
        close()

        // Replace database files
        val dbPath = context.getDatabasePath(DATABASE_NAME)
        if (dbPath.exists()) {
            dbPath.delete()
        }
        val walFile = File(dbPath.path + "-wal")
        if (walFile.exists()) walFile.delete()
        val shmFile = File(dbPath.path + "-shm")
        if (shmFile.exists()) shmFile.delete()
        val journalFile = File(dbPath.path + "-journal")
        if (journalFile.exists()) journalFile.delete()

        // Copy temp file to actual DB location
        FileInputStream(tempFile).use { input ->
            FileOutputStream(dbPath).use { output ->
                input.copyTo(output)
            }
        }
        tempFile.delete()

        // Open newly restored DB and ensure all expected tables exist
        val reopenedDb = writableDatabase
        reopenedDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PRODUCTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                barcode TEXT,
                cost_price REAL NOT NULL,
                sell_price REAL NOT NULL,
                stock_qty REAL NOT NULL,
                unit TEXT NOT NULL
            )
            """.trimIndent()
        )
        reopenedDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CUSTOMERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                balance_debt REAL NOT NULL DEFAULT 0.0,
                notes TEXT
            )
            """.trimIndent()
        )
        reopenedDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_INVOICES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_number TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                customer_id INTEGER,
                customer_name TEXT,
                payment_type TEXT NOT NULL,
                total_amount REAL NOT NULL,
                paid_amount REAL NOT NULL,
                remaining_debt REAL NOT NULL
            )
            """.trimIndent()
        )
        reopenedDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_INVOICE_ITEMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                unit_price REAL NOT NULL,
                quantity REAL NOT NULL,
                subtotal REAL NOT NULL
            )
            """.trimIndent()
        )
        reopenedDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DEBT_PAYMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                customer_name TEXT NOT NULL,
                amount REAL NOT NULL,
                timestamp INTEGER NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )

        val products = getAllProducts()
        val customers = getAllCustomers()
        val invoices = getAllInvoices()
        val payments = getPayments()

        DatabaseImportSummary(
            productsCount = products.size,
            customersCount = customers.size,
            invoicesCount = invoices.size,
            paymentsCount = payments.size,
            dbSizeBytes = dbPath.length()
        )
    }
}
