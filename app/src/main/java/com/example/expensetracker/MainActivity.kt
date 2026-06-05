package com.example.expensetracker

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db                  : ExpenseDbHelper
    private lateinit var tvGreeting          : TextView
    private lateinit var tvTotalSpent        : TextView
    private lateinit var pieChart            : PieChartView
    private lateinit var legendContainer     : LinearLayout
    private lateinit var transactionContainer: LinearLayout
    private lateinit var tvNoTxn             : TextView

    private var currentFilter = "day"
    private var customFrom    = 0L
    private var customTo      = 0L
    private val currency get() = AppPreferences.getCurrencySymbol(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply night mode
        AppCompatDelegate.setDefaultNightMode(
            if (AppPreferences.isNightMode(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // If NOT onboarded → go to onboarding, stop here
        if (!AppPreferences.isOnboarded(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        // Set layout
        setContentView(R.layout.activity_main)

        // Bind views
        db                   = ExpenseDbHelper(this)
        tvGreeting           = findViewById(R.id.tvGreeting)
        tvTotalSpent         = findViewById(R.id.tvTotalSpent)
        pieChart             = findViewById(R.id.pieChart)
        legendContainer      = findViewById(R.id.legendContainer)
        transactionContainer = findViewById(R.id.transactionContainer)
        tvNoTxn              = findViewById(R.id.tvNoTransactions)

        // Init categories
        try { CategoryManager.initialize(this) } catch (e: Exception) { e.printStackTrace() }

        TransactionDeduplicator(this).cleanup()
        setupGreeting()
        setupFilterTabs()
        setupBottomNav()
        setupSeeAllButtons()
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        if (!AppPreferences.isOnboarded(this)) return
        setupGreeting()
        refreshDashboard()
    }

    private fun setupGreeting() {
        val name = AppPreferences.getUsername(this)
        tvGreeting.text = if (name.isNotEmpty()) "Hello, $name" else "Hello!"
    }

    private fun setupFilterTabs() {
        mapOf(
            R.id.tabDay to "day", R.id.tabWeek to "week",
            R.id.tabMonth to "month", R.id.tabCustom to "custom"
        ).forEach { (id, f) ->
            findViewById<TextView>(id).setOnClickListener {
                if (f == "custom") showDatePicker()
                else { currentFilter = f; updateTabUI(); refreshDashboard() }
            }
        }
        updateTabUI()
    }

    private fun updateTabUI() {
        listOf(
            R.id.tabDay to "day", R.id.tabWeek to "week",
            R.id.tabMonth to "month", R.id.tabCustom to "custom"
        ).forEach { (id, f) ->
            val tv = findViewById<TextView>(id)
            if (f == currentFilter) {
                tv.setBackgroundResource(R.drawable.bg_filter_active)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundResource(R.drawable.bg_filter_inactive)
                tv.setTextColor(Color.parseColor("#6B7280"))
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val from = Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val to = Calendar.getInstance().apply {
                    set(y2, m2, d2, 23, 59, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                customFrom = from; customTo = to; currentFilter = "custom"
                updateTabUI(); refreshDashboard()
            }, y, m, d).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getFromTs(): Long {
        val cal = Calendar.getInstance()
        return when (currentFilter) {
            "day"    -> { cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0); cal.timeInMillis }
            "week"   -> { cal.add(Calendar.DAY_OF_YEAR,-7); cal.timeInMillis }
            "month"  -> { cal.set(Calendar.DAY_OF_MONTH,1); cal.set(Calendar.HOUR_OF_DAY,0); cal.timeInMillis }
            "custom" -> customFrom
            else     -> 0L
        }
    }

    private fun getToTs() =
        if (currentFilter == "custom") customTo else System.currentTimeMillis()

    private fun refreshDashboard() {
        try {
            val expenses  = db.getExpenses(getFromTs(), getToTs())
            val catTotals = db.getTotalByCategory(getFromTs(), getToTs())
            val total     = expenses.sumOf { it.amount }

            tvTotalSpent.text = "$currency${String.format("%.0f", total)}"

            val slices = catTotals.map { (name, amt) ->
                val cat = CategoryManager.getCategoryByName(name)
                PieSlice(name, amt.toFloat(),
                    try { Color.parseColor(cat?.colorHex ?: "#9CA3AF") }
                    catch (e: Exception) { Color.GRAY })
            }
            pieChart.setData(slices, currency)

            // Top 3 legend
            legendContainer.removeAllViews()
            slices.sortedByDescending { it.value }.take(3).forEach { slice ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity     = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0,0,0,6) }
                }
                val dot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(8,8).apply { setMargins(0,0,8,0) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL; setColor(slice.color)
                    }
                }
                val label = TextView(this).apply {
                    text = "${slice.label}: $currency${String.format("%.0f", slice.value)}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#6B7280"))
                }
                row.addView(dot); row.addView(label)
                legendContainer.addView(row)
            }

            // Transactions
            transactionContainer.removeAllViews()
            if (expenses.isEmpty()) {
                tvNoTxn.visibility = View.VISIBLE
            } else {
                tvNoTxn.visibility = View.GONE
                expenses.take(5).forEach { addTransactionRow(it) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun addTransactionRow(expense: Expense) {
        val cat      = try { CategoryManager.getCategoryByName(expense.category) } catch (e: Exception) { null }
        val colorHex = cat?.colorHex ?: "#F0F2F5"
        val sdf      = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 14, 0, 14)
        }

        val iconBg = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(48,48).apply { setMargins(0,0,12,0) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = 12f
                setColor(try { Color.parseColor(colorHex) } catch (e: Exception) { Color.LTGRAY })
            }
        }
        iconBg.addView(TextView(this).apply {
            text = getCategoryEmoji(expense.category); textSize = 18f
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })

        val info = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = expense.category; textSize = 14f
            setTextColor(Color.parseColor("#111111")); typeface = Typeface.DEFAULT_BOLD
        })
        info.addView(TextView(this).apply {
            text = sdf.format(Date(expense.timestamp)); textSize = 11f
            setTextColor(Color.parseColor("#9CA3AF"))
        })

        val amt = TextView(this).apply {
            text = if (expense.amount > 0) "$currency${String.format("%.0f", expense.amount)}" else "$currency-"
            textSize = 14f; setTextColor(Color.parseColor("#EF4444")); typeface = Typeface.DEFAULT_BOLD
        }

        row.addView(iconBg); row.addView(info); row.addView(amt)
        transactionContainer.addView(row)
        transactionContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#F0F0F0"))
        })
    }

    private fun setupSeeAllButtons() {
        findViewById<LinearLayout>(R.id.btnSeeAllChart).setOnClickListener {
            startActivity(Intent(this, ExpenseBreakdownActivity::class.java).apply {
                putExtra("filter", currentFilter); putExtra("from", customFrom); putExtra("to", customTo)
            })
        }
        findViewById<TextView>(R.id.btnSeeAllTxn).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java).apply {
                putExtra("entry", "stack")
            })
        }
    }

    private fun setupBottomNav() {
        listOf(
            Triple(R.id.navHomePill, R.id.navHomeIcon, R.id.navHomeLabel),
            Triple(R.id.navTransactionsPill, R.id.navTransactionsIcon, R.id.navTransactionsLabel),
            Triple(R.id.navCategoryPill, R.id.navCategoryIcon, R.id.navCategoryLabel),
            Triple(R.id.navSettingsPill, R.id.navSettingsIcon, R.id.navSettingsLabel)
        ).forEach { (p, i, l) ->
            findViewById<LinearLayout>(p).background = null
            findViewById<ImageView>(i).apply { alpha = 0.5f; clearColorFilter() }
            findViewById<TextView>(l).setTextColor(Color.parseColor("#9CA3AF"))
        }
        // Home active
        findViewById<LinearLayout>(R.id.navHomePill).setBackgroundResource(R.drawable.bg_nav_active_pill)
        findViewById<ImageView>(R.id.navHomeIcon).apply { alpha = 1f; setColorFilter(Color.parseColor("#2D6A4F")) }
        findViewById<TextView>(R.id.navHomeLabel).setTextColor(Color.parseColor("#2D6A4F"))

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {}
        findViewById<LinearLayout>(R.id.navTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navCategory).setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun getCategoryEmoji(name: String) = when (name.lowercase()) {
        "food" -> "🍽️"; "tea/coffee" -> "☕"; "fuel" -> "⛽"
        "shopping" -> "🛍️"; "transport" -> "🚌"; "grocery" -> "🛒"
        "medicine" -> "💊"; "movies" -> "🎬"; "ott" -> "📺"
        "snacks" -> "🍪"; "mutual funds" -> "📈"; "loan emi" -> "💳"
        "online order" -> "📦"; "personal grooming" -> "✂️"
        "internet" -> "📶"; "electricity" -> "⚡"; "gas" -> "🔥"
        "house rent" -> "🏠"; "insurance premium" -> "🛡️"
        else -> "💰"
    }
}
