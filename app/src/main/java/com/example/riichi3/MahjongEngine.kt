package com.example.riichi3

enum class MeldType {
    PONG, // Triplet (Koutsu)
    CHOW  // Sequence (Shuntsu)
}

data class Meld(val type: MeldType, val tiles: List<Tile>)

data class HandPartition(
    val melds: List<Meld> = emptyList(),
    val pair: Tile? = null,
    val isChiitoitsu: Boolean = false,
    val chiitoitsuPairs: List<Tile> = emptyList(),
    val isKokushiMusou: Boolean = false
)

data class EvaluationResult(
    val isWin: Boolean,
    val yakuList: List<Pair<String, Int>> = emptyList(),
    val totalHan: Int = 0,
    val isYakuman: Boolean = false,
    val yakumanCount: Int = 0,
    val scoreDetails: ScoreDetails? = null,
    val errorMessage: String? = null
)

data class ScoreDetails(
    val mainScore: Int,
    val paymentDescription: String,
    val paymentDetail1: String, // E.g., "Este paga X"
    val paymentDetail2: String  // E.g., "No Este paga Y"
)

object MahjongEngine {

    // 1. Core hand partitioner supporting declared melds
    fun partitionHand(
        closedTiles: List<Tile>,
        declaredMelds: List<DeclaredMeld>
    ): List<HandPartition> {
        val totalTilesCount = closedTiles.size + declaredMelds.size * 3
        if (totalTilesCount != 14) return emptyList()

        val partitions = mutableListOf<HandPartition>()

        // Chiitoitsu and Kokushi Musou are ONLY valid for fully closed hands (0 declared melds)
        if (declaredMelds.isEmpty()) {
            if (checkKokushiMusou(closedTiles)) {
                partitions.add(HandPartition(isKokushiMusou = true))
            }

            if (checkChiitoitsu(closedTiles)) {
                val sorted = closedTiles.sorted()
                val pairs = mutableListOf<Tile>()
                for (i in 0 until 14 step 2) {
                    pairs.add(sorted[i])
                }
                partitions.add(HandPartition(isChiitoitsu = true, chiitoitsuPairs = pairs))
            }
        }

        // Convert declaredMelds to standard internal Meld structures
        val convertedDeclaredMelds = declaredMelds.map { dm ->
            val type = if (dm.source == MeldSource.CHI) MeldType.CHOW else MeldType.PONG
            // A Kan consists of 4 tiles physically, but for meld matching it functions as a pong of 3 tiles
            Meld(type, dm.tiles.take(3))
        }

        val neededMelds = 4 - declaredMelds.size

        // Regular hands (neededMelds + 1 Pair in closed hand)
        if (neededMelds == 0) {
            // Closed hand must be exactly 2 tiles forming a pair!
            if (closedTiles.size == 2 && closedTiles[0].type == closedTiles[1].type) {
                partitions.add(HandPartition(melds = convertedDeclaredMelds, pair = closedTiles[0]))
            }
        } else {
            val counts = closedTiles.groupBy { it.type }.mapValues { it.value.size }
            for ((type, count) in counts) {
                if (count >= 2) {
                    val remaining = closedTiles.toMutableList()
                    val pairTile = Tile(type)
                    remaining.remove(pairTile)
                    remaining.remove(pairTile)

                    val meldPartitions = mutableListOf<List<Meld>>()
                    findMeldsBacktracking(remaining.sorted(), emptyList(), meldPartitions, neededMelds)

                    for (melds in meldPartitions) {
                        partitions.add(HandPartition(melds = convertedDeclaredMelds + melds, pair = pairTile))
                    }
                }
            }
        }

        return partitions
    }

    private fun checkKokushiMusou(tiles: List<Tile>): Boolean {
        val terminalAndHonorTypes = setOf(
            TileType.MAN1, TileType.MAN9,
            TileType.PIN1, TileType.PIN9,
            TileType.SOU1, TileType.SOU9,
            TileType.EAST, TileType.SOUTH, TileType.WEST, TileType.NORTH,
            TileType.WHITE, TileType.GREEN, TileType.RED
        )
        val uniqueTypesInHand = tiles.map { it.type }.toSet()
        if (uniqueTypesInHand.size != 13) return false
        return tiles.all { it.type in terminalAndHonorTypes }
    }

    private fun checkChiitoitsu(tiles: List<Tile>): Boolean {
        val counts = tiles.groupBy { it.type }.mapValues { it.value.size }
        return counts.size == 7 && counts.values.all { it == 2 }
    }

    private fun findMeldsBacktracking(
        tiles: List<Tile>,
        currentMelds: List<Meld>,
        result: MutableList<List<Meld>>,
        neededMelds: Int
    ) {
        if (tiles.isEmpty()) {
            if (currentMelds.size == neededMelds) {
                result.add(currentMelds)
            }
            return
        }

        val first = tiles[0]

        // Option A: Try to form a Pong (Triplet)
        val countFirst = tiles.count { it.type == first.type }
        if (countFirst >= 3) {
            val remaining = tiles.toMutableList()
            remaining.remove(first)
            remaining.remove(first)
            remaining.remove(first)

            val newMeld = Meld(MeldType.PONG, listOf(first, first, first))
            findMeldsBacktracking(remaining, currentMelds + newMeld, result, neededMelds)
        }

        // Option B: Try to form a Chow (Sequence)
        // Sequences can only be formed within Pin or Sou. No Man sequences in Sanma, no wind/dragon.
        if (first.type.category == TileCategory.PIN || first.type.category == TileCategory.SOU) {
            val val2 = first.type.value + 1
            val val3 = first.type.value + 2

            if (val3 <= 9) {
                val t2 = tiles.find { it.type.category == first.type.category && it.type.value == val2 }
                val t3 = tiles.find { it.type.category == first.type.category && it.type.value == val3 }

                if (t2 != null && t3 != null) {
                    val remaining = tiles.toMutableList()
                    remaining.remove(first)
                    remaining.remove(t2)
                    remaining.remove(t3)

                    val newMeld = Meld(MeldType.CHOW, listOf(first, t2, t3))
                    findMeldsBacktracking(remaining, currentMelds + newMeld, result, neededMelds)
                }
            }
        }
    }

    // 2. Yaku evaluation engine
    fun evaluateHand(
        closedTiles: List<Tile>,
        declaredMelds: List<DeclaredMeld>,
        isTsumo: Boolean,
        doraCount: Int,
        roundWind: TileType,
        playerWind: TileType,
        hasRiichi: Boolean,
        hasDaburuRiichi: Boolean,
        hasIppatsu: Boolean,
        hasHaiteiOrHoutei: Boolean,
        hasRinshanKaihou: Boolean
    ): EvaluationResult {
        val totalTilesCount = closedTiles.size + declaredMelds.size * 3
        if (totalTilesCount != 14) {
            return EvaluationResult(false, errorMessage = "La mano debe tener exactamente 14 fichas en total (cada grupo declarado cuenta por 3).")
        }

        val partitions = partitionHand(closedTiles, declaredMelds)
        if (partitions.isEmpty()) {
            return EvaluationResult(false, errorMessage = "Mano no válida. No forma una combinación ganadora (4 grupos y 1 pareja, 7 parejas, o 13 huérfanos).")
        }

        val allTiles = closedTiles + declaredMelds.flatMap { it.tiles }

        // Evaluate all partitions and pick the one with the highest points
        var bestResult: EvaluationResult? = null

        for (partition in partitions) {
            val result = evaluatePartition(
                partition,
                allTiles,
                declaredMelds,
                isTsumo,
                doraCount,
                roundWind,
                playerWind,
                hasRiichi,
                hasDaburuRiichi,
                hasIppatsu,
                hasHaiteiOrHoutei,
                hasRinshanKaihou
            )

            if (bestResult == null || result.totalHan > bestResult.totalHan ||
                (result.isYakuman && !bestResult.isYakuman) ||
                (result.isYakuman && bestResult.isYakuman && result.yakumanCount > bestResult.yakumanCount)
            ) {
                bestResult = result
            }
        }

        return bestResult ?: EvaluationResult(false, errorMessage = "No se pudo calcular la puntuación.")
    }

    private fun evaluatePartition(
        partition: HandPartition,
        allTiles: List<Tile>,
        declaredMelds: List<DeclaredMeld>,
        isTsumo: Boolean,
        doraCount: Int,
        roundWind: TileType,
        playerWind: TileType,
        hasRiichi: Boolean,
        hasDaburuRiichi: Boolean,
        hasIppatsu: Boolean,
        hasHaiteiOrHoutei: Boolean,
        hasRinshanKaihou: Boolean
    ): EvaluationResult {
        val yakumans = mutableListOf<String>()

        // Check if hand is open (has any declared open Chii, Pon, or open Kan)
        val isHandOpen = declaredMelds.any { it.source != MeldSource.KAN_OCULTO }

        // Helper to check if a pong meld in the partition was declared as an open meld (PON/KAN)
        val isMeldOpen = { meld: Meld ->
            meld.type == MeldType.PONG && declaredMelds.any { dm ->
                (dm.source == MeldSource.PON || dm.source == MeldSource.KAN) &&
                dm.tiles[0].type == meld.tiles[0].type
            }
        }

        // Check Yakumans
        if (partition.isKokushiMusou) {
            yakumans.add("Kokushi Musou (13 Huérfanos)")
        }

        // Daisangen (Big Three Dragons)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val dragonPongs = partition.melds.count {
                it.type == MeldType.PONG && it.tiles[0].type.category == TileCategory.DRAGON
            }
            if (dragonPongs == 3) {
                yakumans.add("Daisangen (Tres Grandes Dragones)")
            }
        }

        // Suuankou (Four Closed Pongs)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val closedPongsCount = partition.melds.count { it.type == MeldType.PONG && !isMeldOpen(it) }
            if (closedPongsCount == 4 && isTsumo) {
                yakumans.add("Suuankou (Cuatro Pon Cerrados)")
            }
        }

        // Shousuushii (Little Four Winds)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val windPongs = partition.melds.count {
                it.type == MeldType.PONG && it.tiles[0].type.category == TileCategory.WIND
            }
            val windPair = partition.pair?.type?.category == TileCategory.WIND
            if (windPongs == 3 && windPair) {
                yakumans.add("Shousuushii (Cuatro Pequeños Vientos)")
            }
        }

        // Daisuushii (Big Four Winds)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val windPongs = partition.melds.count {
                it.type == MeldType.PONG && it.tiles[0].type.category == TileCategory.WIND
            }
            if (windPongs == 4) {
                yakumans.add("Daisuushii (Cuatro Grandes Vientos)")
            }
        }

        // Tsuuiisou (All Honors)
        val allHonors = allTiles.all { it.type.category == TileCategory.WIND || it.type.category == TileCategory.DRAGON }
        if (allHonors) {
            yakumans.add("Tsuuiisou (Todo Honores)")
        }

        // Chinroutou (All Terminals)
        val allTerminals = allTiles.all {
            it.type == TileType.MAN1 || it.type == TileType.MAN9 ||
            it.type == TileType.PIN1 || it.type == TileType.PIN9 ||
            it.type == TileType.SOU1 || it.type == TileType.SOU9
        }
        if (allTerminals) {
            yakumans.add("Chinroutou (Todo Terminales)")
        }

        // Ryuuiisou (All Green)
        val greenTypes = setOf(
            TileType.SOU2, TileType.SOU3, TileType.SOU4,
            TileType.SOU6, TileType.SOU8, TileType.GREEN
        )
        val allGreen = allTiles.all { it.type in greenTypes }
        if (allGreen) {
            yakumans.add("Ryuuiisou (Todo Verdes)")
        }

        // Chuuren Poutou (Nine Gates)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou && !isHandOpen) {
            val suits = allTiles.map { it.type.category }.toSet()
            if (suits.size == 1 && (suits.contains(TileCategory.PIN) || suits.contains(TileCategory.SOU))) {
                val countsByVal = allTiles.groupBy { it.type.value }.mapValues { it.value.size }
                val hasStandardNineGates = (countsByVal[1] ?: 0) >= 3 &&
                                           (countsByVal[2] ?: 0) >= 1 &&
                                           (countsByVal[3] ?: 0) >= 1 &&
                                           (countsByVal[4] ?: 0) >= 1 &&
                                           (countsByVal[5] ?: 0) >= 1 &&
                                           (countsByVal[6] ?: 0) >= 1 &&
                                           (countsByVal[7] ?: 0) >= 1 &&
                                           (countsByVal[8] ?: 0) >= 1 &&
                                           (countsByVal[9] ?: 0) >= 3
                if (hasStandardNineGates) {
                    yakumans.add("Chuuren Poutou (Nueve Puertas)")
                }
            }
        }

        // Process Yakuman score if any detected
        if (yakumans.isNotEmpty()) {
            val yakuList = yakumans.map { it to 13 }
            val count = yakumans.size
            return EvaluationResult(
                isWin = true,
                yakuList = yakuList,
                totalHan = count * 13,
                isYakuman = true,
                yakumanCount = count
            )
        }

        // Standard Hand Evaluation (Normal Yakus)
        val normalYakus = mutableListOf<Pair<String, Int>>()

        // 1. Riichi & Daburu Riichi (Requires Closed Hand)
        if (!isHandOpen) {
            if (hasDaburuRiichi) {
                normalYakus.add("Daburu Riichi" to 2)
            } else if (hasRiichi) {
                normalYakus.add("Riichi" to 1)
            }

            // 2. Ippatsu
            if (hasIppatsu && (hasRiichi || hasDaburuRiichi)) {
                normalYakus.add("Ippatsu" to 1)
            }
        }

        // 3. Menzen Tsumo (Requires Closed Hand)
        if (isTsumo && !isHandOpen) {
            normalYakus.add("Tsumo" to 1)
        }

        // 4. Haitei Raoyue / Houtei Yui
        if (hasHaiteiOrHoutei) {
            if (isTsumo) {
                normalYakus.add("Haitei" to 1)
            } else {
                normalYakus.add("Houtei" to 1)
            }
        }

        // 5. Rinshan Kaihou (+1 Han, Tsumo Only, requires Kan)
        if (hasRinshanKaihou && isTsumo && declaredMelds.any { it.source == MeldSource.KAN || it.source == MeldSource.KAN_OCULTO }) {
            normalYakus.add("Rinshan Kaihou" to 1)
        }

        // 6. Tanyao (All Simples)
        val simpleCategories = setOf(TileCategory.PIN, TileCategory.SOU)
        val isTanyao = allTiles.all { it.type.category in simpleCategories && it.type.value in 2..8 }
        if (isTanyao) {
            normalYakus.add("Tanyao (Sin Terminales ni Honores)" to 1)
        }

        // 7. Chiitoitsu (Seven Pairs - naturally closed only)
        if (partition.isChiitoitsu) {
            normalYakus.add("Chiitoitsu (7 Parejas)" to 2)
        }

        // 8. Pinfu (Flat Hand - Requires Closed Hand)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou && !isHandOpen) {
            val allChows = partition.melds.all { it.type == MeldType.CHOW }
            val pairValueless = partition.pair != null &&
                                partition.pair.type.category != TileCategory.DRAGON &&
                                partition.pair.type != roundWind &&
                                partition.pair.type != playerWind
            if (allChows && pairValueless) {
                normalYakus.add("Pinfu" to 1)
            }
        }

        // 9. Iipeiko (Pure Double Sequence - Requires Closed Hand)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou && !isHandOpen) {
            val chows = partition.melds.filter { it.type == MeldType.CHOW }
            var pairCount = 0
            val checked = mutableListOf<Int>()
            for (i in chows.indices) {
                if (i in checked) continue
                for (j in i + 1 until chows.size) {
                    if (j in checked) continue
                    if (chows[i].tiles[0].type == chows[j].tiles[0].type) {
                        pairCount++
                        checked.add(i)
                        checked.add(j)
                        break
                    }
                }
            }
            if (pairCount == 1) {
                normalYakus.add("Iipeikou (Doble Secuencia Pura)" to 1)
            } else if (pairCount == 2) {
                // If it is two sets of double sequences, it is Ryanpeiko (3 Han) instead of Iipeiko!
                normalYakus.add("Ryanpeikou (Doble Secuencia Pura x2)" to 3)
            }
        }

        // 10. Toitoi (All Pongs)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val pongCount = partition.melds.count { it.type == MeldType.PONG }
            if (pongCount == 4) {
                normalYakus.add("Toitoi (Todo Pon)" to 2)
            }
        }

        // 11. Sanankou (Three Closed Pongs)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val closedPongsCount = partition.melds.count { it.type == MeldType.PONG && !isMeldOpen(it) }
            if (closedPongsCount == 3 || (closedPongsCount == 4 && !isTsumo)) {
                normalYakus.add("Sanankou (3 Pon Ocultos)" to 2)
            }
        }

        // 12. Yakuhai (Dragons & Winds)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            for (meld in partition.melds) {
                if (meld.type == MeldType.PONG) {
                    val tileType = meld.tiles[0].type
                    if (tileType.category == TileCategory.DRAGON) {
                        normalYakus.add("Yakuhai (${tileType.displayName})" to 1)
                    } else if (tileType.category == TileCategory.WIND) {
                        if (tileType == roundWind) {
                            normalYakus.add("Viento de Ronda (${tileType.displayName})" to 1)
                        }
                        if (tileType == playerWind) {
                            normalYakus.add("Viento de Jugador (${tileType.displayName})" to 1)
                        }
                    }
                }
            }
        }

        // 13. Shousangen (Little Three Dragons)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val dragonPongs = partition.melds.count {
                it.type == MeldType.PONG && it.tiles[0].type.category == TileCategory.DRAGON
            }
            val dragonPair = partition.pair?.type?.category == TileCategory.DRAGON
            if (dragonPongs == 2 && dragonPair) {
                normalYakus.add("Shousangen (3 Pequeños Dragones)" to 2)
            }
        }

        // 14. Itsu / Ikkitsukan (Straight - Kuichigi: 2 Han closed, 1 Han open)
        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val chows = partition.melds.filter { it.type == MeldType.CHOW }
            val suits = setOf(TileCategory.PIN, TileCategory.SOU)
            var hasItsu = false
            for (suit in suits) {
                val has123 = chows.any { it.tiles[0].type.category == suit && it.tiles[0].type.value == 1 }
                val has456 = chows.any { it.tiles[0].type.category == suit && it.tiles[0].type.value == 4 }
                val has789 = chows.any { it.tiles[0].type.category == suit && it.tiles[0].type.value == 7 }
                if (has123 && has456 && has789) {
                    hasItsu = true
                    break
                }
            }
            if (hasItsu) {
                val han = if (isHandOpen) 1 else 2
                normalYakus.add("Itsu (Gran Secuencia Pura)" to han)
            }
        }

        // 15. Chanta (Half Outside - Kuichigi: 2 Han closed, 1 Han open)
        val isTerminalOrHonor = { t: TileType ->
            t == TileType.MAN1 || t == TileType.MAN9 ||
            t == TileType.PIN1 || t == TileType.PIN9 ||
            t == TileType.SOU1 || t == TileType.SOU9 ||
            t.category == TileCategory.WIND || t.category == TileCategory.DRAGON
        }
        val isHonor = { t: TileType -> t.category == TileCategory.WIND || t.category == TileCategory.DRAGON }

        var hasHonorsInHand = allTiles.any { isHonor(it.type) }
        var hasSimpleInHand = allTiles.any { !isTerminalOrHonor(it.type) }

        if (!partition.isChiitoitsu && !partition.isKokushiMusou) {
            val allMeldsChanta = partition.melds.all { meld ->
                meld.tiles.any { isTerminalOrHonor(it.type) }
            }
            val pairChanta = partition.pair != null && isTerminalOrHonor(partition.pair.type)
            
            if (allMeldsChanta && pairChanta && hasHonorsInHand && hasSimpleInHand) {
                val han = if (isHandOpen) 1 else 2
                normalYakus.add("Chanta (Terminales u Honores en todos)" to han)
            }
            
            // 16. Junchan (Pure Outside - Kuichigi: 3 Han closed, 2 Han open)
            if (allMeldsChanta && pairChanta && !hasHonorsInHand && hasSimpleInHand) {
                val han = if (isHandOpen) 2 else 3
                normalYakus.add("Junchan (Terminales en todos)" to han)
            }
        }

        // 17. Honroutou (All Terminals and Honors - 2 Han)
        val onlyTerminalsAndHonors = allTiles.all { isTerminalOrHonor(it.type) }
        if (onlyTerminalsAndHonors && !allHonors && !allTerminals) {
            if (partition.isChiitoitsu || partition.melds.all { it.type == MeldType.PONG }) {
                normalYakus.add("Honroutou (Todo Terminales y Honores)" to 2)
            }
        }

        // 18. Honitsu (Half Flush - Kuichigi: 3 Han closed, 2 Han open)
        val suitsPresent = allTiles.map { it.type.category }.filter { it == TileCategory.PIN || it == TileCategory.SOU }.toSet()
        val hasManzu = allTiles.any { it.type.category == TileCategory.MAN }
        
        if (suitsPresent.size == 1 && !hasManzu && hasHonorsInHand) {
            val han = if (isHandOpen) 2 else 3
            normalYakus.add("Honitsu (Color y Honores)" to han)
        }

        // 19. Chinitsu (Full Flush - Kuichigi: 6 Han closed, 5 Han open)
        if (suitsPresent.size == 1 && !hasManzu && !hasHonorsInHand) {
            val han = if (isHandOpen) 5 else 6
            normalYakus.add("Chinitsu (Color)" to han)
        }

        // Check if hand has at least one Yaku
        if (normalYakus.isEmpty()) {
            return EvaluationResult(
                isWin = false,
                errorMessage = "Sin Yaku. No se puede ganar la mano sin al menos 1 Yaku (Dora no cuenta como Yaku inicial)."
            )
        }

        // Calculate sum of normal Yakus
        val normalHanSum = normalYakus.sumOf { it.second }

        // Add Dora count
        val finalYakuList = normalYakus.toMutableList()
        if (doraCount > 0) {
            finalYakuList.add("Dora" to doraCount)
        }

        val totalHan = normalHanSum + doraCount

        return EvaluationResult(
            isWin = true,
            yakuList = finalYakuList,
            totalHan = totalHan,
            isYakuman = false
        )
    }

    // 3. Point lookup and dealer calculation
    fun getScoreDetails(
        totalHan: Int,
        isTsumo: Boolean,
        isDealer: Boolean, // Dealer (Este) vs Non-Dealer (No Este)
        honbaCount: Int
    ): ScoreDetails {
        val rawHan = totalHan
        // Cap standard Han at 13+
        val cappedHan = if (rawHan > 13) 13 else rawHan

        val mainScore: Int
        val desc: String
        val detail1: String
        val detail2: String

        val honbaTotalPoints = honbaCount * 200

        if (isDealer) {
            // --- DEALER (ESTE) ---
            if (isTsumo) {
                val base = when (cappedHan) {
                    1 -> 500
                    2 -> 1000
                    3 -> 2000
                    4 -> 3900
                    5 -> 4000
                    6, 7 -> 6000
                    8, 9, 10 -> 8000
                    11, 12 -> 12000
                    else -> 16000 // 13+ Han (Yakuman)
                }
                val eachPay = base + (honbaCount * 100)
                mainScore = eachPay * 2
                desc = "Tsumo (Este)"
                detail1 = "Cada jugador (No Este) paga: ${eachPay.formatPoints()} pts"
                detail2 = "Desglose: ${base.formatPoints()} base + ${(honbaCount * 100).formatPoints()} Honba"
            } else {
                val base = when (cappedHan) {
                    1 -> 1500
                    2 -> 2900
                    3 -> 5800
                    4 -> 11600
                    5 -> 12000
                    6, 7 -> 18000
                    8, 9, 10 -> 24000
                    11, 12 -> 36000
                    else -> 48000 // 13+ Han (Yakuman)
                }
                mainScore = base + honbaTotalPoints
                desc = "Ron (Este)"
                detail1 = "El jugador descartador paga: ${mainScore.formatPoints()} pts"
                detail2 = "Desglose: ${base.formatPoints()} base + ${honbaTotalPoints.formatPoints()} Honba"
            }
        } else {
            // --- NON-DEALER (NO ESTE) ---
            if (isTsumo) {
                val (dealerPayBase, nonDealerPayBase) = when (cappedHan) {
                    1 -> Pair(500, 300)
                    2 -> Pair(1000, 500)
                    3 -> Pair(2000, 1000)
                    4 -> Pair(3900, 2000)
                    5 -> Pair(4000, 2000)
                    6, 7 -> Pair(6000, 3000)
                    8, 9, 10 -> Pair(8000, 4000)
                    11, 12 -> Pair(12000, 6000)
                    else -> Pair(16000, 8000) // 13+ Han (Yakuman)
                }

                val dealerPay = dealerPayBase + (honbaCount * 100)
                val nonDealerPay = nonDealerPayBase + (honbaCount * 100)
                mainScore = dealerPay + nonDealerPay

                desc = "Tsumo (No Este)"
                detail1 = "El jugador del Este (Dealer) paga: ${dealerPay.formatPoints()} pts"
                detail2 = "El otro jugador (No Este) paga: ${nonDealerPay.formatPoints()} pts"
            } else {
                val base = when (cappedHan) {
                    1 -> 1000
                    2 -> 2000
                    3 -> 3900
                    4 -> 7700
                    5 -> 8000
                    6, 7 -> 12000
                    8, 9, 10 -> 16000
                    11, 12 -> 24000
                    else -> 32000 // 13+ Han (Yakuman)
                }
                mainScore = base + honbaTotalPoints
                desc = "Ron (No Este)"
                detail1 = "El jugador descartador paga: ${mainScore.formatPoints()} pts"
                detail2 = "Desglose: ${base.formatPoints()} base + ${honbaTotalPoints.formatPoints()} Honba"
            }
        }

        return ScoreDetails(mainScore, desc, detail1, detail2)
    }

    private fun Int.formatPoints(): String {
        return "%,d".format(this).replace(",", ".")
    }
}
