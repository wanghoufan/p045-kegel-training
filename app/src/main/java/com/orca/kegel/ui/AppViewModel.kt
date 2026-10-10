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

    /** 今日半量开关（UI 临时状态，不落库） */
    val halfDose = MutableStateFlow(false)

    init {
        // 启动时恢复已存语言（AC-049），并在语言变化时即时切换（AC-047/048）。
        viewModelScope.launch {
            repo.state.collect { applyLanguage(it.language) }
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setTheme(mode) }
    fun setLanguage(mode: LangMode) = viewModelScope.launch { repo.setLanguage(mode) }
    fun setFrequency(times: Int) = viewModelScope.launch { repo.setFrequency(times) }
    fun setEhsEnabled(on: Boolean) = viewModelScope.launch { repo.setEhsEnabled(on) }

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

    fun adoptAssessment(level: Level) = viewModelScope.launch {
        repo.setLevel(level, "assessment")
    }

    fun dismissRetestPrompt() = viewModelScope.launch { repo.dismissRetestPrompt() }

    fun addEhs(score: Int) = viewModelScope.launch {
        repo.addEhs(EhsRecord(LocalDate.now().toEpochDay(), score))
    }

    fun clearEhs() = viewModelScope.launch { repo.clearEhs() }

    /** 今日计划（已含频率降量 / 半量 / 当日额度） */
    fun todayPlan(state: AppState): PlanResult =
        Quota.build(state.currentLevel, state.dailyFrequency, halfDose.value, state.remainingToday())

    fun startToday() {
        val s = state.value
        when (val result = todayPlan(s)) {
            is PlanResult.Ok -> TrainingBus.send(TrainingCommand.Start(result.plan, s.silentMode))
            is PlanResult.Blocked -> Unit
        }
    }

    fun startLevelPreview(level: Level) {
        val s = state.value
        when (val result = Quota.build(level, s.dailyFrequency, halfDose.value, s.remainingToday())) {
            is PlanResult.Ok -> TrainingBus.send(TrainingCommand.Start(result.plan, s.silentMode))
            is PlanResult.Blocked -> Unit
        }
    }

    fun pause() = TrainingBus.send(TrainingCommand.Pause)
    fun resume() = TrainingBus.send(TrainingCommand.Resume)
    fun stop() = TrainingBus.send(TrainingCommand.Stop)
    fun safetyExit(reason: SafetyReason) = TrainingBus.send(TrainingCommand.SafetyExit(reason))

    fun acknowledgeOutcome() = TrainingState.reset()

    /** 切换主题/语言时训练不中断：只改设置，不动 TrainingState */
    fun currentSilentMode(): Boolean = state.value.silentMode
}