package com.orca.kegel.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orca.kegel.KegelApplication
import com.orca.kegel.audio.TrainingBus
import com.orca.kegel.audio.TrainingCommand
import com.orca.kegel.audio.TrainingState
import com.orca.kegel.data.AppState
import com.orca.kegel.data.LangMode
import com.orca.kegel.data.ThemeMode
import com.orca.kegel.domain.AssessmentResult
import com.orca.kegel.domain.EhsRecord
import com.orca.kegel.domain.Level
import com.orca.kegel.domain.PlanResult
import com.orca.kegel.domain.Quota
import com.orca.kegel.domain.SafetyReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 应用语言（AppCompat 官方机制）：SYSTEM＝空列表跟随系统，否则 zh-CN / en。 */
internal fun applyLanguage(mode: LangMode) {
    val locales = when (mode) {
        LangMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        LangMode.ZH -> LocaleListCompat.forLanguageTags("zh-CN")
        LangMode.EN -> LocaleListCompat.forLanguageTags("en")
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as KegelApplication).repository

    val state: StateFlow<AppState> = repo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    init {
        // 启动时恢复已存语言（AC-049），并在语言变化时即时切换（AC-047/048）。
        viewModelScope.launch {
            repo.state.collect { applyLanguage(it.language) }
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setTheme(mode) }
    fun setLanguage(mode: LangMode) = viewModelScope.launch { repo.setLanguage(mode) }
    fun setReminderTimes(times: Int) = viewModelScope.launch { repo.setReminderTimes(times) }
    fun setSessions(times: Int) = viewModelScope.launch { repo.setSessions(times) }
    fun setEhsEnabled(on: Boolean) = viewModelScope.launch { repo.setEhsEnabled(on) }

    fun setHalfDose(on: Boolean) = viewModelScope.launch {
        repo.setHalfDose(LocalDate.now().toEpochDay(), on)
    }

    fun setSilent(on: Boolean) {
        viewModelScope.launch {
            repo.setSilentMode(on)
            TrainingBus.send(TrainingCommand.SetSilent(on))
        }
    }

    fun selectLevel(level: Level, reason: String) = viewModelScope.launch { repo.setLevel(level, reason) }

    fun saveAssessment(result: AssessmentResult) = viewModelScope.launch {
        repo.saveAssessment(result)
    }

    /** 摸底实际已开始的收缩计入当日耐力额度（不计完成会话）。 */
    fun addAssessmentUsage(epochDay: Long, enduranceStarted: Int) = viewModelScope.launch {
        repo.addAssessmentUsage(epochDay, enduranceStarted)
    }

    fun adoptAssessment(level: Level) = viewModelScope.launch {
        repo.setLevel(level, "assessment")
    }

    fun dismissRetestPrompt() = viewModelScope.launch { repo.dismissRetestPrompt() }

    fun addEhs(score: Int) = viewModelScope.launch {
        repo.addEhs(EhsRecord(LocalDate.now().toEpochDay(), score))
    }

    fun clearEhs() = viewModelScope.launch { repo.clearEhs() }

    /**
     * 今日计划（已含会话次数 / 半量 / 双维余量；等级与会话数按当日冻结值）。
     * 首页始终可开始；安全只由真实症状出口（疲劳/疼痛/无法放松等）处理。
     */
    fun todayPlan(state: AppState, nowMs: Long = System.currentTimeMillis()): PlanResult {
        val today = LocalDate.now().toEpochDay()
        return Quota.build(
            level = state.dayLevel(today),
            sessions = state.daySessions(today),
            halfDose = state.halfDoseOn(today),
            sessionIndex = state.nextSessionIndex(today),
            usedE = state.usedE(today),
            usedF = state.usedF(today),
        )
    }

    fun startToday() {
        val s = state.value
        when (val result = todayPlan(s)) {
            is PlanResult.Ok -> {
                val today = LocalDate.now().toEpochDay()
                viewModelScope.launch { repo.freezeDose(today, result.plan.level, result.plan.sessionCount) }
                TrainingBus.send(TrainingCommand.Start(result.plan, s.silentMode))
            }
            is PlanResult.Blocked -> Unit
        }
    }

    fun pause() = TrainingBus.send(TrainingCommand.Pause)
    fun resume() = TrainingBus.send(TrainingCommand.Resume)
    fun stop() = TrainingBus.send(TrainingCommand.Stop)
    fun safetyExit(reason: SafetyReason) = TrainingBus.send(TrainingCommand.SafetyExit(reason))

    /** 组间休息延长（可延长不可缩短） */
    fun extendRest(seconds: Int) = TrainingBus.send(TrainingCommand.ExtendRest(seconds * 1000L))

    fun acknowledgeOutcome() = TrainingState.reset()

    /** 切换主题/语言时训练不中断：只改设置，不动 TrainingState */
    fun currentSilentMode(): Boolean = state.value.silentMode
}
