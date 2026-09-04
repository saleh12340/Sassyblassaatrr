package com.alazzi.grocery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalReceiptHelper {

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    fun buildReceiptHtml(invoice: Invoice, storeInfo: StoreInfo = StoreInfo()): String {
        val dateStr = formatDate(invoice.timestamp)
        val customerLine = if (!invoice.customerName.isNullOrBlank()) {
            "<div style='margin-bottom: 4px;'><strong>العميل:</strong> ${invoice.customerName}</div>"
        } else {
            "<div style='margin-bottom: 4px;'><strong>العميل:</strong> عميل نقدي عام</div>"
        }

        val itemsHtml = StringBuilder()
        for (item in invoice.items) {
            itemsHtml.append("""
                <tr>
                    <td style="text-align: right; padding: 4px 0;">${item.productName}</td>
                    <td style="text-align: center; padding: 4px 0;">${item.quantity}</td>
                    <td style="text-align: center; padding: 4px 0;">${String.format(Locale.US, "%.2f", item.unitPrice)}</td>
                    <td style="text-align: left; padding: 4px 0; font-weight: bold;">${String.format(Locale.US, "%.2f", item.subtotal)}</td>
                </tr>
            """.trimIndent())
        }

        val remainingLine = if (invoice.remainingDebt > 0) {
            """
            <div style="display:flex; justify-content:space-between; color:#b91c1c; font-weight:bold; margin-top:4px;">
                <span>المتبقي (دين):</span>
                <span>${String.format(Locale.US, "%.2f", invoice.remainingDebt)} ${storeInfo.currency}</span>
            </div>
            """.trimIndent()
        } else ""

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif, 'Courier New';
                        width: 80mm;
                        margin: 0 auto;
                        padding: 8px;
                        font-size: 12px;
                        color: #000;
                        background: #fff;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 1px dashed #000;
                        padding-bottom: 8px;
                        margin-bottom: 8px;
                    }
                    .title {
                        font-size: 16px;
                        font-weight: bold;
                        margin-bottom: 2px;
                    }
                    .subtitle {
                        font-size: 11px;
                        color: #333;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 8px 0;
                        font-size: 12px;
                    }
                    th {
                        border-bottom: 1px dashed #000;
                        padding: 4px 0;
                        font-weight: bold;
                    }
                    .divider {
                        border-bottom: 1px dashed #000;
                        margin: 6px 0;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 10px;
                        font-size: 11px;
                        border-top: 1px dashed #000;
                        padding-top: 6px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">${storeInfo.name}</div>
                    <div class="subtitle">${storeInfo.activity}</div>
                    <div class="subtitle">رقم الفاتورة: ${invoice.invoiceNumber}</div>
                    <div class="subtitle">$dateStr</div>
                </div>

                $customerLine
                <div style="margin-bottom: 6px;"><strong>نوع الدفع:</strong> ${invoice.paymentType.titleAr}</div>

                <table>
                    <thead>
                        <tr>
                            <th style="text-align: right;">الصنف</th>
                            <th style="text-align: center;">الكمية</th>
                            <th style="text-align: center;">السعر</th>
                            <th style="text-align: left;">الإجمالي</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>

                <div class="divider"></div>

                <div style="display:flex; justify-content:space-between; font-size:14px; font-weight:bold; margin-bottom:4px;">
                    <span>الإجمالي الكلي:</span>
                    <span>${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${storeInfo.currency}</span>
                </div>
                <div style="display:flex; justify-content:space-between; margin-bottom:2px;">
                    <span>المدفوع:</span>
                    <span>${String.format(Locale.US, "%.2f", invoice.paidAmount)} ${storeInfo.currency}</span>
                </div>
                $remainingLine

                <div class="footer">
                    <div>${storeInfo.invoiceFooter}</div>
                    <div>${storeInfo.name} - هاتف: ${storeInfo.phone}</div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun printInvoice(context: Context, invoice: Invoice, storeInfo: StoreInfo = StoreInfo()) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter("فاتورة_${invoice.invoiceNumber}")
                        val attributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager.print("فاتورة_${invoice.invoiceNumber}", printAdapter, attributes)
                    } else {
                        Toast.makeText(context, "خدمة الطباعة غير متوفرة", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val html = buildReceiptHtml(invoice, storeInfo)
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "فشل بدء الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildReceiptText(invoice: Invoice, storeInfo: StoreInfo = StoreInfo()): String {
        val sb = StringBuilder()
        sb.append("🧾 *${storeInfo.name}*\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("📄 رقم الفاتورة: ${invoice.invoiceNumber}\n")
        sb.append("📅 التاريخ: ${formatDate(invoice.timestamp)}\n")
        if (!invoice.customerName.isNullOrBlank()) {
            sb.append("👤 العميل: ${invoice.customerName}\n")
        }
        sb.append("💳 نوع الدفع: ${invoice.paymentType.titleAr}\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("📦 *الأصناف:*\n")
        for (item in invoice.items) {
            sb.append("▫️ ${item.productName} × ${item.quantity} = ${String.format(Locale.US, "%.2f", item.subtotal)} ${storeInfo.currency}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *الإجمالي:* ${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${storeInfo.currency}\n")
        sb.append("💵 *المدفوع:* ${String.format(Locale.US, "%.2f", invoice.paidAmount)} ${storeInfo.currency}\n")
        if (invoice.remainingDebt > 0) {
            sb.append("⚠️ *المتبقي (دين):* ${String.format(Locale.US, "%.2f", invoice.remainingDebt)} ${storeInfo.currency}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("📞 للتواصل: ${storeInfo.phone}\n")
        sb.append("${storeInfo.invoiceFooter} ✨")
        return sb.toString()
    }

    fun shareViaWhatsApp(context: Context, phone: String, message: String) {
        try {
            var cleanPhone = phone.replace(Regex("[^0-9]"), "")
            // Yemeni phone numbers handling (+967)
            if (cleanPhone.startsWith("967")) {
                // already international Yemen
            } else if (cleanPhone.startsWith("00967")) {
                cleanPhone = cleanPhone.substring(2)
            } else if (cleanPhone.startsWith("07") && cleanPhone.length == 10) {
                // e.g. 0771234567 -> 967771234567
                cleanPhone = "967" + cleanPhone.substring(1)
            } else if ((cleanPhone.startsWith("77") || cleanPhone.startsWith("78") || 
                        cleanPhone.startsWith("73") || cleanPhone.startsWith("71") || 
                        cleanPhone.startsWith("70")) && cleanPhone.length == 9) {
                // Yemeni mobile numbers (77, 78, 73, 71, 70) -> 9677...
                cleanPhone = "967$cleanPhone"
            } else if (cleanPhone.startsWith("7") && cleanPhone.length == 9) {
                cleanPhone = "967$cleanPhone"
            } else if (cleanPhone.startsWith("05") && cleanPhone.length == 10) {
                cleanPhone = "966" + cleanPhone.substring(1)
            } else if (cleanPhone.startsWith("5") && cleanPhone.length == 9) {
                cleanPhone = "966$cleanPhone"
            }
            val uri = if (cleanPhone.isNotEmpty()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to generic share
            shareText(context, message, "مشاركة الفاتورة")
        }
    }

    fun shareText(context: Context, text: String, title: String = "مشاركة") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر المشاركة", Toast.LENGTH_SHORT).show()
        }
    }
}
