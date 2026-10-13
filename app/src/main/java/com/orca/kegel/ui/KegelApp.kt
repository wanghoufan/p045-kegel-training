package com.orca.kegel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orca.kegel.R
import com.orca.kegel.audio.TrainingState
import com.orca.kegel.data.AppState
import com.orca.kegel.data.LangMode
import com.orca.kegel.data.ThemeMode
import com.orca.kegel.domain.Level
import com.orca.kegel.domain.LevelRules
import com.orca.kegel.domain.HistoryInsights
import com.orca.kegel.domain.PlanResult
import com.orca.kegel.domain.Recommender
import com.orca.kegel.domain.TrainingStage
import java.time.LocalDate

private enum class Tab(val titleRes: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, Icons.Filled.Home),
    ABILITY(R.string.nav_ability, Icons.Filled.Star),
    HISTORY(R.string.nav_history, Icons.Filled.DateRange),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings),
}

private enum class Sub { NONE, ASSESS, STANDARDS, EHS }

@Composable
fun KegelApp(vm: AppViewModel) {
    val stage by TrainingState.stage.collectAsState()
    when (stage) {
        TrainingStage.RUNNING, TrainingStage.PAUSED -> TrainingScreen(vm)
        TrainingStage.FINISHED, TrainingStage.SAFETY_STOPPED -> FinishScreen(vm)
        else -> MainShell(vm)
    }
}

@Composable
private fun MainShell(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var sub by rememberSaveable { mutableStateOf(Sub.NONE) }
    val state by vm.state.collectAsState()

    when (sub) {
        Sub.ASSESS -> AssessmentScreen(vm, onClose = { sub = Sub.NONE })
        Sub.STANDARDS -> StandardsScreen(onBack = { sub = Sub.NONE })
        Sub.EHS -> EhsScreen(vm, onBack = { sub = Sub.NONE })
        Sub.NONE -> MainScaffold(vm, state, tab, onTab = { tab = it }, onSub = { sub = it })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    vm: AppViewModel,
    state: AppState,
    tab: Tab,
    onTab: (Tab) -> Unit,
    onSub: (Sub) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(tab.titleRes)) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { onTab(t) },
                        icon = { Icon(t.icon, contentDescription = stringResource(t.titleRes)) },
                        label = { Text(stringResource(t.titleRes)) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.TODAY -> HomeScreen(vm, state, padding, onOpenStandards = { onSub(Sub.STANDARDS) }, onOpenAssess = { onSub(Sub.ASSESS) })
            Tab.ABILITY -> AbilityScreen(vm, state, padding, onOpenStandards = { onSub(Sub.STANDARDS) }, onOpenAssess = { onSub(Sub.ASSESS) })
            Tab.HISTORY -> HistoryScreen(state, padding)
            Tab.SETTINGS -> SettingsScreen(vm, state, padding, onOpenEhs = { onSub(Sub.EHS) })
        }
    }
}

@Composable
private fun ScreenColumn(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
internal fun DurationText(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) stringResource(R.string.dur_min_sec, m, s) else stringResource(R.string.dur_sec, s)
}

@Composable
private fun LevelLabel(level: Level): String = stringResource(R.string.common_level, level.num)

// ---------------- 首页 ----------------

@Composable
private fun HomeScreen(
    vm: AppViewModel,
    state: AppState,
    padding: PaddingValues,
    onOpenStandards: () -> Unit,
    onOpenAssess: () -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val plan = vm.todayPlan(state)
    val today = LocalDate.now().toEpochDay()
    val halfOn = state.halfDoseOn(today)

    ScreenColumn(padding) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_last_assessment), style = MaterialTheme.typography.bodySmall)
                Text(
                    state.lastAssessment?.let { r ->
                        Recommender.recommend(r)?.let { LevelLabel(it) } ?: "—"
                    } ?: stringResource(R.string.home_level_unknown),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_current_level), style = MaterialTheme.typography.bodySmall)
                Text(
                    LevelLabel(state.currentLevel),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (!state.levelChosen) {
            HintCard(stringResource(R.string.home_skip_hint), actionText = stringResource(R.string.nav_assess), onAction = onOpenAssess)
        }

        HorizontalDivider()

        when (plan) {
            is PlanResult.Ok -> {
                val p = plan.plan
                Text(
                    stringResource(R.string.home_plan_exec, p.enduranceReps, p.fastReps, p.totalContractions),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.home_plan_groups, p.totalGroups, p.enduranceGroups, p.fastGroups),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.home_plan_duration, DurationText(p.estimatedSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.home_plan_session, p.sessionIndex, p.sessionCount),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (p.reducedByRemaining) {
                    Text(stringResource(R.string.home_reduced_note), style = MaterialTheme.typography.bodySmall)
                }
                if (p.halfDose) {
                    Text(stringResource(R.string.home_half_dose_on), style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { vm.startToday() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.home_start), style = MaterialTheme.typography.titleMedium) }
            }
            is PlanResult.Blocked -> {
                Text(
                    stringResource(R.string.home_quota_blocked),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (state.shouldPromptRestDay()) {
            HintCard(stringResource(R.string.home_rest_day_hint, maxOf(state.consecutiveCompletedDays(), state.consecutiveHighDays())))
        }

        if (state.shouldPromptRetest()) {
            HintCard(
                stringResource(R.string.home_retest_hint, state.completedDaysSinceAssessment()),
                actionText = stringResource(R.string.home_retest_action),
                onAction = onOpenAssess,
                dismissText = stringResource(R.string.dismiss),
                onDismiss = { vm.dismissRetestPrompt() },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_change_level))
            }
            OutlinedButton(onClick = onOpenStandards, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_standard))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = halfOn, onCheckedChange = { vm.setHalfDose(it) })
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_half_dose), style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (pickerOpen) {
        LevelPickerDialog(
            current = state.currentLevel,
            onPick = { vm.selectLevel(it, "manual"); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun HintCard(
    text: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (actionText != null || dismissText != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (actionText != null && onAction != null) {
                        TextButton(onClick = onAction) { Text(actionText) }
                    }
                    if (dismissText != null && onDismiss != null) {
                        TextButton(onClick = onDismiss) { Text(dismissText) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelPickerDialog(current: Level, onPick: (Level) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_change_level)) },
        text = {
            Column {
                Text(stringResource(R.string.standards_note), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Level.entries.forEach { lv ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(lv == current, onClick = { onPick(lv) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = lv == current, onClick = { onPick(lv) })
                        Spacer(Modifier.width(4.dp))
                        val s = LevelRules.spec(lv)
                        Text(
                            stringResource(
                                R.string.std_level_summary,
                                LevelLabel(lv), s.holdSec,
                                s.endurancePerSession, s.fastPerSession, s.fullSessions,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

// ---------------- 各级标准 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardsScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.standards_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
        )
    }) { padding ->
        ScreenColumn(padding) {
            Text(stringResource(R.string.standards_note), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            StandardsTable(
                title = stringResource(R.string.std_endurance_title),
                headers = listOf(
                    stringResource(R.string.std_col_level),
                    stringResource(R.string.std_col_day_sessions),
                    stringResource(R.string.std_col_per_session),
                    stringResource(R.string.std_col_day_total),
                ),
                rows = Level.entries.map { lv ->
                    val s = LevelRules.spec(lv)
                    listOf("L${lv.num}", "${s.fullSessions}", "${s.endurancePerSession}", "${s.dailyEndurance}")
                },
            )

            StandardsTable(
                title = stringResource(R.string.std_fast_title),
                headers = listOf(
                    stringResource(R.string.std_col_level),
                    stringResource(R.string.std_col_day_rounds),
                    stringResource(R.string.std_col_per_round),
                    stringResource(R.string.std_col_day_total),
                ),
                rows = Level.entries.map { lv ->
                    val s = LevelRules.spec(lv)
                    listOf("L${lv.num}", "${s.fullSessions}", "${s.fastPerSession}", "${s.dailyFast}")
                },
            )

            StandardsTable(
                title = stringResource(R.string.std_total_title),
                headers = listOf(
                    stringResource(R.string.std_col_level),
                    stringResource(R.string.std_col_e_total),
                    stringResource(R.string.std_col_f_total),
                    stringResource(R.string.std_col_day_sum),
                ),
                rows = Level.entries.map { lv ->
                    val s = LevelRules.spec(lv)
                    listOf("L${lv.num}", "${s.dailyEndurance}", "${s.dailyFast}", "${s.dailyTotal}")
                },
            )

            HorizontalDivider()
            Level.entries.forEach { lv ->
                Text(
                    stringResource(
                        R.string.standards_estimate,
                        LevelLabel(lv),
                        DurationText(LevelRules.standardSessionSeconds(lv)),
                        DurationText(LevelRules.standardDailySeconds(lv)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(stringResource(R.string.standards_footer), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 三张表统一渲染：首列左对齐、其余右对齐，等宽字体便于对齐。 */
@Composable
private fun StandardsTable(title: String, headers: List<String>, rows: List<List<String>>) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    StandardTableRow(headers, header = true)
    HorizontalDivider()
    rows.forEach { StandardTableRow(it, header = false) }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun StandardTableRow(cells: List<String>, header: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        cells.forEachIndexed { i, cell ->
            Text(
                text = cell,
                modifier = Modifier.weight(if (i == 0) 0.7f else 1f),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ---------------- 历史与统计 ----------------

private const val CHART_DAYS = 7
private const val TABLE_DAYS = 14

@Composable
private fun HistoryScreen(state: AppState, padding: PaddingValues) {
    val today = LocalDate.now().toEpochDay()
    val summary = HistoryInsights.summary(state.history)
    val chartDays = HistoryInsights.daily(state.history, CHART_DAYS, today)
    val tableDays = HistoryInsights.daily(state.history, TABLE_DAYS, today)

    ScreenColumn(padding) {
        if (state.history.isEmpty()) {
            Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
            return@ScreenColumn
        }

        Text(stringResource(R.string.history_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryStatCard(stringResource(R.string.history_total_sessions), summary.totalSessions.toString(), Modifier.weight(1f))
            HistoryStatCard(stringResource(R.string.history_total_days), summary.totalDays.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryStatCard(stringResource(R.string.history_total_duration), DurationText(summary.totalDurationSeconds), Modifier.weight(1f))
            HistoryStatCard(stringResource(R.string.history_total_reps), summary.totalReps.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryStatCard(stringResource(R.string.history_total_endurance), summary.totalEnduranceReps.toString(), Modifier.weight(1f))
            HistoryStatCard(stringResource(R.string.history_total_fast), summary.totalFastReps.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryStatCard(stringResource(R.string.history_total_hold), DurationText(summary.totalHoldSeconds), Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }

        Text(stringResource(R.string.history_last_days, CHART_DAYS), style = MaterialTheme.typography.bodySmall)

        HistoryBarChart(
            title = stringResource(R.string.history_chart_duration),
            bars = chartDays.map { chartDateLabel(it.epochDay) to it.durationSeconds },
            valueText = { if (it >= 60) "${it / 60}分" else "${it}秒" },
        )
        HistoryBarChart(
            title = stringResource(R.string.history_chart_reps),
            bars = chartDays.map { chartDateLabel(it.epochDay) to it.totalReps },
            valueText = { it.toString() },
        )

        Text(stringResource(R.string.history_daily_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HistoryTableRow(
            listOf(
                stringResource(R.string.history_col_date),
                stringResource(R.string.history_col_sessions),
                stringResource(R.string.history_col_duration),
                stringResource(R.string.history_col_reps),
            ),
            header = true,
        )
        HorizontalDivider()
        tableDays.filter { it.sessions > 0 }.reversed().forEach { d ->
            HistoryTableRow(
                listOf(chartDateLabel(d.epochDay), "${d.sessions}", DurationClock(d.durationSeconds), "${d.totalReps}"),
                header = false,
            )
        }
    }
}

/** 历史明细四列表：等宽列、数值居中，列间留白清晰不挤。 */
@Composable
private fun HistoryTableRow(cells: List<String>, header: Boolean) {
    val weights = listOf(1.1f, 1f, 1f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { i, cell ->
            Text(
                text = cell,
                modifier = Modifier.weight(weights.getOrElse(i) { 1f }),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** 紧凑时长：m:ss（≥1 小时为 h:mm:ss）。 */
private fun DurationClock(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.US, "%d:%02d", m, s)
}

@Composable
private fun HistoryStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HistoryBarChart(title: String, bars: List<Pair<String, Int>>, valueText: (Int) -> String) {
    val maxV = (bars.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { (_, value) ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (value > 0) {
                            Text(valueText(value), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                        }
                        val h = (100f * value / maxV).coerceAtLeast(if (value > 0) 4f else 1f)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(h.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    if (value > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                bars.forEach { (label, _) ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun chartDateLabel(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return "${d.monthValue}/${d.dayOfMonth}"
}

// ---------------- 能力与记录 ----------------

@Composable
private fun AbilityScreen(
    vm: AppViewModel,
    state: AppState,
    padding: PaddingValues,
    onOpenStandards: () -> Unit,
    onOpenAssess: () -> Unit,
) {
    val stats = com.orca.kegel.domain.HistoryStats.of(state.history)

    ScreenColumn(padding) {
        Text(stringResource(R.string.ability_current_level), style = MaterialTheme.typography.bodySmall)
        Text(LevelLabel(state.currentLevel), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ability_endurance), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ability_endurance_hold_total, DurationText(stats.enduranceHoldSecondsTotal)))
                Text(stringResource(R.string.ability_endurance_reps_total, stats.enduranceRepsTotal))
                Text(stringResource(R.string.ability_longest_hold, stats.longestHoldSeconds))
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ability_fast), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ability_fast_total, stats.fastRepsTotal))
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ability_sessions, stats.sessions))
                Text(stringResource(R.string.ability_days, stats.days))
            }
        }

        if (state.levelChanges.isNotEmpty()) {
            Text(stringResource(R.string.ability_level_history), fontWeight = FontWeight.SemiBold)
            state.levelChanges.takeLast(10).reversed().forEach { c ->
                Text(
                    "${LocalDate.ofEpochDay(c.epochDay)}  ${c.from?.label ?: "—"} → ${c.to.label}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenAssess, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ability_retest))
            }
            OutlinedButton(onClick = onOpenStandards, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_standard))
            }
        }
    }
}

// ---------------- 设置 ----------------

@Composable
private fun SettingsScreen(
    vm: AppViewModel,
    state: AppState,
    padding: PaddingValues,
    onOpenEhs: () -> Unit,
) {
    ScreenColumn(padding) {
        SettingGroup(stringResource(R.string.settings_theme)) {
            RadioRow(stringResource(R.string.settings_theme_system), state.theme == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
            RadioRow(stringResource(R.string.settings_theme_light), state.theme == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
            RadioRow(stringResource(R.string.settings_theme_dark), state.theme == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
        }

        SettingGroup(stringResource(R.string.settings_language)) {
            RadioRow(stringResource(R.string.settings_lang_system), state.language == LangMode.SYSTEM) { vm.setLanguage(LangMode.SYSTEM) }
            RadioRow(stringResource(R.string.settings_lang_zh), state.language == LangMode.ZH) { vm.setLanguage(LangMode.ZH) }
            RadioRow(stringResource(R.string.settings_lang_en), state.language == LangMode.EN) { vm.setLanguage(LangMode.EN) }
        }

        SettingGroup(stringResource(R.string.settings_sessions)) {
            LevelRules.sessionOptions.forEach { n ->
                RadioRow(
                    stringResource(R.string.settings_sessions_times, n),
                    state.sessionCount == n,
                ) { vm.setSessions(n) }
            }
            Text(
                stringResource(R.string.settings_sessions_default, LevelLabel(state.currentLevel), state.effectiveSessions()),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.settings_sessions_note), style = MaterialTheme.typography.bodySmall)
        }

        SettingGroup(stringResource(R.string.settings_reminder)) {
            LevelRules.reminderOptions.forEach { n ->
                RadioRow(stringResource(R.string.settings_frequency_times, n), state.reminderTimes == n) { vm.setReminderTimes(n) }
            }
            Text(stringResource(R.string.settings_reminder_note), style = MaterialTheme.typography.bodySmall)
        }

        SettingGroup(stringResource(R.string.settings_silent)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = state.silentMode, onCheckedChange = { vm.setSilent(it) })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_silent_desc), style = MaterialTheme.typography.bodySmall)
            }
        }

        SettingGroup(stringResource(R.string.settings_ehs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = state.ehsEnabled, onCheckedChange = { vm.setEhsEnabled(it) })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_ehs_desc), style = MaterialTheme.typography.bodySmall)
            }
            val summary = state.ehsRecords.sortedByDescending { it.epochDay }
                .joinToString(", ") { "${it.epochDay}:${it.score}" }
                .ifEmpty { stringResource(R.string.settings_ehs_none) }
            Text(stringResource(R.string.settings_ehs_history, summary), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenEhs) { Text(stringResource(R.string.settings_ehs_record)) }
                OutlinedButton(onClick = { vm.clearEhs() }) { Text(stringResource(R.string.settings_ehs_clear)) }
            }
        }

        SettingGroup(stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.settings_compliance), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.settings_offline), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}

// ---------------- EHS ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EhsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    var selected by rememberSaveable { mutableStateOf(0) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.ehs_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
        )
    }) { padding ->
        ScreenColumn(padding) {
            if (!state.ehsEnabled) {
                Text(stringResource(R.string.ehs_disabled), color = MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.ehs_intro), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            listOf(
                R.string.ehs_level_1, R.string.ehs_level_2, R.string.ehs_level_3, R.string.ehs_level_4,
            ).forEachIndexed { i, res ->
                RadioRow(stringResource(res), selected == i + 1) { selected = i + 1 }
            }
            Button(
                onClick = { if (selected in 1..4) { vm.addEhs(selected); onBack() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.ehsEnabled && selected in 1..4,
            ) { Text(stringResource(R.string.ehs_save)) }
        }
    }
}
