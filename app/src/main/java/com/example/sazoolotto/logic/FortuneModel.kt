package com.example.sazoolotto.logic

import java.time.LocalDate
import java.time.LocalTime

enum class Gender(val displayName: String) {
    MALE("남성"),
    FEMALE("여성")
}

enum class BirthTimeSection(val displayName: String, val approxTime: LocalTime) {
    UNKNOWN("시간 모름", LocalTime.of(12, 0)),
    DAWN("새벽 (00:00~06:00)", LocalTime.of(3, 0)),
    MORNING("오전 (06:00~12:00)", LocalTime.of(9, 0)),
    AFTERNOON("오후 (12:00~18:00)", LocalTime.of(15, 0)),
    NIGHT("밤 (18:00~24:00)", LocalTime.of(21, 0))
}

enum class FiveElement(
    val kName: String,
    val colorHex: Long,
    val desc: String
) {
    WOOD("목(나무)", 0xFF4CAF50, "성장과 의욕이 넘치는 기운"),
    FIRE("화(불)", 0xFFF44336, "열정과 확산의 강한 에너지"),
    EARTH("토(흙)", 0xFFFFC107, "안정과 포용의 단단한 기운"),
    METAL("금(쇠)", 0xFF9E9E9E, "결단력과 냉철한 이성"),
    WATER("수(물)", 0xFF2196F3, "유연함과 지혜의 흐름")
}

data class SajuInfo(
    val birthDate: LocalDate,
    val birthTimeSection: BirthTimeSection,
    val gender: Gender,
    val name: String = ""
)

data class LottoResult(
    val step: Int = 1,
    val recommendNumbers: List<Int> = emptyList(),
    val excludedNumbers: List<Int> = emptyList(),
    val mainFortune: String = "",
    val subAdvice: String = "",
    val premiumFortune: String = "",
    val element: FiveElement = FiveElement.WOOD
)
