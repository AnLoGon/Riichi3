package com.example.riichi3

import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        // 1. Unpack data from Intent
        val selectedTiles = intent.getStringArrayListExtra("selectedTiles") ?: arrayListOf()
        val meldSources = intent.getStringArrayListExtra("meldSources") ?: arrayListOf()
        val meldTiles = intent.getStringArrayListExtra("meldTiles") ?: arrayListOf()
        val isTsumo = intent.getBooleanExtra("isTsumo", false)
        val isDealer = intent.getBooleanExtra("isDealer", false)
        val honbaCount = intent.getIntExtra("honbaCount", 0)
        val totalHan = intent.getIntExtra("totalHan", 0)
        val isYakuman = intent.getBooleanExtra("isYakuman", false)
        val yakumanCount = intent.getIntExtra("yakumanCount", 0)

        val yakuNames = intent.getStringArrayListExtra("yakuNames") ?: arrayListOf()
        val yakuHans = intent.getIntegerArrayListExtra("yakuHans") ?: arrayListOf()

        val mainScore = intent.getIntExtra("mainScore", 0)
        val paymentDescription = intent.getStringExtra("paymentDescription") ?: ""
        val paymentDetail1 = intent.getStringExtra("paymentDetail1") ?: ""
        val paymentDetail2 = intent.getStringExtra("paymentDetail2") ?: ""

        // 2. Bind Views
        val winningTilesLayout = findViewById<LinearLayout>(R.id.winningTilesLayout)
        val textWinTypeLabel = findViewById<TextView>(R.id.textWinTypeLabel)
        val textPointsTotal = findViewById<TextView>(R.id.textPointsTotal)
        val textHanCountLabel = findViewById<TextView>(R.id.textHanCountLabel)
        val textPaymentDetail1 = findViewById<TextView>(R.id.textPaymentDetail1)
        val textPaymentDetail2 = findViewById<TextView>(R.id.textPaymentDetail2)
        val containerYakusList = findViewById<LinearLayout>(R.id.containerYakusList)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        // 3. Render winning hand tiles on the static board
        winningTilesLayout.removeAllViews()
        for (tileDrawableName in selectedTiles) {
            val imageView = ImageView(this)
            
            // Sizing: 36dp width, 48dp height with small margins for compactness
            val widthPx = (36 * resources.displayMetrics.density).toInt()
            val heightPx = (48 * resources.displayMetrics.density).toInt()
            val marginPx = (2 * resources.displayMetrics.density).toInt()

            val params = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                setMargins(marginPx, 0, marginPx, 0)
            }
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            // Load Drawable Resource
            val resId = resources.getIdentifier(tileDrawableName, "drawable", packageName)
            if (resId != 0) {
                imageView.setImageResource(resId)
            }

            // Set rounded physical tile background
            if (tileDrawableName == "white_dragon") {
                imageView.setBackgroundResource(R.drawable.tile_background_white_dragon)
            } else {
                imageView.setBackgroundResource(R.drawable.tile_background)
            }

            // Add padding so the tile symbol sits nicely within the white tile card
            val paddingPx = (2 * resources.displayMetrics.density).toInt()
            imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            winningTilesLayout.addView(imageView)
        }

        // Render declared melds
        for (idx in meldSources.indices) {
            val sourceName = meldSources[idx]
            val tilesCsv = meldTiles[idx]
            val tileNames = tilesCsv.split(",")

            val meldContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padPx = (2 * resources.displayMetrics.density).toInt()
                setPadding(padPx, 0, padPx, 0)
            }

            val containerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins((4 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt(), 0)
            }
            meldContainer.layoutParams = containerParams

            for (i in tileNames.indices) {
                val tileDrawableName = tileNames[i]
                val imageView = ImageView(this)
                // Melds rendered slightly smaller for distinct visual clustering
                val widthPx = (30 * resources.displayMetrics.density).toInt()
                val heightPx = (40 * resources.displayMetrics.density).toInt()
                val tileMarginPx = (1 * resources.displayMetrics.density).toInt()

                val tileParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                    setMargins(tileMarginPx, 0, tileMarginPx, 0)
                }
                imageView.layoutParams = tileParams
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER

                if (sourceName == "KAN_OCULTO" && (i == 1 || i == 2)) {
                    imageView.setBackgroundResource(R.drawable.tile_background_hidden)
                } else {
                    val resId = resources.getIdentifier(tileDrawableName, "drawable", packageName)
                    if (resId != 0) {
                        imageView.setImageResource(resId)
                    }
                    if (tileDrawableName == "white_dragon") {
                        imageView.setBackgroundResource(R.drawable.tile_background_white_dragon)
                    } else {
                        imageView.setBackgroundResource(R.drawable.tile_background)
                    }
                }

                val tilePaddingPx = (1.5 * resources.displayMetrics.density).toInt()
                imageView.setPadding(tilePaddingPx, tilePaddingPx, tilePaddingPx, tilePaddingPx)

                meldContainer.addView(imageView)
            }

            winningTilesLayout.addView(meldContainer)
        }

        // 4. Set Score & Win Type Headings
        val prefix = if (isYakuman) {
            if (yakumanCount > 1) "$yakumanCount x YAKUMAN " else "YAKUMAN "
        } else ""
        
        val typeStr = if (isTsumo) "TSUMO" else "RON"
        val dealerStr = if (isDealer) "(ESTE)" else "(NO ESTE)"
        textWinTypeLabel.text = "$prefix$typeStr $dealerStr"

        textPointsTotal.text = "${formatPoints(mainScore)} pts"

        val tierName = when {
            isYakuman || totalHan >= 13 -> "Yakuman"
            totalHan in 11..12 -> "Sanbaiman"
            totalHan in 8..10 -> "Baiman"
            totalHan in 6..7 -> "Haneman"
            totalHan == 5 -> "Mangan"
            else -> ""
        }
        val hanStr = if (isYakuman) "Yakuman" else "$totalHan Han"
        val tierStr = if (tierName.isNotEmpty()) " ($tierName)" else ""
        val honbaStr = "$honbaCount Honba"
        textHanCountLabel.text = "Total: $hanStr$tierStr - $honbaStr"

        // 5. Set Payment Descriptions
        textPaymentDetail1.text = paymentDetail1
        textPaymentDetail2.text = paymentDetail2

        // 6. Populate Yaku List dynamically
        containerYakusList.removeAllViews()
        for (i in yakuNames.indices) {
            val name = yakuNames[i]
            val hanVal = yakuHans[i]

            // Create row container
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
            }

            // Left text: Yaku name
            val textName = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                text = name
                setTextColor(0xFFE2E8F0.toInt()) // #E2E8F0
                textSize = 14f
            }

            // Right text: Han count
            val textHan = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = if (isYakuman && hanVal == 13) "Yakuman" else "+$hanVal Han"
                setTextColor(0xFF34D399.toInt()) // #34D399
                textSize = 14f
                paint.isFakeBoldText = true
            }

            row.addView(textName)
            row.addView(textHan)
            containerYakusList.addView(row)

            // Add thin divider except for last item
            if (i < yakuNames.size - 1) {
                val divider = android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * resources.displayMetrics.density).toInt()
                    )
                    setBackgroundColor(0xFF334155.toInt()) // #334155
                }
                containerYakusList.addView(divider)
            }
        }

        // Handle empty list case (should not happen, but for safety)
        if (yakuNames.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "Sin combinaciones de Yaku especiales."
                setTextColor(0xFF94A3B8.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
            }
            containerYakusList.addView(emptyTextView)
        }

        // 7. Finish activity and go back
        btnBack.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun formatPoints(points: Int): String {
        return "%,d".format(points).replace(",", ".")
    }
}
