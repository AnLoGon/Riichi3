package com.example.riichi3

enum class TileCategory {
    MAN,     // Carácter
    PIN,     // Círculo
    SOU,     // Bambú
    WIND,    // Vientos
    DRAGON   // Dragones
}

enum class TileType(
    val category: TileCategory,
    val value: Int,
    val drawableName: String,
    val displayName: String
) {
    MAN1(TileCategory.MAN, 1, "man1", "1 Carácter"),
    MAN9(TileCategory.MAN, 9, "man9", "9 Carácter"),

    PIN1(TileCategory.PIN, 1, "pin1", "1 Círculo"),
    PIN2(TileCategory.PIN, 2, "pin2", "2 Círculo"),
    PIN3(TileCategory.PIN, 3, "pin3", "3 Círculo"),
    PIN4(TileCategory.PIN, 4, "pin4", "4 Círculo"),
    PIN5(TileCategory.PIN, 5, "pin5", "5 Círculo"),
    PIN6(TileCategory.PIN, 6, "pin6", "6 Círculo"),
    PIN7(TileCategory.PIN, 7, "pin7", "7 Círculo"),
    PIN8(TileCategory.PIN, 8, "pin8", "8 Círculo"),
    PIN9(TileCategory.PIN, 9, "pin9", "9 Círculo"),

    SOU1(TileCategory.SOU, 1, "sou1", "1 Bambú"),
    SOU2(TileCategory.SOU, 2, "sou2", "2 Bambú"),
    SOU3(TileCategory.SOU, 3, "sou3", "3 Bambú"),
    SOU4(TileCategory.SOU, 4, "sou4", "4 Bambú"),
    SOU5(TileCategory.SOU, 5, "sou5", "5 Bambú"),
    SOU6(TileCategory.SOU, 6, "sou6", "6 Bambú"),
    SOU7(TileCategory.SOU, 7, "sou7", "7 Bambú"),
    SOU8(TileCategory.SOU, 8, "sou8", "8 Bambú"),
    SOU9(TileCategory.SOU, 9, "sou9", "9 Bambú"),

    EAST(TileCategory.WIND, 1, "east", "Este"),
    SOUTH(TileCategory.WIND, 2, "south", "Sur"),
    WEST(TileCategory.WIND, 3, "west", "Oeste"),
    NORTH(TileCategory.WIND, 4, "north", "Norte"),

    WHITE(TileCategory.DRAGON, 1, "white_dragon", "Blanco"),
    GREEN(TileCategory.DRAGON, 2, "green_dragon", "Verde"),
    RED(TileCategory.DRAGON, 3, "red_dragon", "Rojo");

    companion object {
        fun fromDrawableName(name: String): TileType? {
            return values().find { it.drawableName == name }
        }
    }
}

data class Tile(val type: TileType) : Comparable<Tile> {
    override fun compareTo(other: Tile): Int {
        if (this.type.category != other.type.category) {
            return this.type.category.ordinal.compareTo(other.type.category.ordinal)
        }
        return this.type.value.compareTo(other.type.value)
    }
}

enum class MeldSource {
    CHI,
    PON,
    KAN,
    KAN_OCULTO
}

data class DeclaredMeld(
    val source: MeldSource,
    val tiles: List<Tile>
)
