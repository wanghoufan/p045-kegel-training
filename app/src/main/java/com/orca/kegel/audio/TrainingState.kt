package com.orca.kegel.audio

import com.orca.kegel.domain.Level
import com.orca.kegel.domain.PauseReason
import com.orca.kegel.domain.Phase
import com.orca.kegel.domain.SafetyReason
import com.orca.kegel.domain.Section
import com.orca.kegel.domain.SessionPlan
import com.orca.kegel.domain.TrainingStage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** 一次会话的结果（供结束页展示） */
data class SessionOutcome(
    val level: Level,
    val enduranceReps: Int,
    val enduranceHoldSeconds: Int,
    val fastReps: Int,
    val durationSeconds: Int,
    val completed: Boolean,
    val safetyReason: SafetyReason?,
    val halfDose: Boolean,
)

/** 训练实时状态（进程内单源，UI 只读） */
object TrainingState {
    val stage: MutableStateFlow<TrainingStage> = MutableStateFlow(TrainingStage.IDLE)
    val level: MutableStateFlow<Level> = MutableStateFlow(Level.L1)
    val phase: MutableStateFlow<Phase> = MutableStateFlow(Phase.READY)
    val section: MutableStateFlow<Section> = MutableStateFlow(Section.ENDURANCE)
    val repIndex = MutableStateFlow<Int?>(null)
    val repTotal = MutableStateFlow<Int?>(null)
    val remainingMs = MutableStateFlow(0L)
    val stepDurationMs = MutableStateFlow(0L)
    val stepIndex = MutableStateFlow(0)
    val stepCount = MutableStateFlow(0)
    val pauseReason = MutableStateFlow(PauseReason.NONE)
    val silentMode = MutableStateFlow(false)
    val plan = MutableStateFlow<SessionPlan?>(null)
    val outcome = MutableStateFlow<SessionOutcome?>(null)

    val stageFlow: StateFlow<TrainingStage> get() = stage

    fun reset() {
        stage.value = TrainingStage.IDLE
        phase.value = Phase.READY
        section.value = Section.ENDURANCE
        repIndex.value = null
        repTotal.value = null
        remainingMs.value = 0
        stepDurationMs.value = 0
        stepIndex.value = 0
        stepCount.value = 0
        pauseReason.value = PauseReason.NONE
        plan.value = null
        outcome.value = null
    }
}

sealed interface TrainingCommand {
    data class Start(val plan: SessionPlan, val silent: Boolean) : TrainingCommand
    data object Pause : TrainingCommand
    data object Resume : TrainingCommand
    data object Stop : TrainingCommand
    data class SafetyExit(val reason: SafetyReason) : TrainingCommand
    data class SetSilent(val silent: Boolean) : TrainingCommand
}

/** 进程内命令总线（UI → 前台服务） */
object TrainingBus {
    private val _commands = MutableSharedFlow<TrainingCommand>(extraBufferCapacity = 32)
    val commands: SharedFlow<TrainingCommand> = _commands
    fun send(command: TrainingCommand) {
        _commands.tryEmit(command)
    }
}