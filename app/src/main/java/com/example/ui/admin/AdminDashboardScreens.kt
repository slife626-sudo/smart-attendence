package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AttendanceCalculator
import com.example.domain.DashboardReport
import com.example.domain.StaffSummaryEntry
import com.example.domain.YearlyStaffRow
import com.example.ui.components.DailyAttendanceBarChart
import com.example.ui.components.DatePickerField
import com.example.ui.components.DonutSlice
import com.example.ui.components.ExpandableHintBanner
import com.example.ui.components.KpiCard
import com.example.ui.components.OrgBannerHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusDonutChart
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchGreen
import com.example.ui.theme.PunchRed
import com.example.ui.theme.PurpleOvertime
import com.example.ui.theme.TealInfo
import com.example.ui.theme.TextMuted

@Composable
fun AdminDashboardSection(
    report: DashboardReport,
    selectedMonth: String,
    selectedBranch: String,
    onOpenToday: () -> Unit,
    onOpenRep: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OrgBannerHeader(
            orgName = report.org,
            address = report.addr,
            subtitle = "${AttendanceCalculator.monthLabelBn(selectedMonth)}${if (selectedBranch.isNotBlank()) " · শাখা: $selectedBranch" else " · সব শাখা"}"
        )

        // 4 Top KPIs in 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                label = "আজ উপস্থিত (${AttendanceCalculator.formatDateBn(report.selectedDate)})",
                value = AttendanceCalculator.toBanglaDigits(report.todayPresent),
                subtext = "দেরি: ${AttendanceCalculator.toBanglaDigits(report.todayLate)} · ছুটি: ${AttendanceCalculator.toBanglaDigits(report.todayLeave)}",
                accentColor = PunchGreen,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "আজ অনুপস্থিত",
                value = AttendanceCalculator.toBanglaDigits(report.todayAbsent),
                subtext = if (report.todayOff > 0) "ছুটির দিন: ${AttendanceCalculator.toBanglaDigits(report.todayOff)}" else "মোট কর্মী ${AttendanceCalculator.toBanglaDigits(report.staffCount)}",
                accentColor = PunchRed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                label = "মাসিক উপস্থিতির হার",
                value = if (report.totalWork > 0) "${AttendanceCalculator.toBanglaDigits((report.totalPresent * 100) / report.totalWork)}%" else "-",
                subtext = "${AttendanceCalculator.toBanglaDigits(report.totalPresent)} / ${AttendanceCalculator.toBanglaDigits(report.totalWork)} কর্মদিবস",
                accentColor = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "মোট ওভারটাইম",
                value = AttendanceCalculator.formatHmShort(report.totalOtMin),
                subtext = "দেরিতে আগমন: ${AttendanceCalculator.toBanglaDigits(report.totalLate)} দিন",
                accentColor = PurpleOvertime,
                modifier = Modifier.weight(1f)
            )
        }

        // Daily Bar Chart Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "দৈনিক উপস্থিতি",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                DailyAttendanceBarChart(points = report.dailySeries)
            }
        }

        // Status Donut Chart Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "মাসিক অবস্থা বিভাজন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatusDonutChart(
                    slices = listOf(
                        DonutSlice("সময়মতো", (report.totalPresent - report.totalLate).coerceAtLeast(0), PunchGreen),
                        DonutSlice("দেরিতে", report.totalLate, AmberAccent),
                        DonutSlice("অনুপস্থিত", report.totalAbsent, PunchRed),
                        DonutSlice("ছুটি", report.totalLeave, TealInfo),
                        DonutSlice("সরকারি/সাপ্তাহিক", report.totalOff, BlueAccent)
                    )
                )
            }
        }

        // Branch breakdown
        if (report.branchStats.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "শাখা অনুযায়ী উপস্থিতি",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    report.branchStats.forEach { bs ->
                        val ratio = if (bs.work > 0) bs.present.toFloat() / bs.work.toFloat() else 0f
                        val pct = (ratio * 100).toInt()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${bs.branch} (${AttendanceCalculator.toBanglaDigits(bs.staff)} জন)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${AttendanceCalculator.toBanglaDigits(pct)}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // Top 5 Late & Absent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RankedCard(
                title = "শীর্ষ দেরি",
                emptyMsg = "কেউ দেরি করেনি",
                items = report.topLate.map { it.name to "${AttendanceCalculator.toBanglaDigits(it.count)} দিন" },
                badgeColor = AmberAccent,
                modifier = Modifier.weight(1f)
            )
            RankedCard(
                title = "শীর্ষ অনুপস্থিত",
                emptyMsg = "কেউ অনুপস্থিত নেই",
                items = report.topAbsent.map { it.name to "${AttendanceCalculator.toBanglaDigits(it.count)} দিন" },
                badgeColor = PunchRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick preview of Today's Staff
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "আজকের সংক্ষিপ্ত তালিকা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    OutlinedButton(onClick = onOpenToday) {
                        Text("সব দেখুন")
                    }
                }
                report.todayRows.take(6).forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${r.branch} · IN ${AttendanceCalculator.formatTime12h(r.inTime)} · OUT ${AttendanceCalculator.formatTime12h(r.outTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        StatusBadge(text = r.status, cls = r.cls)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (report.todayRows.isEmpty()) {
                    Text("আজকের কোনো রেকর্ড নেই", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RankedCard(
    title: String,
    emptyMsg: String,
    items: List<Pair<String, String>>,
    badgeColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = badgeColor)
            if (items.isEmpty()) {
                Text(emptyMsg, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            } else {
                items.forEach { (n, c) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = n,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(c, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = badgeColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTodaySection(
    report: DashboardReport,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenStaffReport: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = onPrevDay, modifier = Modifier.testTag("today_prev_day")) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "আগের দিন")
                    }
                    DatePickerField(
                        label = "তারিখ",
                        value = selectedDate,
                        onValueSelected = onDateChange,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNextDay, modifier = Modifier.testTag("today_next_day")) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "পরের দিন")
                    }
                    OutlinedButton(onClick = onTodayClick, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("আজ")
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("উপস্থিত", AttendanceCalculator.toBanglaDigits(report.todayPresent), "", PunchGreen, Modifier.weight(1f))
            KpiCard("দেরি", AttendanceCalculator.toBanglaDigits(report.todayLate), "", AmberAccent, Modifier.weight(1f))
            KpiCard("অনুপস্থিত", AttendanceCalculator.toBanglaDigits(report.todayAbsent), "", PunchRed, Modifier.weight(1f))
            KpiCard("ছুটি", AttendanceCalculator.toBanglaDigits(report.todayLeave), "", TealInfo, Modifier.weight(1f))
        }

        report.todayRows.forEach { row ->
            Card(
                onClick = { onOpenStaffReport(row.code) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${row.code} · ${row.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${row.branch} · IN: ${AttendanceCalculator.formatTime12h(row.inTime)} · OUT: ${AttendanceCalculator.formatTime12h(row.outTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        val brk = AttendanceCalculator.breakTextPlain(row.breakSummary)
                        if (brk != "-") {
                            Text(
                                text = "ব্রেক: $brk",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberAccent
                            )
                        }
                    }
                    StatusBadge(text = row.status, cls = row.cls)
                }
            }
        }
        if (report.todayRows.isEmpty()) {
            Text("নির্বাচিত তারিখে কোনো কর্মীর ডেটা নেই", color = TextMuted)
        }
    }
}

@Composable
fun AdminMonthlySummarySection(
    entries: List<StaffSummaryEntry>,
    selectedMonth: String,
    selectedBranch: String,
    onSelectStaffReport: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExpandableHintBanner("যেকোনো কর্মীর কার্ডে ট্যাপ করলে ওই কর্মীর বিস্তারিত মাসিক রিপোর্ট দেখা যাবে।")
        Text(
            text = "${AttendanceCalculator.monthLabelBn(selectedMonth)} — মোট ${AttendanceCalculator.toBanglaDigits(entries.size)} জন",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        entries.forEach { e ->
            Card(
                onClick = { onSelectStaffReport(e.code) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${e.code} · ${e.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("${e.designation} · ${e.branch}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        val pct = if (e.sum.work > 0) (e.sum.present * 100) / e.sum.work else 0
                        Surface(shape = RoundedCornerShape(999.dp), color = EmeraldPrimary.copy(alpha = 0.1f)) {
                            Text(
                                text = "${AttendanceCalculator.toBanglaDigits(pct)}%",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("কর্ম: ${AttendanceCalculator.toBanglaDigits(e.sum.work)}", style = MaterialTheme.typography.labelSmall)
                        Text("উপ: ${AttendanceCalculator.toBanglaDigits(e.sum.present)}", style = MaterialTheme.typography.labelSmall, color = PunchGreen)
                        Text("দেরি: ${AttendanceCalculator.toBanglaDigits(e.sum.late)}", style = MaterialTheme.typography.labelSmall, color = AmberAccent)
                        Text("অনু: ${AttendanceCalculator.toBanglaDigits(e.sum.absent)}", style = MaterialTheme.typography.labelSmall, color = PunchRed)
                        Text("ছুটি: ${AttendanceCalculator.toBanglaDigits(e.sum.leave)}", style = MaterialTheme.typography.labelSmall, color = TealInfo)
                        Text("OT: ${AttendanceCalculator.formatTimeShortColon(e.sum.otMin)}", style = MaterialTheme.typography.labelSmall, color = PurpleOvertime)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminYearlySummarySection(
    rows: List<YearlyStaffRow>,
    selectedMonth: String
) {
    val yearStr = selectedMonth.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "${AttendanceCalculator.toBanglaDigits(yearStr)} — বাৎসরিক উপস্থিতি সারসংক্ষেপ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        rows.forEach { row ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${row.code} · ${row.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(row.branchSpan, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text(
                            text = "মোট উপ: ${AttendanceCalculator.toBanglaDigits(row.totals.present)}/${AttendanceCalculator.toBanglaDigits(row.totals.work)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.monthlyPresent.forEachIndexed { idx, p ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = AttendanceCalculator.MONTH_SHORT_BN[idx],
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = p?.let { AttendanceCalculator.toBanglaDigits(it) } ?: "—",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("দেরি: ${AttendanceCalculator.toBanglaDigits(row.totals.late)}", style = MaterialTheme.typography.labelSmall, color = AmberAccent)
                        Text("অনু: ${AttendanceCalculator.toBanglaDigits(row.totals.absent)}", style = MaterialTheme.typography.labelSmall, color = PunchRed)
                        Text("ছুটি: ${AttendanceCalculator.toBanglaDigits(row.totals.leave)}", style = MaterialTheme.typography.labelSmall, color = TealInfo)
                        Text("OT: ${AttendanceCalculator.formatHmShort(row.totals.otMin)}", style = MaterialTheme.typography.labelSmall, color = PurpleOvertime)
                    }
                }
            }
        }
    }
}
