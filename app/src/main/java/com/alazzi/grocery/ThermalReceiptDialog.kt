package com.alazzi.grocery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@Composable
fun ThermalReceiptDialog(
    invoice: Invoice,
    storeInfo: StoreInfo = StoreInfo(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "إيصال فاتورة - 80mm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Realistic 80mm Thermal Receipt Simulation View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFAFA))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .verticalScroll(scrollState)
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo),
                                contentDescription = "شعار المتجر",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            storeInfo.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            storeInfo.activity,
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "رقم الفاتورة: ${invoice.invoiceNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            ThermalReceiptHelper.formatDate(invoice.timestamp),
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "العميل: ${invoice.customerName ?: "عميل نقدي عام"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                "الدفع: ${invoice.paymentType.titleAr}",
                                fontSize = 12.sp,
                                color = Color(0xFF1F2937)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(6.dp))

                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصنف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(2f))
                            Text("الكمية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text("السعر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text("المجموع", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Table Rows
                        for (item in invoice.items) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.productName, fontSize = 11.sp, color = Color(0xFF1F2937), modifier = Modifier.weight(2f))
                                Text("${item.quantity}", fontSize = 11.sp, color = Color(0xFF1F2937), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(String.format(Locale.US, "%.2f", item.unitPrice), fontSize = 11.sp, color = Color(0xFF1F2937), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(String.format(Locale.US, "%.2f", item.subtotal), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(6.dp))

                        // Totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي الكلي:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${storeInfo.currency}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المدفوع نقداً:", fontSize = 12.sp, color = Color(0xFF374151))
                            Text("${String.format(Locale.US, "%.2f", invoice.paidAmount)} ${storeInfo.currency}", fontSize = 12.sp, color = Color(0xFF374151))
                        }

                        if (invoice.remainingDebt > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المتبقي (دين):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DebtRed)
                                Text("${String.format(Locale.US, "%.2f", invoice.remainingDebt)} ${storeInfo.currency}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DebtRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(storeInfo.invoiceFooter, fontSize = 11.sp, color = Color(0xFF4B5563), textAlign = TextAlign.Center)
                        Text("${storeInfo.name} - هاتف: ${storeInfo.phone}", fontSize = 10.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons (Print, WhatsApp, Share)
                Button(
                    onClick = {
                        ThermalReceiptHelper.printInvoice(context, invoice, storeInfo)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("طباعة حرارية 80mm", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val text = ThermalReceiptHelper.buildReceiptText(invoice, storeInfo)
                            val phone = invoice.customerName?.let { "" } ?: ""
                            ThermalReceiptHelper.shareViaWhatsApp(context, phone, text)
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
                        Text("واتساب", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val pdfFile = FileExportHelper.createInvoicePdf(context, invoice, storeInfo)
                            if (pdfFile != null) {
                                FileExportHelper.shareFile(context, pdfFile, "application/pdf", "فاتورة #${invoice.invoiceNumber}")
                            } else {
                                android.widget.Toast.makeText(context, "فشل إنشاء ملف PDF", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val imgFile = FileExportHelper.createInvoiceImage(context, invoice, storeInfo)
                            if (imgFile != null) {
                                FileExportHelper.shareFile(context, imgFile, "image/jpeg", "فاتورة #${invoice.invoiceNumber}")
                            } else {
                                android.widget.Toast.makeText(context, "فشل إنشاء صورة الفاتورة", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صورة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashedDivider(color: Color = Color(0xFF9CA3AF)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("- - - - - - - - - - - - - - - - - - - - - - - - - -", color = color, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}
