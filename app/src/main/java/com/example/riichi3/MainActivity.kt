package com.example.riichi3

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    // State Variables
    private val selectedTiles = mutableListOf<Tile>()
    private val declaredMelds = mutableListOf<DeclaredMeld>()
    private var doraCount = 0
    private var honbaValue = 0 // Displays as 0, 200, 400, etc.

    // Meld Selection Modes State
    private var activeMeldMode: MeldSource? = null
    private val chiTempSelection = mutableListOf<Tile>()
    
    private val scoreActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            resetToDefault()
        }
    }

    // Views
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutCombination: View
    private lateinit var layoutExtraScore: View

    // Board Views
    private lateinit var boardTilesLayout: LinearLayout
    private lateinit var boardMeldsLayout: LinearLayout
    private lateinit var textTileCount: TextView

    // Meld Declaration Buttons
    private lateinit var btnMeldChi: MaterialButton
    private lateinit var btnMeldPon: MaterialButton
    private lateinit var btnMeldKan: MaterialButton
    private lateinit var btnMeldAnkan: MaterialButton

    // Counter Views
    private lateinit var textDoraValue: TextView
    private lateinit var textHonbaValue: TextView

    // Wind Selection Groups
    private lateinit var toggleRoundWind: MaterialButtonToggleGroup
    private lateinit var togglePlayerWind: MaterialButtonToggleGroup

    // Special Condition Toggles
    private lateinit var btnRiichi: MaterialButton
    private lateinit var btnDaburuRiichi: MaterialButton
    private lateinit var btnIppatsu: MaterialButton
    private lateinit var btnHaiteiHoutei: MaterialButton
    private lateinit var btnRinshanKaihou: MaterialButton

    private lateinit var toggleGroupWinType: MaterialButtonToggleGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        tabLayout = findViewById(R.id.tabLayout)
        layoutCombination = findViewById(R.id.layoutCombination)
        layoutExtraScore = findViewById(R.id.layoutExtraScore)

        boardTilesLayout = findViewById(R.id.boardTilesLayout)
        boardMeldsLayout = findViewById(R.id.boardMeldsLayout)
        textTileCount = findViewById(R.id.textTileCount)

        btnMeldChi = findViewById(R.id.btnMeldChi)
        btnMeldPon = findViewById(R.id.btnMeldPon)
        btnMeldKan = findViewById(R.id.btnMeldKan)
        btnMeldAnkan = findViewById(R.id.btnMeldAnkan)

        textDoraValue = findViewById(R.id.textDoraValue)
        textHonbaValue = findViewById(R.id.textHonbaValue)

        toggleRoundWind = findViewById(R.id.toggleRoundWind)
        togglePlayerWind = findViewById(R.id.togglePlayerWind)

        btnRiichi = findViewById(R.id.btnRiichi)
        btnDaburuRiichi = findViewById(R.id.btnDaburuRiichi)
        btnIppatsu = findViewById(R.id.btnIppatsu)
        btnHaiteiHoutei = findViewById(R.id.btnHaiteiHoutei)
        btnRinshanKaihou = findViewById(R.id.btnRinshanKaihou)

        toggleGroupWinType = findViewById(R.id.toggleGroupWinType)

        // Set Up Tab Layout Switcher
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    if (tab.position == 0) {
                        layoutCombination.visibility = View.VISIBLE
                        layoutExtraScore.visibility = View.GONE
                    } else {
                        layoutCombination.visibility = View.GONE
                        layoutExtraScore.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set Up Meld Mode Buttons click listeners
        setUpMeldButtonsListeners()

        // Set Up Tile Selectors Click Listeners
        setUpTileButtonListeners()

        // Set Up Counters Click Listeners
        setUpCounterListeners()

        // Set Up Special Status Mutual Exclusion & States
        setUpSpecialStatusListeners()

        // Calculate Button Trigger
        val btnCalculate = findViewById<MaterialButton>(R.id.btnCalculate)
        btnCalculate.setOnClickListener {
            performCalculation()
        }

        // Initialize Board
        refreshBoard()
        updateCountersText()
        updateSpecialButtonsState()
        updateMeldConstraints()
    }

    private fun setUpMeldButtonsListeners() {
        btnMeldChi.setOnClickListener {
            if (activeMeldMode == MeldSource.CHI) {
                // Clicking again confirms the 3 selected tiles for Chi
                confirmChiSelection()
            } else {
                setMeldMode(MeldSource.CHI, "Modo CHI activo. Selecciona 3 fichas consecutivas del grid y vuelve a pulsar 'CHI' para confirmar.")
            }
        }

        btnMeldPon.setOnClickListener {
            if (activeMeldMode == MeldSource.PON) {
                setMeldMode(null)
            } else {
                setMeldMode(MeldSource.PON, "Modo PON activo. Clica en una ficha para declarar 3 copias idénticas.")
            }
        }

        btnMeldKan.setOnClickListener {
            if (activeMeldMode == MeldSource.KAN) {
                setMeldMode(null)
            } else {
                setMeldMode(MeldSource.KAN, "Modo KAN activo. Clica en una ficha para declarar 4 copias idénticas.")
            }
        }

        btnMeldAnkan.setOnClickListener {
            if (activeMeldMode == MeldSource.KAN_OCULTO) {
                setMeldMode(null)
            } else {
                setMeldMode(MeldSource.KAN_OCULTO, "Modo KAN OCULTO activo. Clica en una ficha para declarar un Kan cerrado.")
            }
        }
    }

    private fun setMeldMode(mode: MeldSource?, toastMsg: String? = null) {
        activeMeldMode = mode
        chiTempSelection.clear()
        
        btnMeldChi.isChecked = (mode == MeldSource.CHI)
        btnMeldPon.isChecked = (mode == MeldSource.PON)
        btnMeldKan.isChecked = (mode == MeldSource.KAN)
        btnMeldAnkan.isChecked = (mode == MeldSource.KAN_OCULTO)

        if (toastMsg != null) {
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmChiSelection() {
        if (chiTempSelection.size != 3) {
            Toast.makeText(this, "Debes seleccionar exactamente 3 fichas para declarar Chi. Actualmente tienes: ${chiTempSelection.size}", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate sequence
        val isSequence = validateChi(chiTempSelection)
        if (!isSequence) {
            Toast.makeText(this, "Secuencia no válida. Deben ser 3 fichas consecutivas del mismo palo (Círculos o Bambúes).", Toast.LENGTH_LONG).show()
            return
        }

        // Add Meld
        declaredMelds.add(DeclaredMeld(MeldSource.CHI, chiTempSelection.sorted()))
        setMeldMode(null)
        refreshBoard()
        updateMeldConstraints()
    }

    private fun validateChi(selection: List<Tile>): Boolean {
        if (selection.size != 3) return false
        val cat = selection[0].type.category
        if (cat != TileCategory.PIN && cat != TileCategory.SOU) return false
        if (selection.any { it.type.category != cat }) return false

        val sortedVals = selection.map { it.type.value }.sorted()
        return sortedVals[1] == sortedVals[0] + 1 && sortedVals[2] == sortedVals[1] + 1
    }

    private fun setUpTileButtonListeners() {
        val buttonMap = mapOf(
            R.id.btnMan1 to TileType.MAN1,
            R.id.btnMan9 to TileType.MAN9,
            R.id.btnWindEast to TileType.EAST,
            R.id.btnWindSouth to TileType.SOUTH,
            R.id.btnWindWest to TileType.WEST,
            R.id.btnWindNorth to TileType.NORTH,
            R.id.btnDragonWhite to TileType.WHITE,
            R.id.btnDragonGreen to TileType.GREEN,
            R.id.btnDragonRed to TileType.RED,

            R.id.btnPin1 to TileType.PIN1,
            R.id.btnPin2 to TileType.PIN2,
            R.id.btnPin3 to TileType.PIN3,
            R.id.btnPin4 to TileType.PIN4,
            R.id.btnPin5 to TileType.PIN5,
            R.id.btnPin6 to TileType.PIN6,
            R.id.btnPin7 to TileType.PIN7,
            R.id.btnPin8 to TileType.PIN8,
            R.id.btnPin9 to TileType.PIN9,

            R.id.btnSou1 to TileType.SOU1,
            R.id.btnSou2 to TileType.SOU2,
            R.id.btnSou3 to TileType.SOU3,
            R.id.btnSou4 to TileType.SOU4,
            R.id.btnSou5 to TileType.SOU5,
            R.id.btnSou6 to TileType.SOU6,
            R.id.btnSou7 to TileType.SOU7,
            R.id.btnSou8 to TileType.SOU8,
            R.id.btnSou9 to TileType.SOU9
        )

        for ((btnId, tileType) in buttonMap) {
            findViewById<ImageButton>(btnId).setOnClickListener {
                addTile(tileType)
            }
        }
    }

    private fun addTile(tileType: TileType) {
        val totalCount = selectedTiles.size + declaredMelds.size * 3
        if (totalCount >= 14 && activeMeldMode != MeldSource.CHI) {
            Toast.makeText(this, "El tablero está lleno (máximo 14 fichas en total).", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate total existing copies of this tile type (closed hand + declared melds + chi temp selection)
        val existingCount = selectedTiles.count { it.type == tileType } +
                declaredMelds.flatMap { it.tiles }.count { it.type == tileType } +
                chiTempSelection.count { it.type == tileType }

        when (activeMeldMode) {
            null -> {
                // Closed Hand Selection Mode
                if (existingCount >= 4) {
                    Toast.makeText(this, "No puedes colocar más de 4 fichas idénticas de '${tileType.displayName}'.", Toast.LENGTH_SHORT).show()
                    return
                }
                selectedTiles.add(Tile(tileType))
                selectedTiles.sort()
                refreshBoard()
            }
            MeldSource.CHI -> {
                // Chi Mode
                if (chiTempSelection.size >= 3) {
                    Toast.makeText(this, "Ya tienes 3 fichas para Chi. Clica de nuevo en 'CHI' para confirmar.", Toast.LENGTH_SHORT).show()
                    return
                }
                if (existingCount >= 4) {
                    Toast.makeText(this, "No puedes colocar más de 4 fichas idénticas de '${tileType.displayName}'.", Toast.LENGTH_SHORT).show()
                    return
                }
                chiTempSelection.add(Tile(tileType))
                Toast.makeText(this, "Seleccionado para Chi: ${chiTempSelection.joinToString { it.type.displayName }}", Toast.LENGTH_SHORT).show()
            }
            MeldSource.PON -> {
                // Pon Mode (3 identical copies)
                if (existingCount + 3 > 4) {
                    Toast.makeText(this, "No quedan suficientes copias disponibles de '${tileType.displayName}' para formar un Pon.", Toast.LENGTH_SHORT).show()
                    return
                }
                val ponMeld = DeclaredMeld(MeldSource.PON, listOf(Tile(tileType), Tile(tileType), Tile(tileType)))
                declaredMelds.add(ponMeld)
                setMeldMode(null)
                refreshBoard()
                updateMeldConstraints()
            }
            MeldSource.KAN -> {
                // Open Kan Mode (4 identical copies)
                if (existingCount + 4 > 4) {
                    Toast.makeText(this, "No quedan suficientes copias disponibles de '${tileType.displayName}' para formar un Kan.", Toast.LENGTH_SHORT).show()
                    return
                }
                val kanMeld = DeclaredMeld(MeldSource.KAN, listOf(Tile(tileType), Tile(tileType), Tile(tileType), Tile(tileType)))
                declaredMelds.add(kanMeld)
                setMeldMode(null)
                refreshBoard()
                updateMeldConstraints()
            }
            MeldSource.KAN_OCULTO -> {
                // Closed Kan Mode (4 identical copies)
                if (existingCount + 4 > 4) {
                    Toast.makeText(this, "No quedan suficientes copias disponibles de '${tileType.displayName}' para formar un Kan Oculto.", Toast.LENGTH_SHORT).show()
                    return
                }
                val ankanMeld = DeclaredMeld(MeldSource.KAN_OCULTO, listOf(Tile(tileType), Tile(tileType), Tile(tileType), Tile(tileType)))
                declaredMelds.add(ankanMeld)
                setMeldMode(null)
                refreshBoard()
                updateMeldConstraints()
            }
        }
    }

    private fun refreshBoard() {
        // 1. Refresh Hand row (closed hand)
        boardTilesLayout.removeAllViews()
        for (tile in selectedTiles) {
            val imageView = ImageView(this)
            val widthPx = (44 * resources.displayMetrics.density).toInt()
            val heightPx = (56 * resources.displayMetrics.density).toInt()
            val marginPx = (3 * resources.displayMetrics.density).toInt()

            val params = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                setMargins(marginPx, 0, marginPx, 0)
            }
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            val resId = resources.getIdentifier(tile.type.drawableName, "drawable", packageName)
            if (resId != 0) {
                imageView.setImageResource(resId)
            }

            if (tile.type == TileType.WHITE) {
                imageView.setBackgroundResource(R.drawable.tile_background_white_dragon)
            } else {
                imageView.setBackgroundResource(R.drawable.tile_background)
            }

            val paddingPx = (3 * resources.displayMetrics.density).toInt()
            imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            imageView.setOnClickListener {
                selectedTiles.remove(tile)
                selectedTiles.sort()
                refreshBoard()
            }
            boardTilesLayout.addView(imageView)
        }

        // 2. Refresh Declared Melds row (open/closed melds)
        boardMeldsLayout.removeAllViews()
        for (meld in declaredMelds) {
            // Container for this specific meld group
            val meldContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padPx = (2 * resources.displayMetrics.density).toInt()
                setPadding(padPx, 0, padPx, 0)
            }

            val containerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins((6 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt(), 0)
            }
            meldContainer.layoutParams = containerParams

            // Render each tile in the meld
            for (i in meld.tiles.indices) {
                val tile = meld.tiles[i]
                val imageView = ImageView(this)
                val widthPx = (36 * resources.displayMetrics.density).toInt()
                val heightPx = (48 * resources.displayMetrics.density).toInt()
                val tileMarginPx = (1 * resources.displayMetrics.density).toInt()

                val tileParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                    setMargins(tileMarginPx, 0, tileMarginPx, 0)
                }
                imageView.layoutParams = tileParams
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER

                // If Closed Kan (Kan Oculto) -> hide the two middle tiles (draw green back)
                if (meld.source == MeldSource.KAN_OCULTO && (i == 1 || i == 2)) {
                    imageView.setBackgroundResource(R.drawable.tile_background_hidden)
                } else {
                    val resId = resources.getIdentifier(tile.type.drawableName, "drawable", packageName)
                    if (resId != 0) {
                        imageView.setImageResource(resId)
                    }
                    if (tile.type == TileType.WHITE) {
                        imageView.setBackgroundResource(R.drawable.tile_background_white_dragon)
                    } else {
                        imageView.setBackgroundResource(R.drawable.tile_background)
                    }
                }

                val tilePaddingPx = (2 * resources.displayMetrics.density).toInt()
                imageView.setPadding(tilePaddingPx, tilePaddingPx, tilePaddingPx, tilePaddingPx)

                meldContainer.addView(imageView)
            }

            // Click container to remove entire meld
            meldContainer.setOnClickListener {
                declaredMelds.remove(meld)
                refreshBoard()
                updateMeldConstraints()
            }

            boardMeldsLayout.addView(meldContainer)
        }

        // 3. Update counter text
        val totalCount = selectedTiles.size + declaredMelds.size * 3
        textTileCount.text = "Fichas: $totalCount / 14"
        
        updateRinshanButtonState()
    }

    private fun setUpCounterListeners() {
        findViewById<MaterialButton>(R.id.btnDoraMinus).setOnClickListener {
            if (doraCount > 0) {
                doraCount--
                updateCountersText()
            }
        }
        findViewById<MaterialButton>(R.id.btnDoraPlus).setOnClickListener {
            doraCount++
            updateCountersText()
        }

        findViewById<MaterialButton>(R.id.btnHonbaMinus).setOnClickListener {
            if (honbaValue > 0) {
                honbaValue -= 200
                updateCountersText()
            }
        }
        findViewById<MaterialButton>(R.id.btnHonbaPlus).setOnClickListener {
            honbaValue += 200
            updateCountersText()
        }
    }

    private fun updateCountersText() {
        textDoraValue.text = doraCount.toString()
        textHonbaValue.text = honbaValue.toString()
    }

    private fun setUpSpecialStatusListeners() {
        toggleGroupWinType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnToggleTsumo) {
                    btnHaiteiHoutei.text = "Haitei"
                } else if (checkedId == R.id.btnToggleRon) {
                    btnHaiteiHoutei.text = "Houtei"
                }
                updateRinshanButtonState()
            }
        }

        // Mutual Exclusion
        btnRiichi.setOnClickListener {
            if (btnRiichi.isChecked) {
                btnDaburuRiichi.isChecked = false
            }
            updateSpecialButtonsState()
        }

        btnDaburuRiichi.setOnClickListener {
            if (btnDaburuRiichi.isChecked) {
                btnRiichi.isChecked = false
            }
            updateSpecialButtonsState()
        }

        btnIppatsu.setOnClickListener {
            updateSpecialButtonsState()
        }
    }

    private fun updateSpecialButtonsState() {
        val riichiActive = btnRiichi.isChecked || btnDaburuRiichi.isChecked
        btnIppatsu.isEnabled = riichiActive
        if (!riichiActive) {
            btnIppatsu.isChecked = false
        }
    }

    private fun updateMeldConstraints() {
        val isHandOpen = declaredMelds.any { it.source != MeldSource.KAN_OCULTO }

        if (isHandOpen) {
            // Disable and uncheck Riichi/DaburuRiichi/Ippatsu
            btnRiichi.isEnabled = false
            btnRiichi.isChecked = false
            btnDaburuRiichi.isEnabled = false
            btnDaburuRiichi.isChecked = false
            btnIppatsu.isEnabled = false
            btnIppatsu.isChecked = false
        } else {
            btnRiichi.isEnabled = true
            btnDaburuRiichi.isEnabled = true
            updateSpecialButtonsState()
        }
        
        updateRinshanButtonState()
    }

    private fun updateRinshanButtonState() {
        val isTsumo = toggleGroupWinType.checkedButtonId == R.id.btnToggleTsumo
        val hasKans = declaredMelds.any { it.source == MeldSource.KAN || it.source == MeldSource.KAN_OCULTO }

        btnRinshanKaihou.isEnabled = isTsumo && hasKans
        if (!(isTsumo && hasKans)) {
            btnRinshanKaihou.isChecked = false
        }
    }

    private fun performCalculation() {
        val totalCount = selectedTiles.size + declaredMelds.size * 3
        if (totalCount != 14) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Mano Incompleta")
                .setMessage("Tu combinación debe tener exactamente 14 fichas en total (fichas cerradas + 3 por cada grupo declarado) antes de poder calcular. Actualmente tienes $totalCount fichas.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val isTsumo = toggleGroupWinType.checkedButtonId == R.id.btnToggleTsumo
        val honbaCount = honbaValue / 200

        val roundWind = when (toggleRoundWind.checkedButtonId) {
            R.id.btnRoundSouth -> TileType.SOUTH
            R.id.btnRoundWest -> TileType.WEST
            R.id.btnRoundNorth -> TileType.NORTH
            else -> TileType.EAST
        }

        val playerWind = when (togglePlayerWind.checkedButtonId) {
            R.id.btnPlayerSouth -> TileType.SOUTH
            R.id.btnPlayerWest -> TileType.WEST
            R.id.btnPlayerNorth -> TileType.NORTH
            else -> TileType.EAST
        }

        val hasRiichi = btnRiichi.isChecked
        val hasDaburuRiichi = btnDaburuRiichi.isChecked
        val hasIppatsu = btnIppatsu.isChecked
        val hasHaiteiOrHoutei = btnHaiteiHoutei.isChecked
        val hasRinshanKaihou = btnRinshanKaihou.isChecked

        // Evaluate Hand using advanced Engine
        val evalResult = MahjongEngine.evaluateHand(
            closedTiles = selectedTiles,
            declaredMelds = declaredMelds,
            isTsumo = isTsumo,
            doraCount = doraCount,
            roundWind = roundWind,
            playerWind = playerWind,
            hasRiichi = hasRiichi,
            hasDaburuRiichi = hasDaburuRiichi,
            hasIppatsu = hasIppatsu,
            hasHaiteiOrHoutei = hasHaiteiOrHoutei,
            hasRinshanKaihou = hasRinshanKaihou
        )

        if (!evalResult.isWin || evalResult.errorMessage != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Combinación Inválida")
                .setMessage(evalResult.errorMessage ?: "La combinación actual no forma una mano ganadora válida en Riichi Mahjong de 3 jugadores.")
                .setPositiveButton("Revisar", null)
                .show()
            return
        }

        val isDealer = playerWind == TileType.EAST
        val scoreDetails = MahjongEngine.getScoreDetails(
            totalHan = evalResult.totalHan,
            isTsumo = isTsumo,
            isDealer = isDealer,
            honbaCount = honbaCount
        )

        // Serialize declared melds
        val meldSources = ArrayList(declaredMelds.map { it.source.name })
        val meldTiles = ArrayList(declaredMelds.map { dm -> dm.tiles.joinToString(",") { it.type.drawableName } })

        // Launch Result Activity
        val intent = Intent(this, ScoreActivity::class.java).apply {
            putStringArrayListExtra("selectedTiles", ArrayList(selectedTiles.map { it.type.drawableName }))
            putStringArrayListExtra("meldSources", meldSources)
            putStringArrayListExtra("meldTiles", meldTiles)
            putExtra("isTsumo", isTsumo)
            putExtra("isDealer", isDealer)
            putExtra("honbaCount", honbaCount)
            putExtra("totalHan", evalResult.totalHan)
            putExtra("isYakuman", evalResult.isYakuman)
            putExtra("yakumanCount", evalResult.yakumanCount)
            
            putStringArrayListExtra("yakuNames", ArrayList(evalResult.yakuList.map { it.first }))
            putIntegerArrayListExtra("yakuHans", ArrayList(evalResult.yakuList.map { it.second }))
            
            putExtra("mainScore", scoreDetails.mainScore)
            putExtra("paymentDescription", scoreDetails.paymentDescription)
            putExtra("paymentDetail1", scoreDetails.paymentDetail1)
            putExtra("paymentDetail2", scoreDetails.paymentDetail2)
        }
        scoreActivityLauncher.launch(intent)
    }

    private fun resetToDefault() {
        selectedTiles.clear()
        declaredMelds.clear()
        doraCount = 0
        honbaValue = 0
        setMeldMode(null)
        
        toggleRoundWind.check(R.id.btnRoundEast)
        togglePlayerWind.check(R.id.btnPlayerEast)
        toggleGroupWinType.check(R.id.btnToggleTsumo)

        btnRiichi.isChecked = false
        btnDaburuRiichi.isChecked = false
        btnIppatsu.isChecked = false
        btnHaiteiHoutei.isChecked = false
        btnRinshanKaihou.isChecked = false

        refreshBoard()
        updateCountersText()
        updateSpecialButtonsState()
        updateMeldConstraints()
    }
}