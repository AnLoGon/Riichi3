package com.example.riichi3

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {

    // Helper to create tiles list easily
    private fun createHand(vararg types: TileType): List<Tile> {
        return types.map { Tile(it) }
    }

    @Test
    fun testTanyaoPinfuHand() {
        // Tanyao (All Simples) + Pinfu (Flat Hand)
        // PIN: 2-3-4, 5-6-7
        // SOU: 3-4-5, 6-7-8
        // Pair: SOU 2
        val tiles = createHand(
            TileType.PIN2, TileType.PIN3, TileType.PIN4,
            TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.SOU3, TileType.SOU4, TileType.SOU5,
            TileType.SOU6, TileType.SOU7, TileType.SOU8,
            TileType.SOU2, TileType.SOU2
        )

        // Ron win, round wind East, player wind South, no extra statuses, 0 dora
        val result = MahjongEngine.evaluateHand(
            closedTiles = tiles,
            declaredMelds = emptyList(),
            isTsumo = false,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false
        )

        assertTrue("Should be a valid winning hand", result.isWin)
        assertNull("Should not have error message", result.errorMessage)
        
        val yakuNames = result.yakuList.map { it.first }
        assertTrue("Should contain Tanyao", yakuNames.any { it.contains("Tanyao") })
        assertTrue("Should contain Pinfu", yakuNames.any { it.contains("Pinfu") })
        assertEquals("Should have exactly 2 Han total", 2, result.totalHan)
        assertFalse("Should not be a Yakuman", result.isYakuman)
    }

    @Test
    fun testChiitoitsuHand() {
        // Chiitoitsu (Seven Pairs)
        // 2x 1p, 2x 3p, 2x 5p, 2x 7p, 2x 9p, 2x 1s, 2x 9s
        val tiles = createHand(
            TileType.PIN1, TileType.PIN1,
            TileType.PIN3, TileType.PIN3,
            TileType.PIN5, TileType.PIN5,
            TileType.PIN7, TileType.PIN7,
            TileType.PIN9, TileType.PIN9,
            TileType.SOU1, TileType.SOU1,
            TileType.SOU9, TileType.SOU9
        )

        val result = MahjongEngine.evaluateHand(
            closedTiles = tiles,
            declaredMelds = emptyList(),
            isTsumo = false,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false
        )

        println("DEBUG CHIITOITSU: isWin = ${result.isWin}, yakuList = ${result.yakuList}, error = ${result.errorMessage}")
        assertTrue(result.isWin)
        val yakuNames = result.yakuList.map { it.first }
        assertTrue("Should contain Chiitoitsu", yakuNames.any { it.contains("Chiitoitsu") })
        assertEquals("Should have 2 Han total", 2, result.totalHan)
    }

    @Test
    fun testKokushiMusouHand() {
        // Kokushi Musou (13 Orphans)
        val tiles = createHand(
            TileType.MAN1, TileType.MAN9,
            TileType.PIN1, TileType.PIN9,
            TileType.SOU1, TileType.SOU9,
            TileType.EAST, TileType.SOUTH, TileType.WEST, TileType.NORTH,
            TileType.WHITE, TileType.GREEN, TileType.RED,
            TileType.RED // duplicate
        )

        val result = MahjongEngine.evaluateHand(
            closedTiles = tiles,
            declaredMelds = emptyList(),
            isTsumo = false,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false
        )

        assertTrue(result.isWin)
        assertTrue("Should be flagged as Yakuman", result.isYakuman)
        val yakuNames = result.yakuList.map { it.first }
        assertTrue("Should contain Kokushi Musou", yakuNames.any { it.contains("Kokushi Musou") })
        assertEquals(13, result.totalHan)
    }

    @Test
    fun testNoYakuHand() {
        // Chow 3-4-5p, Pong 1p, Chow 3-4-5s, Chow 6-7-8s, Pair 8p
        // Contains terminal 1p, so no Tanyao.
        // Contains Pong of 1p, so no Pinfu.
        // Contains Chow 3-4-5p which has no terminal or honors, so no Chanta.
        // No value wind/dragon pong.
        // Thus, absolutely 0 Yaku!
        val tiles = createHand(
            TileType.PIN3, TileType.PIN4, TileType.PIN5,
            TileType.PIN1, TileType.PIN1, TileType.PIN1,
            TileType.SOU3, TileType.SOU4, TileType.SOU5,
            TileType.SOU6, TileType.SOU7, TileType.SOU8,
            TileType.PIN8, TileType.PIN8
        )

        val result = MahjongEngine.evaluateHand(
            closedTiles = tiles,
            declaredMelds = emptyList(),
            isTsumo = false, // Ron
            doraCount = 2, // Has Dora, but no Yaku!
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false
        )

        // It should identify it is a valid shape but fails because "Sin Yaku" (No Yaku)
        assertFalse("Should fail winning rules because of no Yaku", result.isWin)
        assertNotNull("Should have an error message indicating no Yaku", result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Sin Yaku"))
    }

    @Test
    fun testPointsCalculation() {
        // 1. Tsumo Non-Dealer, 1 Han, 0 Honba
        // Table: Tsumo (No Este) 1 Han = 500 (Dealer pays) / 300 (Non-dealer pays). Total points = 800.
        val score1 = MahjongEngine.getScoreDetails(
            totalHan = 1,
            isTsumo = true,
            isDealer = false,
            honbaCount = 0
        )
        assertEquals(800, score1.mainScore)
        assertTrue(score1.paymentDetail1.contains("500"))
        assertTrue(score1.paymentDetail2.contains("300"))

        // 2. Ron Dealer, 3 Han, 2 Honba
        // Table: Ron (Este) 3 Han = 5.800 pts.
        // Plus 2 Honba * 200 = 400 pts.
        // Total points = 6.200 pts.
        val score2 = MahjongEngine.getScoreDetails(
            totalHan = 3,
            isTsumo = false,
            isDealer = true,
            honbaCount = 2
        )
        assertEquals(6200, score2.mainScore)

        // 3. Tsumo Dealer, Yakuman (13 Han), 1 Honba
        // Table: Tsumo (Este) 13+ Han = 16.000 pts each.
        // Plus 1 Honba * 100 each = 16.100 pts. Total points = 32.200 pts.
        val score3 = MahjongEngine.getScoreDetails(
            totalHan = 13,
            isTsumo = true,
            isDealer = true,
            honbaCount = 1
        )
        assertEquals(32200, score3.mainScore)
        assertTrue(score3.paymentDetail1.contains("16.100"))
    }

    @Test
    fun testOpenHandWithMeldScoringChinitsuAndNoTsumo() {
        // Closed Hand portion: Pin 2, Pin 3, Pin 4, Pin 5, Pin 6, Pin 7, Pin 8, Pin 8 (8 tiles)
        // Declared open melds:
        // 1. Chi: Pin 1-2-3 (3 tiles)
        // 2. Pon: Pin 9-9-9 (3 tiles)
        // Total virtual tiles = 8 + 3 + 3 = 14 tiles
        // Category is only PIN -> Chinitsu (Full Flush) Yaku.
        // Because of open melds:
        // - Chinitsu is open (Kuichigi reduction): 6 Han -> 5 Han.
        // - Even if it is Tsumo, Menzen Tsumo Yaku (Tsumo 1 Han) is NOT awarded.
        val closedTiles = createHand(
            TileType.PIN2, TileType.PIN3, TileType.PIN4,
            TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.PIN8, TileType.PIN8
        )

        val declaredMelds = listOf(
            DeclaredMeld(MeldSource.CHI, createHand(TileType.PIN1, TileType.PIN2, TileType.PIN3)),
            DeclaredMeld(MeldSource.PON, createHand(TileType.PIN9, TileType.PIN9, TileType.PIN9))
        )

        val result = MahjongEngine.evaluateHand(
            closedTiles = closedTiles,
            declaredMelds = declaredMelds,
            isTsumo = true, // Tsumo win, but hand is open!
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false
        )

        assertTrue("Should be a winning hand", result.isWin)
        val yakuNames = result.yakuList.map { it.first }
        
        // Check that Chinitsu is present and has 5 Han (not 6)
        assertTrue("Should have Chinitsu", yakuNames.any { it.contains("Chinitsu") })
        val chinitsuHan = result.yakuList.first { it.first.contains("Chinitsu") }.second
        assertEquals("Chinitsu should be reduced to 5 Han in open hand", 5, chinitsuHan)
        
        // Check that "Tsumo" is NOT present (since hand is open)
        assertFalse("Should NOT receive Tsumo Yaku in open hand", yakuNames.any { it.contains("Tsumo") })
        
        assertEquals("Total Han should be exactly 5", 5, result.totalHan)
    }

    @Test
    fun testClosedKanHandSuuankouAndTsumoValidity() {
        // Ankan (Closed Kan) does NOT open the hand.
        // Closed Hand portion: Pin 2-2, Sou 3-3-3, Sou 5-5-5, Sou 7-7-7 (11 tiles)
        // Declared closed melds: Ankan of Pin 9-9-9-9 (4 tiles, functions as a pong of 3 tiles for hand validation)
        // Total virtual tiles = 11 + 3 = 14 tiles
        // Hand consists of 4 closed pongs: Sou 3-3-3, Sou 5-5-5, Sou 7-7-7, Pin 9-9-9-9
        // Plus 1 pair: Pin 2-2.
        // Suuankou (Four Closed Pongs) is a Yakuman.
        // Since Ankan is CLOSED:
        // - Hand remains closed.
        // - Suuankou remains valid (if Tsumo).
        // - Riichi/Tsumo remain active.
        val closedTiles = createHand(
            TileType.PIN2, TileType.PIN2,
            TileType.SOU3, TileType.SOU3, TileType.SOU3,
            TileType.SOU5, TileType.SOU5, TileType.SOU5,
            TileType.SOU7, TileType.SOU7, TileType.SOU7
        )

        val declaredMelds = listOf(
            DeclaredMeld(MeldSource.KAN_OCULTO, createHand(TileType.PIN9, TileType.PIN9, TileType.PIN9, TileType.PIN9))
        )

        val result = MahjongEngine.evaluateHand(
            closedTiles = closedTiles,
            declaredMelds = declaredMelds,
            isTsumo = true,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = true, // valid because hand is closed!
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false
        )

        assertTrue("Should win", result.isWin)
        assertTrue("Should be Suuankou Yakuman", result.isYakuman)
        val yakuNames = result.yakuList.map { it.first }
        assertTrue("Should contain Suuankou", yakuNames.any { it.contains("Suuankou") })
    }

    @Test
    fun testRinshanKaihouYakuWithKan() {
        // Rinshan Kaihou (+1 Han) on a Tsumo win following an open or closed Kan.
        // Closed Hand portion: Pin 2-3-4, Pin 5-6-7, Sou 3-4-5, Sou 8-8 (11 tiles)
        // Declared meld: Open Kan of Sou 1-1-1-1 (4 tiles)
        // Total virtual tiles = 11 + 3 = 14 tiles
        // Round wind: East, Player wind: South.
        // Win is Tsumo with Rinshan Kaihou selected.
        // Hand is open due to Daiminkan (Open Kan), so:
        // - No Pinfu or closed Tsumo yaku.
        // - However, Rinshan Kaihou Yaku (+1 Han) is awarded because we declared a Kan and won on Tsumo!
        val closedTiles = createHand(
            TileType.PIN2, TileType.PIN3, TileType.PIN4,
            TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.SOU3, TileType.SOU4, TileType.SOU5,
            TileType.SOU8, TileType.SOU8
        )

        val declaredMelds = listOf(
            DeclaredMeld(MeldSource.KAN, createHand(TileType.SOU1, TileType.SOU1, TileType.SOU1, TileType.SOU1))
        )

        val result = MahjongEngine.evaluateHand(
            closedTiles = closedTiles,
            declaredMelds = declaredMelds,
            isTsumo = true, // Tsumo is required for Rinshan Kaihou
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = true // Rinshan Kaihou active!
        )

        assertTrue("Should be winning hand", result.isWin)
        val yakuNames = result.yakuList.map { it.first }
        assertTrue("Should contain Rinshan Kaihou Yaku", yakuNames.any { it.contains("Rinshan Kaihou") })
        assertEquals("Total Han should be 1 (only Rinshan Kaihou)", 1, result.totalHan)
    }

    @Test
    fun testPinfuWaitRestrictions() {
        // Closed Hand portion: PIN 2-3-4, PIN 5-6-7, SOU 3-4-5, SOU 6-7-8, SOU 2-2
        // All four are Chows, Pair is SOU 2 (valueless).
        
        // Case 1: Winning tile is PIN 2. Completes PIN 2-3-4 from the left (wait was 2 with 3-4 shape).
        // This is a valid two-sided wait (can wait for 2 or 5). Should receive Pinfu!
        val tiles1 = createHand(
            TileType.PIN3, TileType.PIN4, TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.SOU3, TileType.SOU4, TileType.SOU5, TileType.SOU6, TileType.SOU7, TileType.SOU8,
            TileType.SOU2, TileType.SOU2,
            TileType.PIN2 // Added last as winning tile!
        )
        val result1 = MahjongEngine.evaluateHand(
            closedTiles = tiles1,
            declaredMelds = emptyList(),
            isTsumo = true,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false,
            winningTile = Tile(TileType.PIN2)
        )
        assertTrue(result1.isWin)
        assertTrue("Should contain Pinfu for valid Ryanzan wait", result1.yakuList.any { it.first.contains("Pinfu") })

        // Case 2: Hand completes with SOU 3 completing sequence 1-2-3 (Edge wait / Penchan wait).
        // PIN 5-6-7, SOU 4-5-6, SOU 7-8-9, SOU 2-2, SOU 1-2 (closed portion)
        // Winning tile is SOU 3 (wait was 3 on 1-2).
        // This is an invalid edge wait (Penchan). Should NOT receive Pinfu!
        val tiles2 = createHand(
            TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.SOU4, TileType.SOU5, TileType.SOU6,
            TileType.SOU7, TileType.SOU8, TileType.SOU9,
            TileType.SOU2, TileType.SOU2,
            TileType.SOU1, TileType.SOU2,
            TileType.SOU3 // Added last as winning tile!
        )
        val result2 = MahjongEngine.evaluateHand(
            closedTiles = tiles2,
            declaredMelds = emptyList(),
            isTsumo = true,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false,
            winningTile = Tile(TileType.SOU3)
        )
        // Hand should still be a win (since it has Tsumo Yaku), but should NOT contain Pinfu!
        assertTrue(result2.isWin)
        assertFalse("Should NOT contain Pinfu for Penchan wait", result2.yakuList.any { it.first.contains("Pinfu") })

        // Case 3: Hand completes with SOU 2 completing sequence 1-2-3 (Middle wait / Kanchan wait).
        // PIN 5-6-7, SOU 4-5-6, SOU 7-8-9, SOU 8-8 (pair), SOU 1-3 (closed portion)
        // Winning tile is SOU 2 (wait was 2 on 1-3).
        // This is an invalid middle wait (Kanchan). Should NOT receive Pinfu!
        val tiles3 = createHand(
            TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.SOU4, TileType.SOU5, TileType.SOU6,
            TileType.SOU7, TileType.SOU8, TileType.SOU9,
            TileType.SOU8, TileType.SOU8,
            TileType.SOU1, TileType.SOU3,
            TileType.SOU2 // Added last as winning tile!
        )
        val result3 = MahjongEngine.evaluateHand(
            closedTiles = tiles3,
            declaredMelds = emptyList(),
            isTsumo = true,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false,
            winningTile = Tile(TileType.SOU2)
        )
        assertTrue(result3.isWin)
        assertFalse("Should NOT contain Pinfu for Kanchan wait", result3.yakuList.any { it.first.contains("Pinfu") })

        // Case 4: Hand completes with SOU 2 completing the pair SOU 2-2 (Pair wait / Tanki wait).
        // PIN 2-3-4, PIN 5-6-7, SOU 3-4-5, SOU 6-7-8, SOU 2 (closed portion)
        // Winning tile is SOU 2 (wait was 2 on single 2).
        // This is an invalid pair wait (Tanki). Should NOT receive Pinfu!
        val tiles4 = createHand(
            TileType.PIN2, TileType.PIN3, TileType.PIN4,
            TileType.PIN5, TileType.PIN6, TileType.PIN7,
            TileType.SOU3, TileType.SOU4, TileType.SOU5,
            TileType.SOU6, TileType.SOU7, TileType.SOU8,
            TileType.SOU2,
            TileType.SOU2 // Added last as winning tile!
        )
        val result4 = MahjongEngine.evaluateHand(
            closedTiles = tiles4,
            declaredMelds = emptyList(),
            isTsumo = true,
            doraCount = 0,
            roundWind = TileType.EAST,
            playerWind = TileType.SOUTH,
            hasRiichi = false,
            hasDaburuRiichi = false,
            hasIppatsu = false,
            hasHaiteiOrHoutei = false,
            hasRinshanKaihou = false,
            winningTile = Tile(TileType.SOU2)
        )
        assertTrue(result4.isWin)
        assertFalse("Should NOT contain Pinfu for Tanki wait", result4.yakuList.any { it.first.contains("Pinfu") })
    }
}