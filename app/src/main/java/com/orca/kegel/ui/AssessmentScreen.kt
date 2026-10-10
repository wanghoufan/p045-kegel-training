package com.orca.kegel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orca.kegel.R
import com.orca.kegel.audio.AssessmentAudio
import com.orca.kegel.domain.AssessmentResult
import com.orca.kegel.domain.ControlQuality
import com.orca.kegel.domain.Recommender
import com.orca.kegel.domain.RelaxQuality
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

/** 第 3 项每轮之间的放松休息时长（秒） */
private const val ASSESS_REST_SECONDS = 5

/**
 * 摸底：5 项（动作控制 / 耐力秒数 / 耐力次数 / 快速次数 / 放松质量）。
 * 第 2、3 项带计时/倒计时引导，用户跟着做，不靠脑子数；快速次数一次性选择，不逐次点击。
 * 可选、可跳过；结果只作建议，不锁等级；不静默覆盖历史。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentScreen(vm: AppViewModel, onClose: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var control by rememberSaveable { mutableStateOf("") }
    /** 0＝尚未测量，1–10＝实际保持秒数 */
    var holdSec by rememberSaveable { mutableIntStateOf(0) }
    /** 0＝尚未记次 */
    var reps by rememberSaveable { mutableIntStateOf(0) }
    var fastReps by rememberSaveable { mutableIntStateOf(5) }
    var relax by rememberSaveable { mutableStateOf("") }
    var saved by remember { mutableStateOf<AssessmentResult?>(null) }

    // 提前加载摸底提示音，保证第一次点击就有声音
    LaunchedEffect(Unit) { AssessmentAudio.warmUp() }

    fun next() {
        if (step < 4) {
            step++
        } else {
            val result = AssessmentResult(
                epochDay = LocalDate.now().toEpochDay(),
                controlQuality = runCatching { ControlQuality.valueOf(control) }.getOrDefault(ControlQuality.UNSURE),
                holdSeconds = holdSec.coerceIn(1, 10),
                enduranceReps = reps.coerceIn(1, 10),
                fastReps = fastReps.coerceIn(1, 10),
                relaxQuality = runCatching { RelaxQuality.valueOf(relax) }.getOrDefault(RelaxQuality.UNCERTAIN),
            )
            vm.saveAssessment(result)
            saved = result
            step = 5
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.assess_title)) },
            navigationIcon = { TextButton(onClick = onClose) { Text(stringResource(R.string.back)) } },
            actions = {
                if (step < 5) TextButton(onClick = onClose) { Text(stringResource(R.string.assess_skip)) }
            },
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.assess_intro), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()

            when (step) {
                0 -> {
                    Text(stringResource(R.string.assess_step_of, 1), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.assess_control_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.assess_control_body), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.assess_control_q), fontWeight = FontWeight.SemiBold)
                    ChoiceRow(stringResource(R.string.assess_control_clear), control == ControlQuality.CLEAR.name) { control = ControlQuality.CLEAR.name }
                    ChoiceRow(stringResource(R.string.assess_control_unsure), control == ControlQuality.UNSURE.name) { control = ControlQuality.UNSURE.name }
                    ChoiceRow(stringResource(R.string.assess_control_compensating), control == ControlQuality.COMPENSATING.name) { control = ControlQuality.COMPENSATING.name }
                }
                1 -> {
                    Text(stringResource(R.string.assess_step_of, 2), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.assess_seconds_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.assess_seconds_body), style = MaterialTheme.typography.bodyMedium)
                    HoldTimerStep(value = holdSec, onRecorded = { holdSec = it })
                }
                2 -> {
                    Text(stringResource(R.string.assess_step_of, 3), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.assess_reps_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.assess_reps_body), style = MaterialTheme.typography.bodyMedium)
                    RepsTimedStep(value = reps, holdSec = holdSec.coerceAtLeast(1), onCount = { reps = it })
                }
                3 -> {
                    Text(stringResource(R.string.assess_step_of, 4), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.assess_fast_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.assess_fast_body), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.assess_counter, fastReps, 10),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.assess_fast_pick), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = fastReps.toFloat(),
                        onValueChange = { fastReps = it.roundToInt().coerceIn(1, 10) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                }
                4 -> {
                    Text(stringResource(R.string.assess_step_of, 5), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.assess_relax_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.assess_relax_q), fontWeight = FontWeight.SemiBold)
                    ChoiceRow(stringResource(R.string.assess_relax_good), relax == RelaxQuality.GOOD.name) { relax = RelaxQuality.GOOD.name }
                    ChoiceRow(stringResource(R.string.assess_relax_uncertain), relax == RelaxQuality.UNCERTAIN.name) { relax = RelaxQuality.UNCERTAIN.name }
                    ChoiceRow(stringResource(R.string.assess_relax_poor), relax == RelaxQuality.POOR.name) { relax = RelaxQuality.POOR.name }
                    Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    val r = saved
                    Text(stringResource(R.string.assess_result_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    val recommended = r?.let { Recommender.recommend(it) }
                    if (recommended != null) {
                        Text(
                            stringResource(R.string.assess_result_body, stringResource(R.string.common_level, recommended.num)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(
                            onClick = { vm.adoptAssessment(recommended); onClose() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.assess_accept)) }
                        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.assess_keep))
                        }
                    } else {
                        Text(stringResource(R.string.assess_result_veto), color = MaterialTheme.colorScheme.error)
                        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    }
                    Text(stringResource(R.string.assess_saved), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (step < 5) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step > 0) {
                        OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.assess_prev))
                        }
                    }
                    Button(
                        onClick = { next() },
                        modifier = Modifier.weight(1f),
                        enabled = when (step) {
                            0 -> control.isNotEmpty()
                            1 -> holdSec >= 1
                            2 -> reps >= 1
                            4 -> relax.isNotEmpty()
                            else -> true
                        },
                    ) {
                        Text(
                            if (step == 4) stringResource(R.string.common_confirm)
                            else stringResource(R.string.assess_next)
                        )
                    }
                }
                Card {
                    Text(
                        stringResource(R.string.assess_skip),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * 第 2 项：耐力保持秒数。正计时（1→10 秒），用户跟着做；
 * 每秒一声「滴」提示 + 最后 3 秒播报 3/2/1 倒数，到 10 秒播「完成」并自动记录；
 * 撑不住点「停」，播「放松」并记录实际秒数。全程有声音引导，不必盯屏。
 */
@Composable
private fun HoldTimerStep(value: Int, onRecorded: (Int) -> Unit) {
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        if (running) {
            elapsed = 0
            while (true) {
                delay(1000)
                elapsed += 1
                when {
                    elapsed >= 10 -> {
                        AssessmentAudio.play(R.raw.cue_num1)
                        break
                    }
                    elapsed == 8 -> AssessmentAudio.play(R.raw.cue_num3)
                    elapsed == 9 -> AssessmentAudio.play(R.raw.cue_num2)
                    else -> AssessmentAudio.play(R.raw.tick)
                }
            }
            delay(650)
            AssessmentAudio.play(R.raw.cue_done)
            onRecorded(10)
            running = false
        }
    }

    val shown = if (running) elapsed else value
    Text(
        stringResource(R.string.assess_counter, shown, 10),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
    )
    if (running) {
        Text(stringResource(R.string.assess_hold_running), style = MaterialTheme.typography.bodyMedium)
    } else if (value >= 1) {
        Text(stringResource(R.string.assess_hold_recorded, value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (running) {
            OutlinedButton(onClick = {
                AssessmentAudio.play(R.raw.cue_relax)
                onRecorded(elapsed.coerceAtLeast(1))
                running = false
            }) {
                Text(stringResource(R.string.assess_hold_stop))
            }
        } else {
            Button(onClick = {
                AssessmentAudio.play(R.raw.cue_tighten)
                elapsed = 0
                running = true
            }) {
                Text(stringResource(if (value >= 1) R.string.assess_hold_retry else R.string.assess_hold_start))
            }
        }
    }
}

/**
 * 第 3 项：耐力次数。每轮「收紧」倒计时（时长＝第 2 项实测秒数）→ 自动计一次 →
 * 放松休息「同样倒计时」→ 休息数完自动进入下一轮（不必手动点击）；休息不够可「再休息 5 秒」，
 * 撑不住点「结束本项」。最多 10 次。全程有声音引导，不必盯屏。
 */
@Composable
private fun RepsTimedStep(value: Int, holdSec: Int, onCount: (Int) -> Unit) {
    // phase: idle / hold / rest
    var phase by rememberSaveable { mutableStateOf("idle") }
    var remaining by rememberSaveable { mutableIntStateOf(0) }
    var restLeft by rememberSaveable { mutableIntStateOf(0) }
    /** 本地完成的次数（自动推进时避免读到旧的 value） */
    var done by rememberSaveable { mutableIntStateOf(value) }

    LaunchedEffect(phase) {
        when (phase) {
            "hold" -> {
                remaining = holdSec
                AssessmentAudio.play(R.raw.cue_tighten)
                var e = 0
                while (e < holdSec) {
                    delay(1000)
                    e += 1
                    remaining = holdSec - e
                    when (remaining) {
                        1 -> AssessmentAudio.play(R.raw.cue_num1)
                        2 -> AssessmentAudio.play(R.raw.cue_num2)
                        3 -> AssessmentAudio.play(R.raw.cue_num3)
                        else -> if (remaining > 3) AssessmentAudio.play(R.raw.tick)
                    }
                }
                // 本轮完成 → 自动计数 → 进入休息
                done += 1
                onCount(done)
                if (done >= AssessmentResult.MAX_REPS) AssessmentAudio.play(R.raw.cue_done)
                phase = "rest"
            }
            "rest" -> {
                restLeft = ASSESS_REST_SECONDS
                AssessmentAudio.play(R.raw.cue_relax)
                while (restLeft > 0) {
                    delay(1000)
                    restLeft -= 1
                    when (restLeft) {
                        1 -> AssessmentAudio.play(R.raw.cue_num1)
                        2 -> AssessmentAudio.play(R.raw.cue_num2)
                        3 -> AssessmentAudio.play(R.raw.cue_num3)
                        else -> if (restLeft > 3) AssessmentAudio.play(R.raw.tick)
                    }
                }
                // 休息数完 → 自动进入下一轮（除非已到上限）
                phase = if (done >= AssessmentResult.MAX_REPS) "idle" else "hold"
            }
        }
    }

    Text(
        stringResource(R.string.assess_reps_done, done),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
    )

    when (phase) {
        "hold" -> {
            Text(stringResource(R.string.assess_reps_holding), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.assess_sec, remaining),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(onClick = { phase = "idle" }) {
                Text(stringResource(R.string.assess_end_item))
            }
        }
        "rest" -> {
            Text(stringResource(R.string.assess_reps_rest), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.assess_sec, restLeft),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.assess_reps_rest_hint), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { restLeft += ASSESS_REST_SECONDS }) {
                    Text(stringResource(R.string.assess_reps_more_rest))
                }
                OutlinedButton(onClick = { phase = "idle" }) {
                    Text(stringResource(R.string.assess_end_item))
                }
            }
        }
        else -> {
            if (done >= AssessmentResult.MAX_REPS) {
                Text(stringResource(R.string.assess_reps_limit), style = MaterialTheme.typography.bodySmall)
            } else {
                Button(onClick = { phase = "hold" }) {
                    Text(stringResource(R.string.assess_reps_start, holdSec))
                }
            }
            Text(stringResource(R.string.assess_reps_auto_hint, holdSec), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}