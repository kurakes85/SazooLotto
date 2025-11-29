package com.example.sazoolotto.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object RealLottoEngine {
    data class LottoResult(val drwNo: Int, val date: String, val numbers: List<Int>, val bonus: Int)

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val actNw = cm.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    // 최신 회차 번호 계산 (숫자만 반환)
    fun calculateLatestRound(): Int {
        val startDate = LocalDate.of(2002, 12, 7)
        val now = LocalDateTime.now()
        val diffDays = ChronoUnit.DAYS.between(startDate, now.toLocalDate())
        var round = (diffDays / 7).toInt() + 1
        if (now.dayOfWeek == DayOfWeek.SATURDAY && now.hour < 21) round -= 1
        return round
    }

    // 특정 회차 정보 가져오기 (기본값은 최신)
    suspend fun fetchLotto(round: Int): LottoResult? = withContext(Dispatchers.IO) {
        try {
            getLottoFromApi(round)
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun getLottoFromApi(round: Int): LottoResult? {
        return try {
            val url = URL("https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=$round")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                if (json.getString("returnValue") == "success") {
                    val nums = listOf(json.getInt("drwtNo1"), json.getInt("drwtNo2"), json.getInt("drwtNo3"), json.getInt("drwtNo4"), json.getInt("drwtNo5"), json.getInt("drwtNo6"))
                    LottoResult(round, json.getString("drwNoDate"), nums, json.getInt("bnusNo"))
                } else null
            } else null
        } catch (e: Exception) { null }
    }
}