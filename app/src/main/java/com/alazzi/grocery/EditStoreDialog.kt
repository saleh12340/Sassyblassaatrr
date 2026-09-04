package com.alazzi.grocery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun EditStoreDialog(
    initialStoreInfo: StoreInfo,
    onSave: (StoreInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var storeName by remember { mutableStateOf(initialStoreInfo.name) }
    var ownerName by remember { mutableStateOf(initialStoreInfo.ownerName) }
    var phone by remember { mutableStateOf(initialStoreInfo.phone) }
    var activity by remember { mutableStateOf(initialStoreInfo.activity) }
    var address by remember { mutableStateOf(initialStoreInfo.address) }
    var country by remember { mutableStateOf(initialStoreInfo.country) }
    var currency by remember { mutableStateOf(initialStoreInfo.currency) }
    var invoiceFooter by remember { mutableStateOf(initialStoreInfo.invoiceFooter) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .imePadding()
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
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تعديل بيانات المتجر",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("اسم المتجر / البقالة *") },
                        placeholder = { Text("مثال: بقالة العزي للمواد الغذائية") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("المالك / المدير المسؤول") },
                        placeholder = { Text("مثال: العزي") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف / الواتساب الرسمي *") },
                        placeholder = { Text("771234567 أو 01234567") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = activity,
                        onValueChange = { activity = it },
                        label = { Text("نشاط البقالة / الوصف المختصر") },
                        placeholder = { Text("مواد غذائية، معلبات، مشروبات وتموينات") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان / الموقع") },
                        placeholder = { Text("الشارع العام") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("الدولة") },
                        placeholder = { Text("اليمن") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("رمز العملة المعتمدة") },
                            placeholder = { Text("ريال يمني (ر.ي)") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ريال يمني (ر.ي)", "ريال سعودي (ر.س)", "دولار ($)").forEach { cur ->
                                FilterChip(
                                    selected = currency == cur,
                                    onClick = { currency = cur },
                                    label = { Text(cur, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = invoiceFooter,
                        onValueChange = { invoiceFooter = it },
                        label = { Text("ملاحظة أسفل الفاتورة (رسالة الشكر)") },
                        placeholder = { Text("شكراً لزيارتكم ونسعد بخدمتكم دائماً") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save and Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (storeName.isNotBlank()) {
                                onSave(
                                    StoreInfo(
                                        name = storeName.trim(),
                                        ownerName = ownerName.trim(),
                                        phone = phone.trim(),
                                        activity = activity.trim(),
                                        address = address.trim(),
                                        country = country.trim().ifBlank { "اليمن" },
                                        currency = currency.trim().ifBlank { "ريال يمني (ر.ي)" },
                                        invoiceFooter = invoiceFooter.trim()
                                    )
                                )
                            }
                        },
                        enabled = storeName.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
