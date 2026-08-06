
package com.nadrlab.baitbudget.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadrlab.baitbudget.data.BudgetRepository
import com.nadrlab.baitbudget.data.UserPrefs
import com.nadrlab.baitbudget.data.db.AppDatabase
import com.nadrlab.baitbudget.data.model.Store
import com.nadrlab.baitbudget.data.model.Transaction
import com.nadrlab.baitbudget.data.model.TransactionType
import com.nadrlab.baitbudget.data.model.UserSummaryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(db.storeDao(), db.transactionDao())
    val userPrefs = UserPrefs(application)

    private val _isLoggedIn = MutableStateFlow(userPrefs.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isAdmin = MutableStateFlow(userPrefs.isAdmin)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _userName = MutableStateFlow(userPrefs.userName)
    val userName: StateFlow<String> = _userName

    val allStores = repository.getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimePurchases = repository.getAllTimePurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTimePayments = repository.getAllTimePayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val userSummaries: StateFlow<List<UserSummaryData>> = repository.getUserSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storesWithDebt: StateFlow<List<StoreWithDebt>> = allStores.flatMapLatest { stores ->
        if (stores.isEmpty()) flowOf(emptyList())
        else combine(stores.map { store ->
            combine(
                repository.getTotalPurchases(store.id),
                repository.getTotalPayments(store.id)
            ) { purchases, payments ->
                StoreWithDebt(store, purchases, payments, purchases - payments)
            }
        }) { it.toList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun onMessageShown() { _message.value = null }

    fun loginAsAdmin(password: String): Boolean {
        return if (userPrefs.checkAdminPassword(password)) {
            userPrefs.isAdmin = true; userPrefs.isLoggedIn = true; userPrefs.userName = "مشرف"
            _isAdmin.value = true; _isLoggedIn.value = true; _userName.value = "مشرف"
            true
        } else false
    }

    fun loginAsUser(name: String) {
        userPrefs.isAdmin = false; userPrefs.isLoggedIn = true; userPrefs.userName = name
        _isAdmin.value = false; _isLoggedIn.value = true; _userName.value = name
    }

    fun logout() {
        userPrefs.logout()
        _isLoggedIn.value = false; _isAdmin.value = false; _userName.value = ""
    }

    fun changeAdminPassword(oldPass: String, newPass: String): Boolean {
        return userPrefs.changeAdminPassword(oldPass, newPass)
    }

    fun addStore(name: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.insertStore(Store(name = name, phone = phone, address = address))
            _message.value = "تم إضافة $name"
        }
    }

    fun deleteStore(store: Store) {
        viewModelScope.launch {
            repository.deleteStore(store)
            _message.value = "تم حذف ${store.name}"
        }
    }

    fun addPurchase(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    storeId = storeId, amount = amount, description = description,
                    type = TransactionType.PURCHASE, note = note,
                    senderTag = _userName.value
                )
            )
            _message.value = "تم تسجيل الشراء: ${formatAmount(amount)}"
        }
    }

    fun addPayment(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    storeId = storeId, amount = amount, description = description,
                    type = TransactionType.PAYMENT, note = note,
                    senderTag = _userName.value
                )
            )
            _message.value = "تم تسجيل الدفع: ${formatAmount(amount)}"
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _message.value = "تم حذف المعاملة"
        }
    }

    fun normalizeNumbers(text: String): String {
        return text
            .replace('٠', '0').replace('١', '1').replace('٢', '2')
            .replace('٣', '3').replace('٤', '4').replace('٥', '5')
            .replace('٦', '6').replace('٧', '7').replace('٨', '8')
            .replace('٩', '9').replace('٫', '.')
    }

    // ═══════════════════════════════
    // التصدير (فقط المعاملات الجديدة)
    // ═══════════════════════════════
    suspend fun exportDataForSharing(): String {
        val stores = db.storeDao().getAllStoresOnce()
        val transactions = db.transactionDao().getUnexportedTransactions()

        if (transactions.isEmpty()) {
            return "لا توجد معاملات جديدة للتصدير"
        }

        val json = JSONObject().apply {
            put("app", "BaitBudget")
            put("v", 1)
            put("d", System.currentTimeMillis())
            put("u", _userName.value)

            val sa = JSONArray()
            for (store in stores) {
                sa.put(JSONObject().apply {
                    put("n", store.name)
                    put("p", store.phone)
                    put("a", store.address)
                })
            }
            put("s", sa)

            val ta = JSONArray()
            for (t in transactions) {
                val storeName = stores.find { it.id == t.storeId }?.name ?: ""
                ta.put(JSONObject().apply {
                    put("n", storeName)
                    put("a", t.amount)
                    put("d", t.description)
                    put("t", if (t.type == TransactionType.PURCHASE) "P" else "Y")
                    put("dt", t.date)
                    put("nt", t.note)
                })
            }
            put("t", ta)
        }

        val base64 = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )

        // تحديد المعاملات كمُصدَّرة
        db.transactionDao().markAllAsExported()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val pCount = transactions.count { it.type == TransactionType.PURCHASE }
        val yCount = transactions.count { it.type == TransactionType.PAYMENT }

        return buildString {
            appendLine("📊 بيانات ميزانية البيت")
            appendLine("👤 المرسل: ${_userName.value}")
            appendLine("📅 ${dateFormat.format(Date())}")
            appendLine("🛒 مشتريات: $pCount | 💰 مدفوعات: $yCount")
            appendLine("────────────────")
            appendLine("BB2::$base64")
            appendLine("────────────────")
            appendLine("📥 انسخ هذه الرسالة واضغط استيراد")
        }
    }

    // ═══════════════════════════════
    // الاستيراد (مع فحص التكرار)
    // ═══════════════════════════════
    fun importFromClipboard(clipboardText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanText = clipboardText
                    .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\uFEFF]"), "")
                    .replace("\r\n", "\n").replace("\r", "\n")

                val markerIndex = cleanText.indexOf("BB2::")
                if (markerIndex < 0) {
                    _message.value = "لم يتم العثور على بيانات صالحة"
                    return@launch
                }

                val afterMarker = cleanText.substring(markerIndex + 5)
                val base64Chars = StringBuilder()

                for (line in afterMarker.split("\n")) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("────") || trimmed.startsWith("---")) {
                        if (base64Chars.isNotEmpty()) break
                        continue
                    }
                    if (trimmed.length < 20 && base64Chars.isEmpty() && !trimmed.contains("=")) continue
                    for (ch in trimmed) {
                        if (ch.isLetterOrDigit() || ch == '+' || ch == '/' || ch == '-' || ch == '_' || ch == '=') {
                            base64Chars.append(ch)
                        }
                    }
                }

                val base64 = base64Chars.toString().trim()
                if (base64.length < 20) {
                    _message.value = "بيانات غير كافية"
                    return@launch
                }

                val decoded = try {
                    Base64.decode(base64, Base64.NO_WRAP or Base64.URL_SAFE)
                } catch (e: Exception) {
                    try { Base64.decode(base64, Base64.DEFAULT) }
                    catch (e2: Exception) {
                        _message.value = "لا يمكن فك تشفير البيانات"
                        return@launch
                    }
                }

                val json = try { JSONObject(String(decoded, Charsets.UTF_8)) }
                catch (e: Exception) {
                    _message.value = "بيانات غير صالحة"
                    return@launch
                }

                val senderName = json.optString("u", "غير معروف")
                val storesArray = try { json.getJSONArray("s") } catch (e: Exception) {
                    _message.value = "لا توجد بقالات"; return@launch
                }
                val transArray = try { json.getJSONArray("t") } catch (e: Exception) {
                    _message.value = "لا توجد معاملات"; return@launch
                }

                val currentStores = db.storeDao().getAllStoresOnce()
                val storeMap = mutableMapOf<String, Long>()

                for (i in 0 until storesArray.length()) {
                    val s = storesArray.getJSONObject(i)
                    val name = s.getString("n")
                    val phone = s.optString("p", "")
                    val address = s.optString("a", "")
                    val existing = currentStores.find { it.name.equals(name, ignoreCase = true) }
                    val storeId = existing?.id ?: repository.insertStore(
                        Store(name = name, phone = phone, address = address)
                    )
                    storeMap[name] = storeId
                }

                var count = 0
                var skipped = 0
                for (i in 0 until transArray.length()) {
                    try {
                        val t = transArray.getJSONObject(i)
                        val storeName = t.getString("n")
                        val storeId = storeMap[storeName] ?: continue
                        val amount = t.getDouble("a")
                        val transType = if (t.getString("t") == "P") TransactionType.PURCHASE else TransactionType.PAYMENT
                        val date = t.getLong("dt")

                        // ═══ فحص التكرار ═══
                        val existing = repository.countDuplicate(storeId, amount, transType, date)
                        if (existing > 0) {
                            skipped++
                            continue
                        }

                        repository.insertTransaction(Transaction(
                            storeId = storeId,
                            amount = amount,
                            description = t.optString("d", ""),
                            type = transType,
                            date = date,
                            note = t.optString("nt", ""),
                            senderTag = senderName,
                            exported = true
                        ))
                        count++
                    } catch (_: Exception) {}
                }

                val msg = buildString {
                    append("تم استيراد $count معاملة من $senderName")
                    if (skipped > 0) append(" (تم تخطي $skipped مكررة)")
                }
                _message.value = msg

            } catch (e: Exception) {
                _message.value = "خطأ: ${e.message}"
            }
        }
    }

    fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) "%.0f".format(amount) else "%.2f".format(amount)
    }

    data class StoreWithDebt(
        val store: Store,
        val totalPurchases: Double,
        val totalPayments: Double,
        val debt: Double
    )
}
