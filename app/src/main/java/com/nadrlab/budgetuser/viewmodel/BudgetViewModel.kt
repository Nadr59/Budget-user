package com.nadrlab.budgetuser.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadrlab.budgetuser.data.BudgetRepository
import com.nadrlab.budgetuser.data.UserPrefs
import com.nadrlab.budgetuser.data.db.AppDatabase
import com.nadrlab.budgetuser.data.model.Store
import com.nadrlab.budgetuser.data.model.Transaction
import com.nadrlab.budgetuser.data.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(db.storeDao(), db.transactionDao())
    val userPrefs = UserPrefs(application)

    private val _isReady = MutableStateFlow(userPrefs.isSetupComplete)
    val isReady: StateFlow<Boolean> = _isReady

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

    val transactionCount: StateFlow<Int> = allTransactions
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val purchaseCount: StateFlow<Int> = allTransactions
        .map { txs -> txs.count { it.type == TransactionType.PURCHASE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val paymentCount: StateFlow<Int> = allTransactions
        .map { txs -> txs.count { it.type == TransactionType.PAYMENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTransactions: StateFlow<List<Transaction>> = allTransactions
        .map { txs ->
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            txs.filter { it.date >= startOfDay }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detailedReportItems: StateFlow<List<ReportItem>> =
        allTransactions.combine(allStores) { transactions, stores ->
            transactions.sortedByDescending { it.date }.map { tx ->
                val storeName = stores.find { it.id == tx.storeId }?.name ?: "غير معروف"
                ReportItem(tx.id, storeName, tx.description, tx.amount, tx.type, tx.date, tx.note)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ جديد: كشف التكرارات ═══
    val duplicateWarnings: StateFlow<List<DuplicateWarning>> =
        allTransactions.combine(allStores) { transactions, stores ->
            val duplicates = mutableListOf<DuplicateWarning>()
            val sorted = transactions.sortedByDescending { it.date }

            for (i in sorted.indices) {
                for (j in i + 1 until sorted.size) {
                    val a = sorted[i]
                    val b = sorted[j]

                    // نفس المبلغ + نفس البقالة + نفس النوع + خلال ساعة
                    if (a.storeId == b.storeId &&
                        a.amount == b.amount &&
                        a.type == b.type &&
                        kotlin.math.abs(a.date - b.date) < TimeUnit.HOURS.toMillis(1)
                    ) {
                        val storeName = stores.find { it.id == a.storeId }?.name ?: "غير معروف"
                        duplicates.add(
                            DuplicateWarning(
                                transaction1 = a,
                                transaction2 = b,
                                storeName = storeName,
                                amount = a.amount,
                                type = a.type,
                                timeDiff = kotlin.math.abs(a.date - b.date)
                            )
                        )
                    }
                }
            }
            duplicates.distinctBy { setOf(it.transaction1.id, it.transaction2.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val lastTransaction: StateFlow<LastTransactionInfo?> =
        allTransactions.combine(allStores) { transactions, stores ->
            if (transactions.isEmpty()) null
            else {
                val last = transactions.maxByOrNull { it.date }
                if (last != null) {
                    val storeName = stores.find { it.id == last.storeId }?.name ?: "غير معروف"
                    LastTransactionInfo(storeName, last.amount, last.type, last.date, last.description)
                } else null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun onMessageShown() { _message.value = null }

    fun setUserName(name: String) {
        userPrefs.userName = name
        _userName.value = name
        _isReady.value = true
    }

    fun changeUserName(newName: String) {
        userPrefs.userName = newName
        _userName.value = newName
        _message.value = "تم تغيير الاسم إلى $newName"
    }

    fun logout() {
        userPrefs.clear()
        _isReady.value = false
        _userName.value = ""
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

    // ═══ تصدير بيانات للمشاركة (الأصلي) ═══
    suspend fun exportDataForSharing(): String {
        val stores = db.storeDao().getAllStoresOnce()
        val transactions = db.transactionDao().getUnexportedTransactions()

        if (transactions.isEmpty()) {
            return "لا توجد معاملات جديدة للتصدير"
        }

        val json = JSONObject().apply {
            put("app", "BudgetUser")
            put("v", 1)
            put("d", System.currentTimeMillis())
            put("u", _userName.value)

            val sa = JSONArray()
            for (store in stores) {
                sa.put(JSONObject().apply {
                    put("n", store.name); put("p", store.phone); put("a", store.address)
                })
            }
            put("s", sa)

            val ta = JSONArray()
            for (t in transactions) {
                val storeName = stores.find { it.id == t.storeId }?.name ?: ""
                ta.put(JSONObject().apply {
                    put("n", storeName); put("a", t.amount); put("d", t.description)
                    put("t", if (t.type == TransactionType.PURCHASE) "P" else "Y")
                    put("dt", t.date); put("nt", t.note)
                })
            }
            put("t", ta)
        }

        val base64 = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )

        db.transactionDao().markAllAsExported()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val pCount = transactions.count { it.type == TransactionType.PURCHASE }
        val yCount = transactions.count { it.type == TransactionType.PAYMENT }

        return buildString {
            appendLine("📊 بيانات مشتريات المستخدم")
            appendLine("👤 المرسل: ${_userName.value}")
            appendLine("📅 ${dateFormat.format(Date())}")
            appendLine("🛒 مشتريات: $pCount | 💰 مدفوعات: $yCount")
            appendLine("────────────────")
            appendLine("BB2::$base64")
            appendLine("────────────────")
            appendLine("📥 انسخ هذه الرسالة واضغط استيراد")
        }
    }

    // ═══ جديد: تقرير مفصل للمشرف ═══
    
               // ═══ تقرير مفصل للمشرف (مُحدَّث) ═══
    suspend fun generateReportForAdmin(): String {
        val stores = db.storeDao().getAllStoresOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val dateOnly = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        val timeOnly = SimpleDateFormat("HH:mm", Locale("ar"))

        if (transactions.isEmpty()) {
            return "لا توجد معاملات مسجلة"
        }

        val sorted = transactions.sortedByDescending { it.date }
        val purchases = transactions.filter { it.type == TransactionType.PURCHASE }
        val payments = transactions.filter { it.type == TransactionType.PAYMENT }
        val totalPurchases = purchases.sumOf { it.amount }
        val totalPayments = payments.sumOf { it.amount }
        val debt = totalPurchases - totalPayments

        return buildString {
            appendLine("===========================")
            appendLine("تقرير مشتريات المستخدم")
            appendLine("===========================")
            appendLine("")
            appendLine("المستخدم: ${_userName.value}")
            appendLine("التاريخ: ${dateFormat.format(Date())}")
            appendLine("")

            // ═══ ملخص عام ═══
            appendLine("--- الملخص ---")
            appendLine("عدد المشتريات: ${purchases.size}")
            appendLine("عدد المدفوعات: ${payments.size}")
            appendLine("اجمالي العمليات: ${transactions.size}")
            appendLine("اجمالي المشتريات: ${formatAmount(totalPurchases)}")
            appendLine("اجمالي المدفوعات: ${formatAmount(totalPayments)}")
            appendLine("المديونية: ${formatAmount(kotlin.math.abs(debt))}")
            appendLine("")

            // ═══ تقرير كل بقالة على حدة ═══
            for (store in stores) {
                val storeTransactions = sorted.filter { it.storeId == store.id }
                if (storeTransactions.isEmpty()) continue

                val storePurchases = storeTransactions.filter { it.type == TransactionType.PURCHASE }
                val storePayments = storeTransactions.filter { it.type == TransactionType.PAYMENT }
                val storeTotalPurchases = storePurchases.sumOf { it.amount }
                val storeTotalPayments = storePayments.sumOf { it.amount }
                val storeDebt = storeTotalPurchases - storeTotalPayments

                appendLine("--- تقرير ${store.name} ---")
                appendLine("الهاتف: ${store.phone.ifBlank { "غير مسجل" }}")
                appendLine("العنوان: ${store.address.ifBlank { "غير مسجل" }}")
                appendLine("عدد العمليات: ${storeTransactions.size}")
                appendLine("اجمالي الشراء: ${formatAmount(storeTotalPurchases)}")
                appendLine("اجمالي الدفع: ${formatAmount(storeTotalPayments)}")
                appendLine("المديونية: ${formatAmount(kotlin.math.abs(storeDebt))}")
                appendLine("")

                appendLine("التاريخ  | الوقت | النوع | الوصف              | المبلغ")
                appendLine("---------|-------|-------|--------------------|---------")
                for (tx in storeTransactions) {
                    val date = dateOnly.format(Date(tx.date))
                    val time = timeOnly.format(Date(tx.date))
                    val type = if (tx.type == TransactionType.PURCHASE) "شراء" else "دفع "
                    val desc = if (tx.description.isNotBlank()) tx.description else "بدون وصف"
                    appendLine("$date | $time | $type | $desc | ${formatAmount(tx.amount)}")
                }
                appendLine("")
            }

            // ═══ تنبيه التكرارات ═══
            val dups = duplicateWarnings.value
            if (dups.isNotEmpty()) {
                appendLine("--- تنبيه تكرار ---")
                appendLine("يوجد ${dups.size} عملية مشبوهة بالتكرار:")
                for (dup in dups) {
                    appendLine("  - ${dup.storeName} - ${formatAmount(dup.amount)} (${if (dup.type == TransactionType.PURCHASE) "شراء" else "دفع"})")
                }
                appendLine("")
            }

            appendLine("===========================")
            appendLine("تقرير تلقائي من تطبيق مشتريات المستخدم")
        }
    }

    // ═══ جديد: تقرير بقالة محددة ═══
    suspend fun generateStoreReport(storeId: Long): String {
        val store = db.storeDao().getStoreById(storeId) ?: return "البقالة غير موجودة"
        val transactions = db.transactionDao().getAllTransactionsOnce()
            .filter { it.storeId == storeId }
            .sortedByDescending { it.date }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val dateOnly = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        val timeOnly = SimpleDateFormat("HH:mm", Locale("ar"))

        if (transactions.isEmpty()) {
            return "لا توجد معاملات لـ ${store.name}"
        }

        val purchases = transactions.filter { it.type == TransactionType.PURCHASE }
        val payments = transactions.filter { it.type == TransactionType.PAYMENT }
        val totalPurchases = purchases.sumOf { it.amount }
        val totalPayments = payments.sumOf { it.amount }
        val debt = totalPurchases - totalPayments

        return buildString {
            appendLine("===========================")
            appendLine("تقرير بقالة: ${store.name}")
            appendLine("===========================")
            appendLine("")
            appendLine("المستخدم: ${_userName.value}")
            appendLine("التاريخ: ${dateFormat.format(Date())}")
            appendLine("هاتف البقالة: ${store.phone.ifBlank { "غير مسجل" }}")
            appendLine("")

            appendLine("--- الملخص ---")
            appendLine("عدد المشتريات: ${purchases.size}")
            appendLine("عدد المدفوعات: ${payments.size}")
            appendLine("اجمالي الشراء: ${formatAmount(totalPurchases)}")
            appendLine("اجمالي الدفع: ${formatAmount(totalPayments)}")
            appendLine("المديونية: ${formatAmount(kotlin.math.abs(debt))}")
            appendLine("")

            appendLine("التاريخ  | الوقت | النوع | الوصف              | المبلغ")
            appendLine("---------|-------|-------|--------------------|---------")
            for (tx in transactions) {
                val date = dateOnly.format(Date(tx.date))
                val time = timeOnly.format(Date(tx.date))
                val type = if (tx.type == TransactionType.PURCHASE) "شراء" else "دفع "
                val desc = if (tx.description.isNotBlank()) tx.description else "بدون وصف"
                appendLine("$date | $time | $type | $desc | ${formatAmount(tx.amount)}")
            }
            appendLine("")

            appendLine("===========================")
            appendLine("تقرير من تطبيق مشتريات المستخدم")
        }
    }
    
            
    fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) "%.0f".format(amount) else "%.2f".format(amount)
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date(timestamp))
    }

    // ═══ بيانات ═══
    data class ReportItem(
        val id: Long, val storeName: String, val description: String,
        val amount: Double, val type: TransactionType, val date: Long, val note: String
    )

    data class DuplicateWarning(
        val transaction1: Transaction,
        val transaction2: Transaction,
        val storeName: String,
        val amount: Double,
        val type: TransactionType,
        val timeDiff: Long
    )

    data class LastTransactionInfo(
        val storeName: String, val amount: Double,
        val type: TransactionType, val date: Long, val description: String
    )

    data class StoreWithDebt(
        val store: Store, val totalPurchases: Double,
        val totalPayments: Double, val debt: Double
    )
}
