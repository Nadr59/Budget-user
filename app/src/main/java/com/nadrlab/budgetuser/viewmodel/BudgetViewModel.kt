
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

    private val monthStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val monthEnd: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

    val monthPurchases = repository.getTotalPurchasesByDateRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthPayments = repository.getTotalPaymentsByDateRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val weekStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val weekPurchases = repository.getTotalPurchasesByDateRange(weekStart, System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekPayments = repository.getTotalPaymentsByDateRange(weekStart, System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    suspend fun exportDataForSharing(): String {
        val stores = db.storeDao().getAllStoresOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()

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
