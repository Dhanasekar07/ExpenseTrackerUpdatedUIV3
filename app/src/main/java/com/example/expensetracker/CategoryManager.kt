package com.example.expensetracker

import android.content.Context
import android.graphics.Color

data class Category(
    val id        : String,
    val name      : String,
    val iconName  : String,
    val colorHex  : String,
    var isActive  : Boolean = false,
    var sortOrder : Int = 0
) {
    fun color() = try { Color.parseColor(colorHex) } catch (e: Exception) { Color.GRAY }
}

object CategoryManager {

    val CATALOG = listOf(
        Category("cat_01", "House Rent",       "home",                 "#93C5FD"),
        Category("cat_02", "Internet",          "wifi",                 "#7DD3FC"),
        Category("cat_03", "Insurance Premium", "verified_user",        "#A5B4FC"),
        Category("cat_04", "Electricity",       "bolt",                 "#C7D2FE"),
        Category("cat_05", "Gas",               "local_fire_department","#BAE6FD"),
        Category("cat_06", "Food",              "restaurant",           "#FDBA74"),
        Category("cat_07", "Tea/Coffee",        "coffee",               "#CAA47E"),
        Category("cat_08", "Snacks",            "bakery_dining",        "#FDE047"),
        Category("cat_09", "Grocery",           "shopping_cart",        "#FED7AA"),
        Category("cat_10", "Medicine",          "medical_services",     "#FCA5A5"),
        Category("cat_11", "Mutual Funds",      "trending_up",          "#86EFAC"),
        Category("cat_12", "Loan EMI",          "credit_card",          "#A7F3D0"),
        Category("cat_13", "Shopping",          "shopping_bag",         "#D6BBFA"),
        Category("cat_14", "Online Order",      "local_shipping",       "#F472B6"),
        Category("cat_15", "Movies",            "local_movies",         "#E9D5FF"),
        Category("cat_16", "OTT",               "live_tv",              "#FBCFE8"),
        Category("cat_17", "Personal Grooming", "content_cut",          "#FDA4AF"),
        Category("cat_18", "Fuel",              "local_gas_station",    "#5EEAD4"),
        Category("cat_19", "Transport",         "commute",              "#67E8F9")
    )

    val DEFAULT_ACTIVE_IDS = listOf(
        "cat_06", "cat_07", "cat_18", "cat_13", "cat_19", "cat_09"
    )

    private val _active   = mutableListOf<Category>()
    private val _archived = mutableListOf<Category>()
    private var _initialized = false

    val activeCategories   get() = _active.toList()
    val archivedCategories get() = _archived.toList()

    fun initialize(context: Context) {
        // ── FIX: always clear lists before loading to prevent duplicates ──
        _active.clear()
        _archived.clear()

        try {
            val db     = CategoryDbHelper(context)
            val stored = db.getAllCategories()

            if (stored.isEmpty()) {
                // First launch — seed from catalog
                CATALOG.forEach { cat ->
                    val isActive  = cat.id in DEFAULT_ACTIVE_IDS
                    val sortOrder = if (isActive) DEFAULT_ACTIVE_IDS.indexOf(cat.id) else 0
                    val c = cat.copy(isActive = isActive, sortOrder = sortOrder)
                    db.insertCategory(c)
                    if (isActive) _active.add(c) else _archived.add(c)
                }
                _active.sortBy { it.sortOrder }
            } else {
                stored.forEach { cat ->
                    if (cat.isActive) _active.add(cat) else _archived.add(cat)
                }
                _active.sortBy { it.sortOrder }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback — load defaults in-memory even if DB fails
            if (_active.isEmpty()) {
                DEFAULT_ACTIVE_IDS.forEachIndexed { i, id ->
                    CATALOG.find { it.id == id }?.let {
                        _active.add(it.copy(isActive = true, sortOrder = i))
                    }
                }
                CATALOG.filter { it.id !in DEFAULT_ACTIVE_IDS }.forEach {
                    _archived.add(it.copy(isActive = false))
                }
            }
        }

        _initialized = true
    }

    fun getCategoryById(id: String) =
        (_active + _archived).find { it.id == id }

    fun getCategoryByName(name: String) =
        (_active + _archived).find { it.name.equals(name, ignoreCase = true) }

    fun archiveCategory(context: Context, id: String) {
        val cat = _active.find { it.id == id } ?: return
        _active.remove(cat)
        _archived.add(cat.copy(isActive = false))
        try { CategoryDbHelper(context).updateCategoryActive(id, false) } catch (e: Exception) {}
        reorderActive(context)
    }

    fun activateCategory(context: Context, id: String) {
        val cat = _archived.find { it.id == id } ?: return
        _archived.remove(cat)
        val active = cat.copy(isActive = true, sortOrder = _active.size)
        _active.add(active)
        try { CategoryDbHelper(context).updateCategoryActive(id, true) } catch (e: Exception) {}
    }

    fun deleteCategory(context: Context, id: String) {
        _active.removeAll   { it.id == id }
        _archived.removeAll { it.id == id }
        try { CategoryDbHelper(context).deleteCategory(id) } catch (e: Exception) {}
    }

    fun renameCategory(context: Context, id: String, newName: String) {
        val ai = _active.indexOfFirst { it.id == id }
        if (ai >= 0) _active[ai] = _active[ai].copy(name = newName)
        else {
            val ri = _archived.indexOfFirst { it.id == id }
            if (ri >= 0) _archived[ri] = _archived[ri].copy(name = newName)
        }
        try { CategoryDbHelper(context).renameCategory(id, newName) } catch (e: Exception) {}
    }

    fun reorderActive(context: Context) {
        _active.forEachIndexed { i, cat ->
            _active[i] = cat.copy(sortOrder = i)
            try { CategoryDbHelper(context).updateSortOrder(cat.id, i) } catch (e: Exception) {}
        }
    }
}
