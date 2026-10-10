package com.orca.kegel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orca.kegel.R
import com.orca.kegel.audio.TrainingState
import com.orca.kegel.domain.PauseReason
import com.orca.kegel.domain.Phase
import com.orca.kegel.domain.SafetyReason
import com.orca.kegel.domain.Section
import com.orca.kegel.domain.TrainingStage

@Composable
private fun phaseLabel(phase: Phase): String = stringResource(
    when (phase) {
        Phase.READY -> R.string.training_ready
        Phase.TIGHTEN -> R.string.training_tighten
        Phase.HOLD -> R.string.training_hold
        Phase.RELAX -> R.string.training_relax
        Phase.REST -> R.string.training_rest
        Phase.NEXT -> R.string.training_next
        Phase.QUICK_TIGHTEN -> R.string.training_quick_tighten
        Phase.QUICK_RELAX -> R.string.training_quick_relax
        Phase.DONE -> R.string.training_done
    }
)

@Composable
private fun sectionLabel(section: Section): String = stringResource(
    when (section) {
        Section.ENDURANCE -> R.string.training_endurance
        Section.REST -> R.string.training_rest
        Section.FAST -> R.string.training_fast
        Section.FINISH -> R.string.training_done
    }
)

@Composable
private fun phaseCaption(phase: Phase, section: Section): String? = when {
    phase == Phase.HOLD -> stringResource(R.string.training_quality_hint)
    section == Section.REST -> stringResource(R.string.training_fast_intro)
    else -> null
}

@Composable
private fun pauseReasonText(reason: PauseReason): String? = when (reason) {
    PauseReason.MANUAL -> stringResource(R.string.pause_reason_manual)
    PauseReason.FOCUS_LOST -> stringResource(R.string.pause_reason_focus)
    PauseReason.NOISY -> stringResource(R.string.pause_reason_noisy)
    PauseReason.SCREEN_OFF -> stringResource(R.string.pause_reason_screen)
    PauseReason.NONE -> null
}

/**
 * 训练页：Audio First —— 语音为主、屏幕为辅。
 * 不覆盖用户主题（AC-046 / FR-022）；低调模式仅字幕 + 倒计时，不外放。
 */
@Composable
fun TrainingScreen(vm: AppViewModel) {
    val stage by TrainingState.stage.collectAsState()
    val phase by TrainingState.phase.collectAsState()
    val section by TrainingState.section.collectAsState()
    val repIndex by TrainingState.repIndex.collectAsState()
    val repTotal by TrainingState.repTotal.collectAsState()
    val remainingMs by TrainingState.remainingMs.collectAsState()
    val pauseReason by TrainingState.pauseReason.collectAsState()
    val silent by TrainingState.silentMode.collectAsState()
    val level by TrainingState.level.collectAsState()

    var confirmStop by remember { mutableStateOf(false) }
    var safetyOpen by remember { mutableStateOf(false) }

    val paused = stage == TrainingStage.PAUSED
    val seconds = ((remainingMs + 999) / 1000).coerceAtLeast(0)
    val caption = phaseCaption(phase, section)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.common_level, level.num) + " · " + sectionLabel(section),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (silent) stringResource(R.string.training_silent_on) else stringResource(R.string.training_silent_off),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        phaseLabel(phase),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        seconds.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (repIndex != null && repTotal != null && section != Section.REST) {
                        Text(
                            stringResource(R.string.training_rep_progress, repIndex!!, repTotal!!),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (caption != null) {
                        Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { if (paused) vm.resume() else vm.pause() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (paused) stringResource(R.string.training_resume) else stringResource(R.string.training_pause),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { confirmStop = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.training_stop))
                        }
                        OutlinedButton(onClick = { safetyOpen = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.training_safety_exit))
                        }
                    }
                }
            }

            if (paused) {
                PauseOverlay(
                    reasonText = pauseReasonText(pauseReason),
                    onResume = { vm.resume() },
                    onStop = { confirmStop = true },
                )
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(R.string.training_stop)) },
            text = { Text(stringResource(R.string.pause_need_resume)) },
            confirmButton = {
                TextButton(onClick = { confirmStop = false; vm.stop() }) {
                    Text(stringResource(R.string.finish_aborted))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (safetyOpen) {
        SafetyExitDialog(
            onDismiss = { safetyOpen = false },
            onConfirm = { reason -> safetyOpen = false; vm.safetyExit(reason) },
        )
    }
}

@Composable
private fun PauseOverlay(reasonText: String?, onResume: () -> Unit, onStop: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (reasonText != null) {
                    Text(reasonText, style = MaterialTheme.typography.bodyLarge)
                }
                Text(stringResource(R.string.pause_need_resume), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.training_resume), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.training_stop))
                }
            }
        }
    }
}

@Composable
private fun SafetyExitDialog(onDismiss: () -> Unit, onConfirm: (SafetyReason) -> Unit) {
    var choice by remember { mutableStateOf<SafetyReason?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.safety_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.safety_question), fontWeight = FontWeight.SemiBold)
                SafetyRow(stringResource(R.string.safety_pain), choice == SafetyReason.PAIN) { choice = SafetyReason.PAIN }
                SafetyRow(stringResource(R.string.safety_urinary), choice == SafetyReason.URINARY) { choice = SafetyReason.URINARY }
                SafetyRow(stringResource(R.string.safety_relax_fail), choice == SafetyReason.RELAX_FAIL) { choice = SafetyReason.RELAX_FAIL }
                HorizontalDivider()
                Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { choice?.let(onConfirm) }, enabled = choice != null) {
                Text(stringResource(R.string.safety_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun SafetyRow(label: String, selected: Boolean, onSelect: () -> Unit) {
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

/**
 * 结束页：展示本次结果（已自动落库），安全停止时给出提示。
 * 返回首页会重置进程内训练状态。
 */
@Composable
fun FinishScreen(vm: AppViewModel) {
    val outcome by TrainingState.outcome.collectAsState()
    val stage by TrainingState.stage.collectAsState()
    val safetyStopped = stage == TrainingStage.SAFETY_STOPPED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.finish_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(if (outcome?.completed == true) R.string.finish_completed else R.string.finish_aborted),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider()

        val o = outcome
        if (o != null) {
            Text(stringResource(R.string.finish_endurance_reps, o.enduranceReps), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.finish_endurance_hold, o.enduranceHoldSeconds), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.finish_fast_reps, o.fastReps), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.finish_duration, DurationText(o.durationSeconds)), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.finish_saved), style = MaterialTheme.typography.bodySmall)
        }

        if (safetyStopped || o?.safetyReason != null) {
            Card {
                Text(
                    stringResource(R.string.safety_stopped),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        HorizontalDivider()
        Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.bodySmall)

        Button(
            onClick = { vm.acknowledgeOutcome() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.finish_back_home), style = MaterialTheme.typography.titleMedium) }
    }
}