package com.example.sazoolotto.logic

import kotlin.math.abs

class SazooEngine {

    fun calculateFiveElement(sajuInfo: SajuInfo): FiveElement {
        val dateHash = sajuInfo.birthDate.hashCode()
        val timeHash = sajuInfo.birthTimeSection.approxTime.hashCode()
        val genderFactor = if (sajuInfo.gender == Gender.MALE) 7 else 3

        val totalHash = dateHash + (timeHash * 13) + (genderFactor * 17)
        val values = FiveElement.values()
        return values[abs(totalHash) % values.size]
    }

    fun generateInitialResult(sajuInfo: SajuInfo): LottoResult {
        val element = calculateFiveElement(sajuInfo)
        val numbers = generateNumbers(count = 6, exclude = emptySet())

        val advice = when (element) {
            FiveElement.WOOD -> "오늘은 새로운 일을 시작하기에 좋습니다."
            FiveElement.FIRE -> "성급함보다는 차분함이 필요한 하루입니다."
            FiveElement.EARTH -> "주변 사람들과의 신뢰가 행운을 부릅니다."
            FiveElement.METAL -> "중요한 결정은 오전보다 오후가 좋습니다."
            FiveElement.WATER -> "물 흐르듯 유연하게 대처하면 이득이 있습니다."
        }

        return LottoResult(
            step = 1,
            recommendNumbers = numbers.sorted(),
            element = element,
            mainFortune = "${element.kName}의 기운이 강한 날입니다.",
            subAdvice = advice
        )
    }

    fun applyStep(current: LottoResult, nextStep: Int): LottoResult {
        return when (nextStep) {
            2 -> {
                val badNumbers = pickBadNumbers(from = current.recommendNumbers, count = 3)
                val allExcluded = current.excludedNumbers.toSet() + badNumbers.toSet()
                val newNumbers = generateNumbers(count = 6, exclude = allExcluded)

                current.copy(
                    step = 2,
                    excludedNumbers = allExcluded.toList().sorted(),
                    recommendNumbers = newNumbers.sorted(),
                    subAdvice = "액운 숫자 ${badNumbers.joinToString(", ")}을(를) 제외하고 정제했습니다."
                )
            }

            3 -> {
                current.copy(
                    step = 3,
                    premiumFortune = "동쪽에서 귀인을 만날 수 있습니다. 40대 이상에게 길한 숫자 조합이 완성되었습니다."
                )
            }

            else -> current
        }
    }

    private fun generateNumbers(count: Int, exclude: Set<Int>): List<Int> {
        val pool = (1..45).filter { it !in exclude }
        return pool.shuffled().take(count)
    }

    private fun pickBadNumbers(from: List<Int>, count: Int): List<Int> =
        from.shuffled().take(count)
}
