package com.example.sazoolotto.logic

import androidx.compose.ui.graphics.Color
import com.example.sazoolotto.ui.theme.FireAccent
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

enum class FiveElement(val koreanName: String, val emoji: String, val color: Color, val luckyNumbers: List<Int>) {
    WOOD("목(나무)", "🌿", Color(0xFF66A86E), listOf(3, 8)),
    FIRE("화(불)", "🔥", FireAccent, listOf(2, 7)),
    EARTH("토(흙)", "⛰️", Color(0xFFB59473), listOf(5, 0)),
    METAL("금(쇠)", "💎", Color(0xFF90A4AE), listOf(4, 9)),
    WATER("수(물)", "🌊", Color(0xFF4FC3F7), listOf(1, 6))
}

data class SajuInfo(val yearGanji: String, val zodiac: String, val dayGanji: String, val dayElement: FiveElement)

object SazooEngine {
    // ... (기존 상수 및 계산 함수 calculateSaju 등은 그대로 유지. 분량상 생략하나 꼭 포함하세요!) ...
    private val CHEONGAN = listOf("갑", "을", "병", "정", "무", "기", "경", "신", "임", "계")
    private val JIJI = listOf("자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해")
    private val ZODIAC_ANIMALS = listOf("쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양", "원숭이", "닭", "개", "돼지")
    private fun getElementFromCheongan(idx: Int): FiveElement { return when (idx) { 0,1->FiveElement.WOOD; 2,3->FiveElement.FIRE; 4,5->FiveElement.EARTH; 6,7->FiveElement.METAL; else->FiveElement.WATER } }

    fun calculateSaju(date: LocalDate, time: LocalTime): SajuInfo {
        val y = date.year
        val yearStem = (y - 4 + 10) % 10
        val yearBranch = (y - 4 + 12) % 12
        val yearGanji = "${CHEONGAN[yearStem]}${JIJI[yearBranch]}년"
        val zodiac = "${ZODIAC_ANIMALS[yearBranch]}띠"
        val refDate = LocalDate.of(2000, 1, 1)
        val days = ChronoUnit.DAYS.between(refDate, date)
        var dayIdx = (54L + days) % 60L
        if (dayIdx < 0) dayIdx += 60
        val dStem = (dayIdx % 10).toInt()
        val element = getElementFromCheongan(dStem)
        val dBranch = (dayIdx % 12).toInt()
        val dayGanji = "${CHEONGAN[dStem]}${JIJI[dBranch]}일"
        return SajuInfo(yearGanji, zodiac, dayGanji, element)
    }

    fun getBadLuckNumber(date: LocalDate?, excludeList: List<Int>): Int {
        val today = LocalDate.now().toString()
        // 단계별로 다른 시드 사용 (excludeList 사이즈로 구분)
        val seed = "$date|$today|BAD|${excludeList.size}".hashCode()
        val random = Random(seed)
        var badNum: Int
        var safety = 0
        do {
            badNum = random.nextInt(45) + 1
            safety++
        } while (excludeList.contains(badNum) && safety < 100)
        return badNum
    }

    // 🔥 [신규] 조심해야 할 것 (Caution) 목록 - 100개 DB
    private val CAUTION_DB = listOf(
        "과속 운전", "빙판길", "뜨거운 물", "날카로운 칼", "보증 서기", "충동 구매", "밤길 걷기", "지나친 음주", "말실수", "비밀 누설",
        "찬 음식", "높은 곳", "무거운 물건", "오래된 음식", "낯선 사람", "서명/계약", "빌려준 돈", "늦잠", "약속 지각", "스마트폰 분실",
        "계단 조심", "문턱", "전기 콘센트", "가스 밸브", "지갑 분실", "비밀번호", "이메일 해킹", "보이스피싱", "주식 단타", "무리한 운동",
        "감기 기운", "소화 불량", "두통", "불면증", "근육통", "눈 피로", "허리 조심", "무릎 관절", "목 디스크", "손목 터널",
        "친구와 다툼", "연인과 오해", "부모님 잔소리", "직장 상사", "동료의 뒷담화", "이웃간 소음", "반려동물 가출", "화초 죽이기", "물건 파손", "옷에 얼룩",
        "우산 분실", "차 키 분실", "지갑 두고 나옴", "버스 놓침", "지하철 반대 방향", "택시 요금 폭탄", "길 잃음", "예약 실수", "주문 실수", "계산 착오",
        "배탈", "알레르기", "벌레 물림", "햇볕 화상", "미세먼지", "빗길 운전", "눈길 운전", "안개", "강풍", "천둥 번개",
        "공사장 근처", "맨홀 뚜껑", "유리 조각", "미끄러운 바닥", "뜨거운 냄비", "가위", "바늘", "압정", "스테이플러", "종이에 베임",
        "거짓말", "변명", "미루는 습관", "게으름", "욕심", "질투", "오만", "편견", "고집", "무시",
        "나태함", "부정적 생각", "남 탓하기", "책임 회피", "과식", "폭식", "야식", "단 음식", "짠 음식", "매운 음식"
    )

    // 🔥 [신규] 오늘의 조심해야 할 것 가져오기 (단계별로 다른 것 리턴)
    fun getCautionMessage(date: LocalDate?, count: Int): String {
        if (date == null) return "안전 제일"
        val today = LocalDate.now().toString()
        // 날짜 + 단계(count)를 조합해 고정된 랜덤 값 추출
        val seed = "$date|$today|CAUTION|$count".hashCode()
        val random = Random(seed)
        return CAUTION_DB[random.nextInt(CAUTION_DB.size)]
    }

    // ... (나머지 운세/로또 함수들은 기존과 동일하게 유지) ...
    // (generateFortune, getSpecialFortune, generateLottoNumbers 등)
    // (직전 답변의 대용량 DB 내용 포함)

    // (에러 방지용 임시 코드 - 실제로는 이전의 풍부한 내용 사용)
    fun getSpecialFortune(element: FiveElement, birthDate: LocalDate): Pair<String, String> = "프리미엄" to "대박나세요"
    fun generateFortune(date: LocalDate?, time: LocalTime?, gender: String?, count: Int): Pair<String, String> = "기본 운세" to "좋은 날입니다."
    fun generateLottoNumbers(date: LocalDate?, gender: String?, count: Int, excludeList: List<Int>): List<Int> {
        val all = (1..45).filter { !excludeList.contains(it) }.toMutableList()
        all.shuffle(Random(System.nanoTime()))
        return all.take(6).sorted()
    }
}