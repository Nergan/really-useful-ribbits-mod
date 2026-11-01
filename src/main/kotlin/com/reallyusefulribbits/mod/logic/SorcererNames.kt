package com.reallyusefulribbits.mod.logic

/** Смешные и проклятые ники для заклинания смены имени. */
object SorcererNames {
    val NAMES: List<String> = listOf(
        "Steve's Evil Twin", "xX_Frog_Lord_Xx", "Mr. Ribbit", "Notch's Intern", "Grass Block Enjoyer",
        "404 Name Not Found", "Totally Not Herobrine", "Toad Mode", "Slimeball Supreme", "Creeper Hugger",
        "Lord of Dirt", "Bucket of Frogs", "Fall Damage Pro", "Wet Cardboard", "Lava Swimmer",
        "Moist Bread", "N00B_SL4YER_3000", "Cursed Potato", "The Lag Itself", "AFK Since 2012",
        "Chicken Jockey", "Sir Croaks-a-Lot", "Bedrock Eater", "Gravel Gourmet", "Glow Squid Truther",
        "Pickaxe Licker", "Uncle Enderman", "Soggy Waffle", "Captain Obvious", "Definitely A Villager",
        "Жабий Король", "Квакен", "Вася Пупкин", "Земляной Червяк", "Пельмень",
        "Огурчик", "Батон", "Котлета с пюрешкой", "Слизень Геннадий", "Алмазный Нищеброд",
        "Король Гравия", "Тапок", "Бабушкин Внук", "Ква-ква", "Проклятая Картошка",
        "Сосиска в Тесте", "Лягушонок Пепе", "Шашлычок", "Тот Самый Стив", "Потерянный Носок",
        "Нубик_228", "Дед Инсайд", "Кабачок", "Шлёпа", "Пирожок с Ничем",
        "S̷t̴e̵v̶e̷", "Ẍ̴̤́ẍ̵̣́_̷̤̄Ḧ̸́ë̷́r̵̤̈", "К̷в̸а̵к̶", "ERROR_NAME_NULL", "[object Object]",
    )

    /** Цвета ника — коды ChatFormatting 0–15. -1 оставляет обычный цвет. */
    fun color(roll: Double, colorRoll: Int): Int = if (roll < 0.5) -1 else Math.floorMod(colorRoll, 16)

    fun pick(roll: Int, current: String): String {
        val choices = NAMES.filter { it != current }
        return choices[Math.floorMod(roll, choices.size)]
    }
}
