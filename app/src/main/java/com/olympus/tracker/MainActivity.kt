
package com.olympus.tracker

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    data class Spin(
        val bet: Double,
        val win: Double,
        val scatter: String,
        val multiplier: Double,
        val cascades: Int
    )

    data class Bonus(
        val type: String,
        val initial: Int,
        val extra: Int,
        val multiplier: Double,
        val payout: Double,
        val cost: Double
    )

    private val spins = mutableListOf<Spin>()
    private val bonuses = mutableListOf<Bonus>()
    private var page = "Обзор"
    private lateinit var content: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("olympus_data", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        showApp()
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float = 16f): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
    }

    private fun field(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
    }

    private fun button(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun showApp() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(25, 29, 39))
            setPadding(dp(12), dp(12), dp(12), dp(8))
        }

        root.addView(label("OLYMPUS TRACKER", 25f))
        root.addView(label("Учёт спинов и бонусов", 14f))

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf("Обзор", "Спины", "Бонусы").forEach { tab ->
            tabs.addView(
                button(tab) {
                    page = tab
                    showApp()
                },
                LinearLayout.LayoutParams(0, dp(52), 1f)
            )
        }

        root.addView(tabs)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        setContentView(root)

        when (page) {
            "Обзор" -> overview()
            "Спины" -> spinPage()
            "Бонусы" -> bonusPage()
        }
    }

    private fun overview() {
        val bets = spins.sumOf { it.bet }
        val wins = spins.sumOf { it.win }
        val profit = wins - bets
        val rtp = if (bets > 0) wins / bets * 100 else 0.0

        content.addView(label("ОБЩИЙ ОБЗОР", 23f))
        content.addView(label("Всего спинов: ${spins.size}"))
        content.addView(label("Всего ставок: %.2f".format(bets)))
        content.addView(label("Всего выигрышей: %.2f".format(wins)))
        content.addView(label("Результат: %.2f".format(profit)))
        content.addView(label("RTP: %.2f%%".format(rtp)))
        content.addView(label("Всего бонусов: ${bonuses.size}"))
        content.addView(
            label("Выплаты бонусов: %.2f".format(
                bonuses.sumOf { it.payout }
            ))
        )
        content.addView(
            label("Покупки бонусов: %.2f".format(
                bonuses.sumOf { it.cost }
            ))
        )

        content.addView(label("ПОСЛЕДНИЕ СПИНЫ", 20f))

        spins.takeLast(10).reversed().forEachIndexed { index, s ->
            content.addView(label(
                "Ставка: ${s.bet} | Выигрыш: ${s.win}\n" +
                "Scatter: ${s.scatter} | x${s.multiplier} | " +
                "Каскады: ${s.cascades}"
            ))
        }
    }

    private fun spinPage() {
        content.addView(label("УЧЁТ СПИНОВ", 23f))

        val bet = field("Ставка")
        val win = field("Выигрыш")
        val scatter = field("Scatter: нет / 3 / 4 / 5")
        val mult = field("Множитель (например 12)")
        val cascades = field("Количество каскадов")

        listOf(bet, win, scatter, mult, cascades).forEach {
            content.addView(it)
        }

        content.addView(button("СОХРАНИТЬ СПИН") {
            val b = bet.text.toString().toDoubleOrNull()
            val w = win.text.toString().toDoubleOrNull()

            if (b == null || w == null || b < 0 || w < 0) {
                Toast.makeText(this, "Проверь ставку и выигрыш", Toast.LENGTH_SHORT).show()
                return@button
            }

            spins.add(
                Spin(
                    b, w,
                    scatter.text.toString().ifBlank { "Нет" },
                    mult.text.toString().toDoubleOrNull() ?: 0.0,
                    cascades.text.toString().toIntOrNull() ?: 0
                )
            )

            saveData()
            showApp()
            Toast.makeText(this, "Спин сохранён", Toast.LENGTH_SHORT).show()
        })

        content.addView(label("ИСТОРИЯ СПИНОВ", 20f))

        spins.reversed().forEachIndexed { i, s ->
            content.addView(label(
                "${i + 1}. Ставка ${s.bet} | Выигрыш ${s.win}\n" +
                "Scatter: ${s.scatter} | x${s.multiplier} | " +
                "Каскады: ${s.cascades}"
            ))
        }
    }

    private fun bonusPage() {
        content.addView(label("УЧЁТ БОНУСОВ", 23f))

        val type = field("Обычный бонус / Super Bonus")
        val initial = field("Начальные фриспины")
        val extra = field("Дополнительные фриспины")
        val mult = field("Общий множитель")
        val payout = field("Итоговая выплата")
        val cost = field("Стоимость покупки бонуса")

        listOf(type, initial, extra, mult, payout, cost).forEach {
            content.addView(it)
        }

        content.addView(button("СОХРАНИТЬ БОНУС") {
            val p = payout.text.toString().toDoubleOrNull()

            if (p == null || p < 0) {
                Toast.makeText(this, "Укажи выплату", Toast.LENGTH_SHORT).show()
                return@button
            }

            bonuses.add(
                Bonus(
                    type.text.toString().ifBlank { "Обычный бонус" },
                    initial.text.toString().toIntOrNull() ?: 0,
                    extra.text.toString().toIntOrNull() ?: 0,
                    mult.text.toString().toDoubleOrNull() ?: 0.0,
                    p,
                    cost.text.toString().toDoubleOrNull() ?: 0.0
                )
            )

            saveData()
            showApp()
            Toast.makeText(this, "Бонус сохранён", Toast.LENGTH_SHORT).show()
        })

        content.addView(label("ИСТОРИЯ БОНУСОВ", 20f))

        bonuses.reversed().forEachIndexed { i, b ->
            content.addView(label(
                "${i + 1}. ${b.type}\n" +
                "Фриспины: ${b.initial} + ${b.extra}\n" +
                "Множитель: x${b.multiplier}\n" +
                "Выплата: ${b.payout} | Покупка: ${b.cost}"
            ))
        }
    }

    private fun saveData() {
        val s = JSONArray()
        spins.forEach {
            s.put(JSONObject().apply {
                put("bet", it.bet)
                put("win", it.win)
                put("scatter", it.scatter)
                put("multiplier", it.multiplier)
                put("cascades", it.cascades)
            })
        }

        val b = JSONArray()
        bonuses.forEach {
            b.put(JSONObject().apply {
                put("type", it.type)
                put("initial", it.initial)
                put("extra", it.extra)
                put("multiplier", it.multiplier)
                put("payout", it.payout)
                put("cost", it.cost)
            })
        }

        prefs.edit()
            .putString("spins", s.toString())
            .putString("bonuses", b.toString())
            .apply()
    }

    private fun loadData() {
        try {
            val s = JSONArray(prefs.getString("spins", "[]"))
            for (i in 0 until s.length()) {
                val o = s.getJSONObject(i)
                spins.add(
                    Spin(
                        o.optDouble("bet"),
                        o.optDouble("win"),
                        o.optString("scatter"),
                        o.optDouble("multiplier"),
                        o.optInt("cascades")
                    )
                )
            }

            val b = JSONArray(prefs.getString("bonuses", "[]"))
            for (i in 0 until b.length()) {
                val o = b.getJSONObject(i)
                bonuses.add(
                    Bonus(
                        o.optString("type"),
                        o.optInt("initial"),
                        o.optInt("extra"),
                        o.optDouble("multiplier"),
                        o.optDouble("payout"),
                        o.optDouble("cost")
                    )
                )
            }
        } catch (_: Exception) {
            spins.clear()
            bonuses.clear()
        }
    }
}
