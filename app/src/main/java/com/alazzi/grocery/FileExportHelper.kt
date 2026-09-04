package com.alazzi.grocery

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportRow(
    val operationNum: String,
    val date: String,
    val type: String,       // له أو عليه
    val details: String,
    val amount: Double,
    val balance: Double
)

object FileExportHelper {

    private fun getAppFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "com.alazzi.grocery.fileprovider",
            file
        )
    }

    /**
     * Generate a professional PDF document for a Customer Statement or Single Operation
     */
    fun createCustomerStatementPdf(
        context: Context,
        customer: Customer,
        storeInfo: StoreInfo,
        rows: List<ExportRow>,
        totalDebit: Double,
        totalCredit: Double,
        finalBalance: Double
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard width (points)
            val pageHeight = 842 // A4 standard height (points)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
            }

            val boldPaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                color = Color.BLACK
                isFakeBoldText = true
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                color = Color.rgb(21, 101, 192) // Primary Blue
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val rightAlignPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.BLACK
                textAlign = Paint.Align.RIGHT
            }

            val rightBoldPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.BLACK
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }

            val centerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(235, 243, 255)
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.rgb(220, 230, 242)
            }

            val linePaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }

            val redPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.rgb(180, 20, 20)
                textAlign = Paint.Align.RIGHT
            }

            val greenPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.rgb(20, 140, 50)
                textAlign = Paint.Align.RIGHT
            }

            var currentY = 40f

            // Store Header Card
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 70f, headerBgPaint)
            currentY += 25f
            canvas.drawText(storeInfo.name, (pageWidth / 2).toFloat(), currentY, titlePaint)
            currentY += 18f
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("${storeInfo.activity} • ${storeInfo.country} • هاتف: ${storeInfo.phone}", (pageWidth / 2).toFloat(), currentY, boldPaint)
            currentY += 35f

            // Document Title
            titlePaint.textSize = 15f
            titlePaint.color = Color.rgb(30, 30, 30)
            canvas.drawText("كشف حساب تفصيلي للعميل", (pageWidth / 2).toFloat(), currentY, titlePaint)
            currentY += 24f

            // Client Info Box
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())
            canvas.drawText("اسم العميل: ${customer.name}", (pageWidth - 30).toFloat(), currentY, rightBoldPaint)
            canvas.drawText("التاريخ: $dateStr", 30f, currentY, paint)
            currentY += 16f
            canvas.drawText("الهاتف: ${customer.phone.ifBlank { "غير مسجل" }}", (pageWidth - 30).toFloat(), currentY, rightAlignPaint)
            canvas.drawText("العملة المعتمدة: ${storeInfo.currency}", 30f, currentY, paint)
            currentY += 22f

            // Table Header
            val tableTop = currentY
            val tableBottom = currentY + 22f
            canvas.drawRect(20f, tableTop, (pageWidth - 20).toFloat(), tableBottom, tableHeaderPaint)

            // Columns layout (Right to Left):
            // [رقم العملية | التاريخ | النوع | التفاصيل | المبلغ | الرصيد]
            val colOp = pageWidth - 45f
            val colDate = pageWidth - 90f
            val colType = pageWidth - 145f
            val colDetails = pageWidth - 260f
            val colAmount = pageWidth - 430f
            val colBalance = 40f

            canvas.drawText("رقم", colOp, currentY + 15f, rightBoldPaint)
            canvas.drawText("التاريخ", colDate, currentY + 15f, rightBoldPaint)
            canvas.drawText("النوع", colType, currentY + 15f, rightBoldPaint)
            canvas.drawText("البيان والتفاصيل", colDetails, currentY + 15f, rightBoldPaint)
            canvas.drawText("المبلغ", colAmount, currentY + 15f, rightBoldPaint)
            canvas.drawText("الرصيد", colBalance + 40f, currentY + 15f, rightBoldPaint)

            currentY = tableBottom + 16f

            // Rows
            var rowCount = 0
            for (r in rows) {
                if (rowCount % 2 == 1) {
                    canvas.drawRect(20f, currentY - 12f, (pageWidth - 20).toFloat(), currentY + 8f, headerBgPaint)
                }

                canvas.drawText(r.operationNum, colOp, currentY, rightAlignPaint)
                canvas.drawText(r.date, colDate, currentY, rightAlignPaint)

                val typePaint = if (r.type == "له") greenPaint else redPaint
                canvas.drawText(r.type, colType, currentY, typePaint)

                val safeDetails = if (r.details.length > 28) r.details.take(28) + ".." else r.details
                canvas.drawText(safeDetails, colDetails, currentY, rightAlignPaint)

                val amountStr = String.format(Locale.US, "%,.2f", r.amount)
                canvas.drawText(amountStr, colAmount, currentY, typePaint)

                val balanceStr = String.format(Locale.US, "%,.2f", r.balance)
                canvas.drawText(balanceStr, colBalance + 40f, currentY, rightBoldPaint)

                canvas.drawLine(20f, currentY + 8f, (pageWidth - 20).toFloat(), currentY + 8f, linePaint)
                currentY += 22f
                rowCount++
                if (currentY > pageHeight - 110) break // Fit in single clean page
            }

            // Summary box at bottom
            currentY += 10f
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 45f, tableHeaderPaint)
            currentY += 20f

            rightBoldPaint.textSize = 12f
            canvas.drawText(
                "إجمالي عليه: ${String.format(Locale.US, "%,.2f", totalDebit)} ${storeInfo.currency}",
                (pageWidth - 30).toFloat(),
                currentY,
                redPaint
            )
            canvas.drawText(
                "إجمالي له (المدفوع): ${String.format(Locale.US, "%,.2f", totalCredit)} ${storeInfo.currency}",
                (pageWidth / 2).toFloat() + 50f,
                currentY,
                greenPaint
            )
            currentY += 18f

            val balanceLabel = if (finalBalance > 0) "الرصيد المتبقي عليه" else "الرصيد خالص"
            val balColor = if (finalBalance > 0) redPaint else greenPaint
            canvas.drawText(
                "$balanceLabel: ${String.format(Locale.US, "%,.2f", finalBalance)} ${storeInfo.currency}",
                (pageWidth - 30).toFloat(),
                currentY,
                balColor
            )

            currentY += 30f
            boldPaint.textSize = 10f
            boldPaint.textAlign = Paint.Align.CENTER
            boldPaint.color = Color.GRAY
            canvas.drawText(storeInfo.invoiceFooter, (pageWidth / 2).toFloat(), currentY, boldPaint)

            pdfDoc.finishPage(page)

            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "statement_${customer.id}_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate an Excel compatible CSV file with UTF-8 BOM
     */
    fun createCustomerStatementExcelCsv(
        context: Context,
        customer: Customer,
        storeInfo: StoreInfo,
        rows: List<ExportRow>,
        totalDebit: Double,
        totalCredit: Double,
        finalBalance: Double
    ): File? {
        return try {
            val cacheDir = context.cacheDir
            val csvFile = File(cacheDir, "statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(csvFile)

            // UTF-8 BOM for Excel Arabic compatibility
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val sb = StringBuilder()
            sb.append("${storeInfo.name} - ${storeInfo.country}\n")
            sb.append("كشف حساب العميل: ${customer.name}\n")
            sb.append("رقم الهاتف: ${customer.phone}\n")
            sb.append("العملة: ${storeInfo.currency}\n")
            sb.append("تاريخ التصدير: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}\n\n")

            // Headers
            sb.append("رقم الحركة,التاريخ,نوع الحركة,البيان والتفاصيل,المبلغ,الرصيد المتبقي\n")

            for (r in rows) {
                val cleanDetails = r.details.replace(",", " - ").replace("\n", " ")
                sb.append("${r.operationNum},${r.date},${r.type},\"$cleanDetails\",${r.amount},${r.balance}\n")
            }

            sb.append("\n")
            sb.append("إجمالي عليه,,,\"${String.format(Locale.US, "%.2f", totalDebit)}\"\n")
            sb.append("إجمالي له,,,\"${String.format(Locale.US, "%.2f", totalCredit)}\"\n")
            sb.append("الرصيد النهائي,,,\"${String.format(Locale.US, "%.2f", finalBalance)}\"\n")

            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
            fos.close()
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Render statement/invoice table as a clean Image Bitmap and save to cache
     */
    fun createStatementImage(
        context: Context,
        customerName: String,
        storeInfo: StoreInfo,
        rows: List<ExportRow>,
        totalDebit: Double,
        totalCredit: Double,
        finalBalance: Double
    ): File? {
        return try {
            val width = 600
            val height = 300 + (rows.size * 36) + 120
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply { isAntiAlias = true }
            val headerBg = Paint().apply { color = Color.rgb(24, 76, 120) }
            val tableHeader = Paint().apply { color = Color.rgb(230, 240, 250) }
            val dividerPaint = Paint().apply { color = Color.rgb(220, 220, 220); strokeWidth = 1f }

            val titleText = Paint().apply {
                color = Color.WHITE
                textSize = 20f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val subtitleText = Paint().apply {
                color = Color.rgb(220, 235, 255)
                textSize = 12f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val textRight = Paint().apply {
                color = Color.rgb(30, 30, 30)
                textSize = 12f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val textRightBold = Paint().apply {
                color = Color.rgb(30, 30, 30)
                textSize = 13f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val greenText = Paint().apply {
                color = Color.rgb(20, 140, 50)
                textSize = 12f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val redText = Paint().apply {
                color = Color.rgb(180, 20, 20)
                textSize = 12f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            // Draw Header
            canvas.drawRect(0f, 0f, width.toFloat(), 95f, headerBg)
            canvas.drawText(storeInfo.name, (width / 2).toFloat(), 38f, titleText)
            canvas.drawText("${storeInfo.activity} • ${storeInfo.country} • هاتف: ${storeInfo.phone}", (width / 2).toFloat(), 64f, subtitleText)
            canvas.drawText("كشف حساب: $customerName", (width / 2).toFloat(), 85f, subtitleText)

            var y = 120f
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())
            canvas.drawText("العميل: $customerName", (width - 20).toFloat(), y, textRightBold)
            textRight.textAlign = Paint.Align.LEFT
            canvas.drawText("التاريخ: $dateStr", 20f, y, textRight)
            textRight.textAlign = Paint.Align.RIGHT
            y += 20f
            canvas.drawText("العملة: ${storeInfo.currency}", (width - 20).toFloat(), y, textRight)
            y += 18f

            // Table Header
            canvas.drawRect(15f, y, (width - 15).toFloat(), y + 30f, tableHeader)
            y += 20f
            canvas.drawText("رقم", (width - 25).toFloat(), y, textRightBold)
            canvas.drawText("التاريخ", (width - 70).toFloat(), y, textRightBold)
            canvas.drawText("النوع", (width - 145).toFloat(), y, textRightBold)
            canvas.drawText("البيان", (width - 230).toFloat(), y, textRightBold)
            canvas.drawText("المبلغ", (width - 380).toFloat(), y, textRightBold)
            canvas.drawText("الرصيد", 70f, y, textRightBold)
            y += 16f

            // Table Rows
            for (r in rows) {
                canvas.drawLine(15f, y, (width - 15).toFloat(), y, dividerPaint)
                y += 20f
                canvas.drawText(r.operationNum, (width - 25).toFloat(), y, textRight)
                canvas.drawText(r.date, (width - 70).toFloat(), y, textRight)

                val tPaint = if (r.type == "له") greenText else redText
                canvas.drawText(r.type, (width - 145).toFloat(), y, tPaint)

                val shortDetail = if (r.details.length > 20) r.details.take(20) + ".." else r.details
                canvas.drawText(shortDetail, (width - 230).toFloat(), y, textRight)

                val amtStr = String.format(Locale.US, "%,.2f", r.amount)
                canvas.drawText(amtStr, (width - 380).toFloat(), y, tPaint)

                val balStr = String.format(Locale.US, "%,.2f", r.balance)
                canvas.drawText(balStr, 70f, y, textRightBold)
                y += 12f
            }

            // Summary
            y += 15f
            canvas.drawRect(15f, y, (width - 15).toFloat(), y + 45f, tableHeader)
            y += 20f
            canvas.drawText("إجمالي عليه: ${String.format(Locale.US, "%,.2f", totalDebit)} ${storeInfo.currency}", (width - 25).toFloat(), y, redText)
            canvas.drawText("المدفوع له: ${String.format(Locale.US, "%,.2f", totalCredit)} ${storeInfo.currency}", (width / 2).toFloat() + 50f, y, greenText)
            y += 18f
            canvas.drawText("الرصيد المتبقي: ${String.format(Locale.US, "%,.2f", finalBalance)} ${storeInfo.currency}", (width - 25).toFloat(), y, if (finalBalance > 0) redText else greenText)

            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "statement_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.close()
            imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share any file (PDF, CSV, Image) via standard Android Share Sheet
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = getAppFileProviderUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "فشل المشاركة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share single transaction or invoice via WhatsApp
     */
    fun shareSingleOperationViaWhatsApp(
        context: Context,
        customerPhone: String,
        operationNumber: String,
        customerName: String,
        operationType: String,
        amount: Double,
        date: String,
        details: String,
        balance: Double,
        storeInfo: StoreInfo
    ) {
        val message = """
            🧾 *${storeInfo.name}*
            📍 ${storeInfo.country} - ${storeInfo.address}
            📞 هاتف المتجر: ${storeInfo.phone}
            -----------------------------
            📌 *إشعار عملية بحسابكم الكريم*
            👤 العميل: $customerName
            🔢 رقم العملية: #$operationNumber
            📅 التاريخ: $date
            🏷️ نوع الحركة: $operationType
            📝 البيان: $details
            💰 المبلغ: ${String.format(Locale.US, "%.2f", amount)} ${storeInfo.currency}
            ⚖️ الرصيد الحالي: ${String.format(Locale.US, "%.2f", balance)} ${storeInfo.currency}
            -----------------------------
            ${storeInfo.invoiceFooter}
        """.trimIndent()

        ThermalReceiptHelper.shareViaWhatsApp(context, customerPhone, message)
    }

    /**
     * Share complete account statement via WhatsApp text
     */
    fun shareFullStatementViaWhatsApp(
        context: Context,
        customer: Customer,
        storeInfo: StoreInfo,
        rows: List<ExportRow>,
        totalDebit: Double,
        totalCredit: Double,
        finalBalance: Double
    ) {
        val sb = StringBuilder()
        sb.append("🏪 *${storeInfo.name}*\n")
        sb.append("📍 ${storeInfo.country} - ${storeInfo.address}\n")
        sb.append("📞 ${storeInfo.phone}\n")
        sb.append("-----------------------------\n")
        sb.append("📋 *كشف حساب تفصيلي*\n")
        sb.append("👤 العميل: ${customer.name}\n")
        sb.append("📅 التاريخ: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())}\n")
        sb.append("-----------------------------\n")

        for (r in rows.take(15)) { // First 15 operations for readable WhatsApp message
            sb.append("🔹 [${r.date}] #${r.operationNum}\n")
            sb.append("   ${r.type}: ${String.format(Locale.US, "%.2f", r.amount)} ${storeInfo.currency} (${r.details})\n")
        }

        if (rows.size > 15) {
            sb.append("... وغيرها من العمليات السابقة\n")
        }

        sb.append("-----------------------------\n")
        sb.append("🔴 إجمالي المشتريات (عليه): ${String.format(Locale.US, "%.2f", totalDebit)} ${storeInfo.currency}\n")
        sb.append("🟢 إجمالي المدفوعات (له): ${String.format(Locale.US, "%.2f", totalCredit)} ${storeInfo.currency}\n")
        sb.append("⚖️ *الرصيد المتبقي: ${String.format(Locale.US, "%.2f", finalBalance)} ${storeInfo.currency}*\n")
        sb.append("-----------------------------\n")
        sb.append(storeInfo.invoiceFooter)

        ThermalReceiptHelper.shareViaWhatsApp(context, customer.phone, sb.toString())
    }

    /**
     * Generate single invoice PDF
     */
    fun createInvoicePdf(
        context: Context,
        invoice: Invoice,
        storeInfo: StoreInfo
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 91, 148)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.rgb(80, 80, 80)
                textSize = 11f
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(30, 91, 148)
                style = Paint.Style.FILL
            }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.rgb(220, 220, 220)
                strokeWidth = 1f
            }

            var y = 45f
            canvas.drawText(storeInfo.name, 550f - titlePaint.measureText(storeInfo.name), y, titlePaint)
            y += 20f
            val subText = "${storeInfo.activity} • ${storeInfo.country} - ${storeInfo.phone}"
            canvas.drawText(subText, 550f - subtitlePaint.measureText(subText), y, subtitlePaint)
            y += 20f

            val invTitle = "فاتورة مبيعات رقم #${invoice.invoiceNumber}"
            canvas.drawText(invTitle, 550f - boldPaint.measureText(invTitle), y, boldPaint)
            val dateStr = ThermalReceiptHelper.formatDate(invoice.timestamp)
            canvas.drawText(dateStr, 45f, y, subtitlePaint)
            y += 18f

            val custStr = "العميل: ${invoice.customerName ?: "عميل نقدي عام"}"
            canvas.drawText(custStr, 550f - textPaint.measureText(custStr), y, textPaint)
            val payStr = "طريقة الدفع: ${invoice.paymentType.titleAr}"
            canvas.drawText(payStr, 45f, y, textPaint)
            y += 24f

            // Table Header
            canvas.drawRect(45f, y, 550f, y + 26f, headerPaint)
            val headerY = y + 17f
            canvas.drawText("م", 530f, headerY, headerTextPaint)
            canvas.drawText("اسم الصنف / البيان", 350f, headerY, headerTextPaint)
            canvas.drawText("الكمية", 230f, headerY, headerTextPaint)
            canvas.drawText("السعر", 140f, headerY, headerTextPaint)
            canvas.drawText("الإجمالي", 60f, headerY, headerTextPaint)
            y += 26f

            // Table Rows
            var idx = 1
            for (item in invoice.items) {
                y += 22f
                canvas.drawText("$idx", 530f, y, textPaint)
                canvas.drawText(item.productName, 350f, y, textPaint)
                canvas.drawText("${item.quantity}", 230f, y, textPaint)
                canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), 140f, y, textPaint)
                canvas.drawText(String.format(Locale.US, "%.2f", item.subtotal), 60f, y, boldPaint)
                y += 6f
                canvas.drawLine(45f, y, 550f, y, linePaint)
                idx++
            }

            // Totals
            y += 30f
            val totalBoxPaint = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }
            canvas.drawRect(45f, y, 550f, y + 65f, totalBoxPaint)
            y += 22f
            canvas.drawText("الإجمالي: ${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${storeInfo.currency}", 530f - 120f, y, boldPaint)
            canvas.drawText("المدفوع: ${String.format(Locale.US, "%.2f", invoice.paidAmount)} ${storeInfo.currency}", 280f, y, textPaint)
            if (invoice.remainingDebt > 0) {
                val redPaint = Paint().apply {
                    color = Color.rgb(185, 28, 28)
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("المتبقي (دين): ${String.format(Locale.US, "%.2f", invoice.remainingDebt)} ${storeInfo.currency}", 60f, y, redPaint)
            }
            y += 24f
            canvas.drawText(storeInfo.invoiceFooter, 550f - subtitlePaint.measureText(storeInfo.invoiceFooter), y, subtitlePaint)

            pdfDoc.finishPage(page)
            val file = File(context.cacheDir, "invoice_${invoice.invoiceNumber}.pdf")
            val fos = FileOutputStream(file)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate single invoice image (JPG)
     */
    fun createInvoiceImage(
        context: Context,
        invoice: Invoice,
        storeInfo: StoreInfo
    ): File? {
        return try {
            val width = 600
            val height = 400 + (invoice.items.size * 32)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 91, 148)
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 13f
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.rgb(220, 220, 220)
                strokeWidth = 1f
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(30, 91, 148)
                style = Paint.Style.FILL
            }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var y = 45f
            canvas.drawText(storeInfo.name, (width - 30) - titlePaint.measureText(storeInfo.name), y, titlePaint)
            y += 24f
            val subText = "${storeInfo.activity} • هاتف: ${storeInfo.phone}"
            canvas.drawText(subText, (width - 30) - textPaint.measureText(subText), y, textPaint)
            y += 24f

            val invTitle = "فاتورة مبيعات #${invoice.invoiceNumber}"
            canvas.drawText(invTitle, (width - 30) - boldPaint.measureText(invTitle), y, boldPaint)
            val dateStr = ThermalReceiptHelper.formatDate(invoice.timestamp)
            canvas.drawText(dateStr, 30f, y, textPaint)
            y += 24f

            val custStr = "العميل: ${invoice.customerName ?: "عميل نقدي عام"}"
            canvas.drawText(custStr, (width - 30) - textPaint.measureText(custStr), y, textPaint)
            y += 24f

            // Header
            canvas.drawRect(20f, y, (width - 20).toFloat(), y + 30f, headerPaint)
            val hy = y + 20f
            canvas.drawText("م", (width - 45).toFloat(), hy, headerTextPaint)
            canvas.drawText("الصنف", (width - 200).toFloat(), hy, headerTextPaint)
            canvas.drawText("الكمية", (width - 330).toFloat(), hy, headerTextPaint)
            canvas.drawText("السعر", (width - 430).toFloat(), hy, headerTextPaint)
            canvas.drawText("الإجمالي", 40f, hy, headerTextPaint)
            y += 30f

            // Rows
            var idx = 1
            for (item in invoice.items) {
                y += 24f
                canvas.drawText("$idx", (width - 45).toFloat(), y, textPaint)
                val shortName = if (item.productName.length > 18) item.productName.take(18) + ".." else item.productName
                canvas.drawText(shortName, (width - 200).toFloat(), y, textPaint)
                canvas.drawText("${item.quantity}", (width - 330).toFloat(), y, textPaint)
                canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), (width - 430).toFloat(), y, textPaint)
                canvas.drawText(String.format(Locale.US, "%.2f", item.subtotal), 40f, y, boldPaint)
                y += 6f
                canvas.drawLine(20f, y, (width - 20).toFloat(), y, linePaint)
                idx++
            }

            y += 30f
            canvas.drawText("المجموع الإجمالي: ${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${storeInfo.currency}", (width - 30) - boldPaint.measureText("المجموع الإجمالي: ..."), y, boldPaint)
            if (invoice.remainingDebt > 0) {
                y += 24f
                val redPaint = Paint().apply {
                    color = Color.rgb(185, 28, 28)
                    textSize = 14f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("المتبقي (دين): ${String.format(Locale.US, "%.2f", invoice.remainingDebt)} ${storeInfo.currency}", (width - 30) - redPaint.measureText("المتبقي (دين): ..."), y, redPaint)
            }

            y += 30f
            canvas.drawText(storeInfo.invoiceFooter, (width / 2f) - (textPaint.measureText(storeInfo.invoiceFooter) / 2f), y, textPaint)

            val file = File(context.cacheDir, "invoice_${invoice.invoiceNumber}.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
