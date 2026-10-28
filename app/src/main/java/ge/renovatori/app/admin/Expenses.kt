package ge.renovatori.app.admin

import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.Paged
import ge.renovatori.app.network.Pagination
import ge.renovatori.app.network.UploadFile
import ge.renovatori.app.network.instantOrNull
import ge.renovatori.app.network.stringOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate

/** Only projects in this status accept new rows (backend `ONGOING_STATUS`). */
const val ONGOING_STATUS = "მიმდინარე"

/** Date-based project progress (backend `getProjectProgress`). */
data class ProjectProgress(val percent: Int, val label: String, val isValid: Boolean) {
    companion object {
        fun fromJson(json: JSONObject?) = ProjectProgress(
            percent = json?.optInt("percent") ?: 0,
            label = json?.optString("label").orEmpty(),
            isValid = json?.optBoolean("isValid") ?: false
        )
    }
}

/** Row of the admin project list (`serializeExpenseCard`). */
data class ExpenseCard(
    val id: String,
    val projectName: String,
    val projectStatus: String,
    val totalAmount: Double,
    val totalExpenses: Double,
    val remainingAmount: Double,
    val currentAmount: Double,
    val progress: ProjectProgress
) {
    companion object {
        fun fromJson(json: JSONObject) = ExpenseCard(
            id = json.getString("id"),
            projectName = json.optString("projectName"),
            projectStatus = json.optString("projectStatus"),
            totalAmount = json.optDouble("totalAmount", 0.0),
            totalExpenses = json.optDouble("totalExpenses", 0.0),
            remainingAmount = json.optDouble("remainingAmount", 0.0),
            currentAmount = json.optDouble("currentAmount", 0.0),
            progress = ProjectProgress.fromJson(json.optJSONObject("progress"))
        )
    }
}

data class ExpenseType(val id: String, val title: String)
data class ExpenseImage(val url: String, val thumbUrl: String)
data class TypeShare(val title: String, val amount: Double, val percent: Double)
data class PaidAmount(val id: String, val paidAmount: Double, val paidAmountDollar: Double, val paidAt: Instant?, val comment: String)

data class ExpenseEntry(
    val id: String,
    val expenseType: ExpenseType?,
    val expenseDate: Instant?,
    val amount: Double,
    val comment: String,
    val images: List<ExpenseImage>
)

/** Full project (`serializeExpenseDetail`); [canManage] is true for admins, [canAddEntries] also needs „მიმდინარე“. */
data class ExpenseDetail(
    val id: String,
    val projectName: String,
    val projectStatus: String,
    val projectAddress: String,
    val publicProjectId: String?,
    val clientId: String?,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String,
    val area: Double,
    val totalAmount: Double,
    val totalAmountDollar: Double,
    val pricePerSquareArea: Double,
    val pricePerSquareAreaDollar: Double,
    val remainingAmount: Double,
    val remainingAmountDollar: Double,
    val totalExpenses: Double,
    val totalProfit: Double,
    val totalPaidAmount: Double,
    val totalPaidAmountDollar: Double,
    val currentAmount: Double,
    val startDate: Instant?,
    val endDate: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val progress: ProjectProgress,
    val expenseTypeSummary: List<TypeShare>,
    val paidAmounts: List<PaidAmount>,
    val expenses: List<ExpenseEntry>,
    val canManage: Boolean,
    val canAddEntries: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): ExpenseDetail {
            val permissions = json.optJSONObject("permissions")
            return ExpenseDetail(
                id = json.getString("id"),
                projectName = json.optString("projectName"),
                projectStatus = json.optString("projectStatus"),
                projectAddress = json.optString("projectAddress"),
                publicProjectId = json.stringOrNull("publicProjectId"),
                clientId = json.stringOrNull("clientId"),
                clientName = json.optString("clientName"),
                clientPhone = json.optString("clientPhone"),
                clientEmail = json.optString("clientEmail"),
                area = json.optDouble("area", 0.0),
                totalAmount = json.optDouble("totalAmount", 0.0),
                totalAmountDollar = json.optDouble("totalAmountDollar", 0.0),
                pricePerSquareArea = json.optDouble("pricePerSquareArea", 0.0),
                pricePerSquareAreaDollar = json.optDouble("pricePerSquareAreaDollar", 0.0),
                remainingAmount = json.optDouble("remainingAmount", 0.0),
                remainingAmountDollar = json.optDouble("remainingAmountDollar", 0.0),
                totalExpenses = json.optDouble("totalExpenses", 0.0),
                totalProfit = json.optDouble("totalProfit", 0.0),
                totalPaidAmount = json.optDouble("totalPaidAmount", 0.0),
                totalPaidAmountDollar = json.optDouble("totalPaidAmountDollar", 0.0),
                currentAmount = json.optDouble("currentAmount", 0.0),
                startDate = json.instantOrNull("startDate"),
                endDate = json.instantOrNull("endDate"),
                createdAt = json.instantOrNull("createdAt"),
                updatedAt = json.instantOrNull("updatedAt"),
                progress = ProjectProgress.fromJson(json.optJSONObject("progress")),
                expenseTypeSummary = json.optJSONArray("expenseTypeSummary").objects().map {
                    TypeShare(it.optString("title"), it.optDouble("amount", 0.0), it.optDouble("percent", 0.0))
                },
                paidAmounts = json.optJSONArray("paidAmounts").objects().map {
                    PaidAmount(
                        id = it.getString("id"),
                        paidAmount = it.optDouble("paidAmount", 0.0),
                        paidAmountDollar = it.optDouble("paidAmountDollar", 0.0),
                        paidAt = it.instantOrNull("paidAt"),
                        comment = it.optString("comment")
                    )
                },
                expenses = json.optJSONArray("expenses").objects().map { entry ->
                    ExpenseEntry(
                        id = entry.getString("id"),
                        expenseType = entry.optJSONObject("expenseType")?.let { ExpenseType(it.getString("id"), it.optString("title")) },
                        expenseDate = entry.instantOrNull("expenseDate"),
                        amount = entry.optDouble("amount", 0.0),
                        comment = entry.optString("comment"),
                        images = entry.optJSONArray("images").objects().map { ExpenseImage(it.optString("url"), it.optString("thumbUrl")) }
                    )
                },
                canManage = permissions?.optBoolean("canManage") ?: false,
                canAddEntries = permissions?.optBoolean("canAddEntries") ?: false
            )
        }
    }
}

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }

private fun encode(id: String) = URLEncoder.encode(id, "UTF-8")

// Every call below is staff-only: Bearer token required, 403 `forbidden` for others, admin-only writes.

suspend fun ApiClient.fetchExpenses(page: Int, status: String?): Paged<ExpenseCard> {
    val root = get("/admin/expenses", mapOf("page" to page, "status" to status), authenticated = true)
    val data = root.getJSONArray("data")
    return Paged(
        items = List(data.length()) { ExpenseCard.fromJson(data.getJSONObject(it)) },
        pagination = Pagination.fromJson(root.getJSONObject("pagination"))
    )
}

suspend fun ApiClient.fetchExpenseStatuses(): List<String> {
    val statuses = get("/admin/expenses/statuses", authenticated = true).getJSONObject("data").getJSONArray("statuses")
    return List(statuses.length()) { statuses.getString(it) }
}

suspend fun ApiClient.fetchExpenseTypes(): List<ExpenseType> =
    get("/admin/expense-types", authenticated = true).getJSONObject("data").optJSONArray("expenseTypes").objects()
        .map { ExpenseType(it.getString("id"), it.optString("title")) }

suspend fun ApiClient.fetchExpense(id: String): ExpenseDetail =
    ExpenseDetail.fromJson(get("/admin/expenses/${encode(id)}", authenticated = true).getJSONObject("data").getJSONObject("expense"))

suspend fun ApiClient.deleteExpense(id: String) {
    delete("/admin/expenses/${encode(id)}", authenticated = true)
}

/** Fields of the web add/edit project form; numbers stay strings until the server parses them. */
data class ExpenseProjectForm(
    val projectName: String = "",
    val projectStatus: String = "დასაწყები",
    val publicProjectId: String = "",
    val totalAmount: String = "",
    val totalAmountDollar: String = "",
    val area: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val projectAddress: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val clientId: String = "",
    val sendTelegramNotification: Boolean = false
)

suspend fun ApiClient.createExpense(form: ExpenseProjectForm): ExpenseDetail {
    val body = JSONObject()
        .put("projectName", form.projectName.trim())
        .put("projectStatus", form.projectStatus)
        .put("publicProjectId", form.publicProjectId.trim())
        .put("totalAmount", form.totalAmount.trim())
        .put("totalAmountDollar", form.totalAmountDollar.trim())
        .put("area", form.area.trim())
        // "YYYY-MM-DD", exactly what the web date input posts.
        .put("startDate", form.startDate.toString())
        .put("endDate", form.endDate.toString())
        .put("projectAddress", form.projectAddress.trim())
        .put("clientName", form.clientName.trim())
        .put("clientPhone", form.clientPhone.trim())
        .put("clientEmail", form.clientEmail.trim())
        .put("clientId", form.clientId.trim())
        .put("sendTelegramNotification", form.sendTelegramNotification)
    return ExpenseDetail.fromJson(post("/admin/expenses", body, authenticated = true).getJSONObject("data").getJSONObject("expense"))
}

/** Fields of the web add/edit expense-row form. */
data class ExpenseEntryForm(
    val expenseTypeId: String = "",
    val expenseDate: LocalDate? = LocalDate.now(),
    val amount: String = "",
    val comment: String = "",
    val sendTelegramNotification: Boolean = false
)

/** Adds a row ([entryId] null) or updates one; [images] are new photos (appended to existing ones on edit). */
suspend fun ApiClient.saveExpenseEntry(expenseId: String, entryId: String?, form: ExpenseEntryForm, images: List<UploadFile>): ExpenseDetail {
    val fields = mapOf(
        "expenseType" to form.expenseTypeId,
        "expenseDate" to form.expenseDate?.toString().orEmpty(),
        "amount" to form.amount.trim(),
        "comment" to form.comment.trim(),
        "sendTelegramNotification" to form.sendTelegramNotification.toString()
    )
    val path = "/admin/expenses/${encode(expenseId)}/expenses" + (entryId?.let { "/${encode(it)}" } ?: "")
    val root = multipart(if (entryId == null) "POST" else "PUT", path, fields, mapOf("images" to images), authenticated = true)
    return ExpenseDetail.fromJson(root.getJSONObject("data").getJSONObject("expense"))
}

/** Fields of the web "ჩარიცხვის დამატება" form. */
data class PaidAmountForm(
    val paidAmount: String = "",
    val paidAmountDollar: String = "",
    val paidAt: LocalDate? = LocalDate.now(),
    val comment: String = "",
    val sendTelegramNotification: Boolean = false,
    val sendClientEmail: Boolean = false
)

/** Adds a payment; returns the updated project and whether the client e-mail actually went out. */
suspend fun ApiClient.addPaidAmount(expenseId: String, form: PaidAmountForm): Pair<ExpenseDetail, Boolean> {
    val body = JSONObject()
        .put("paidAmount", form.paidAmount.trim())
        .put("paidAmountDollar", form.paidAmountDollar.trim())
        .put("paidAt", form.paidAt?.toString().orEmpty())
        .put("comment", form.comment.trim())
        .put("sendTelegramNotification", form.sendTelegramNotification)
        .put("sendClientEmail", form.sendClientEmail)
    val data = post("/admin/expenses/${encode(expenseId)}/paid-amounts", body, authenticated = true).getJSONObject("data")
    return ExpenseDetail.fromJson(data.getJSONObject("expense")) to data.optBoolean("clientEmailSent")
}

/**
 * Bumped after any expense write so open screens (list, detail) reload, like the web redirecting
 * back to a freshly rendered page.
 */
object ExpenseChanges {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()
    fun changed() {
        _version.value += 1
    }
}
