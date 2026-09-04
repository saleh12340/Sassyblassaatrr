package com.alazzi.grocery

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@Composable
fun DatabaseManagementDialog(
    stats: DatabaseImportSummary?,
    isLoading: Boolean,
    onImport: (Uri) -> Unit,
    onExport: (Uri) -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // File Picker for Importing Database (.db, SQLite, binary)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirmDialog = true
        }
    }

    // File Picker for Exporting Database to Phone Storage
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            onExport(uri)
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "إدارة واستيراد البيانات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "النسخ الاحتياطي وقاعدة البيانات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Database Stats Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "بيانات قاعدة البيانات الحالية على الهاتف:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("الأصناف في المخزن: ${stats?.productsCount ?: 0}", fontSize = 12.sp)
                                    Text("العملاء المسجلين: ${stats?.customersCount ?: 0}", fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("فواتير المبيعات: ${stats?.invoicesCount ?: 0}", fontSize = 12.sp)
                                    val sizeKb = (stats?.dbSizeBytes ?: 0L) / 1024
                                    Text("حجم الملف: $sizeKb KB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Loading indicator when restoring or processing
                    if (isLoading) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "جاري قراءة واستيراد قاعدة البيانات...",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Action 1: Import Database from Phone
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "استيراد قاعدة بيانات من الهاتف",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "اختر ملف قاعدة بيانات SQLite (.db) موجود في هاتفك لاسترجاع البيانات بالكامل.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    importLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/x-sqlite3"))
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اختيار ملف قاعدة البيانات (.db) من الهاتف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Action 2: Export Database to Phone Storage
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "تصدير وحفظ نسخة احتياطية في الهاتف",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "حفظ ملف قاعدة البيانات في مجلد التنزيلات أو المستندات بهاتفك.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    val fileName = "alazzi_grocery_backup_${System.currentTimeMillis()}.db"
                                    exportLauncher.launch(fileName)
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ نسخة احتياطية في ملفات الهاتف", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Action 3: Share Backup File
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "مشاركة ملف قاعدة البيانات مباشرة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "إرسال ملف النسخة الاحتياطية عبر واتساب، تيليجرام أو جوجل درايف.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            FilledTonalButton(
                                onClick = onShare,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFDCFCE7),
                                    contentColor = Color(0xFF15803D)
                                )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("مشاركة ملف القاعدة (واتساب / درايف)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("إغلاق")
                }
            }
        }
    }

    // Confirmation dialog before overwriting database with imported file
    if (showImportConfirmDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportUri = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد استيراد قاعدة البيانات",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "تنبيه هام: سيؤدي استيراد قاعدة البيانات المحددة إلى استبدال البيانات الحالية على التطبيق بالبيانات المستوردة من الملف.\n\nهل أنت متأكد من رغبتك في استيراد هذه القاعدة؟",
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        showImportConfirmDialog = false
                        pendingImportUri = null
                        if (uri != null) {
                            onImport(uri)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، استيراد واستبدال")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        pendingImportUri = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
