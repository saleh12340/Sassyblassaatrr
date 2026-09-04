package com.alazzi.grocery

import android.content.Context
import android.content.SharedPreferences

class StorePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("store_settings", Context.MODE_PRIVATE)

    fun getStoreInfo(): StoreInfo {
        return StoreInfo(
            name = prefs.getString("store_name", "بقالة العزي للمواد الغذائية") ?: "بقالة العزي للمواد الغذائية",
            ownerName = prefs.getString("owner_name", "العزي") ?: "العزي",
            phone = prefs.getString("phone", "0501112233") ?: "0501112233",
            activity = prefs.getString("activity", "مواد غذائية، معلبات، مشروبات وتموينات") ?: "مواد غذائية، معلبات، مشروبات وتموينات",
            address = prefs.getString("address", "الشارع العام") ?: "الشارع العام",
            country = prefs.getString("country", "اليمن") ?: "اليمن",
            currency = prefs.getString("currency", "ريال يمني (ر.ي)") ?: "ريال يمني (ر.ي)",
            invoiceFooter = prefs.getString("invoice_footer", "شكراً لزيارتكم ونسعد بخدمتكم دائماً") ?: "شكراً لزيارتكم ونسعد بخدمتكم دائماً"
        )
    }

    fun saveStoreInfo(info: StoreInfo) {
        prefs.edit()
            .putString("store_name", info.name.trim().ifBlank { "بقالة العزي للمواد الغذائية" })
            .putString("owner_name", info.ownerName.trim().ifBlank { "العزي" })
            .putString("phone", info.phone.trim().ifBlank { "0501112233" })
            .putString("activity", info.activity.trim().ifBlank { "مواد غذائية، معلبات، مشروبات وتموينات" })
            .putString("address", info.address.trim().ifBlank { "الشارع العام" })
            .putString("country", info.country.trim().ifBlank { "اليمن" })
            .putString("currency", info.currency.trim().ifBlank { "ريال يمني (ر.ي)" })
            .putString("invoice_footer", info.invoiceFooter.trim().ifBlank { "شكراً لزيارتكم ونسعد بخدمتكم دائماً" })
            .apply()
    }
}
