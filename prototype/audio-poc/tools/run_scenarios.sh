#!/usr/bin/env bash
# PRE-02 技术样机｜真机五场景取证脚本（主机侧驱动）
#
# 用法：bash tools/run_scenarios.sh <setup|s1|s2|s3|s4|s5|teardown>
#   每个场景单独调用，便于逐条核对产出。
#
# 设备：Redmi Note 11T Pro（22041216UC）/ Android 14 / API 34 / adb serial IN9LZTAYV4UGU4JF
# 证据目录：<repo>/docs/qa/PRE-02-技术样机/
#
# 已知限制（见结论文档）：
#   * Android `screenrecord` 在「锁屏界面显示中」报 INVALID_LAYER_STACK 无法启动，
#     因此录屏在“熄屏”状态下进行，锁屏界面另用截图留证。
#   * screenrecord 无法录制内部音频（Android 无该能力），音轨证据用 App 单调时钟日志
#     + dumpsys media_session/audio 替代。
set -uo pipefail

D="${POC_SERIAL:-IN9LZTAYV4UGU4JF}"
PKG=com.orca.audiopoc
HERE="$(cd "$(dirname "$0")" && pwd)"
EV="$(cd "$HERE/../../.." && pwd)/docs/qa/PRE-02-技术样机"
mkdir -p "$EV"

ADB=(adb -s "$D")
dev() { "${ADB[@]}" shell "$@"; }
say() { printf '\n=== %s | %s ===\n' "$(date +%H:%M:%S)" "$*"; }

center() {
  echo "$1" | sed -E 's/.*\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\].*/\1 \2 \3 \4/' \
    | awk '{printf "%d %d", ($1+$3)/2, ($2+$4)/2}'
}

resolve_coords() {
  local xml bp bs bg tries=0
  while :; do
    dev uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    xml="$("${ADB[@]}" exec-out cat /sdcard/ui.xml 2>/dev/null | tr '<' '\n<')"
    bp=$(echo "$xml" | grep -F 'text="开始/继续"' | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1)
    [ -n "$bp" ] && break
    tries=$((tries + 1))
    if [ "$tries" -ge 6 ]; then echo "WARN: 未能定位控件（UI 未就绪）"; break; fi
    sleep 2
  done
  bs=$(echo "$xml" | grep -F 'checkable="true"' | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1)
  bg=$(echo "$xml" | grep -F '抢占音频焦点' | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1)
  PLAY=$(center "$bp"); SILENT=$(center "$bs"); GRAB=$(center "$bg")
  echo "coords: PLAY=($PLAY) SILENT=($SILENT) GRAB=($GRAB)"
}

wake() {
  dev input keyevent 224 >/dev/null 2>&1
  sleep 1
  dev wm dismiss-keyguard >/dev/null 2>&1
  sleep 1
}

ensure_awake() {
  local w kg
  for _ in 1 2 3 4 5 6 7 8; do
    w=$(dev dumpsys power 2>&1 | grep -m1 'mWakefulness=' | tr -d ' ')
    if [ "$w" = "mWakefulness=Awake" ]; then
      kg=$(dev dumpsys window 2>&1 | grep -m1 -i 'isKeyguardShowing=' | tr -d ' ')
      case "$kg" in
        *true*) dev wm dismiss-keyguard >/dev/null 2>&1; sleep 1 ;;
      esac
      echo "screen: $w $kg"
      return 0
    fi
    dev input keyevent 224 >/dev/null 2>&1
    sleep 2
  done
  echo "WARN: 屏幕未能唤醒 ($w)"
  return 1
}

grant_notif() {
  dev pm grant "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
  dev appops set "$PKG" POST_NOTIFICATION allow >/dev/null 2>&1
  dev dumpsys package "$PKG" 2>&1 | grep -m1 'POST_NOTIFICATIONS: granted' | sed 's/^ *//'
}

bring_app_front() {
  local top
  for _ in 1 2 3; do
    ensure_awake >/dev/null 2>&1
    dev wm dismiss-keyguard >/dev/null 2>&1
    dev am start -n "$PKG/.MainActivity" >/dev/null 2>&1
    sleep 3
    top=$(dev dumpsys activity activities 2>&1 | grep -E "ResumedActivity" | head -1)
    case "$top" in
      *"$PKG"*) echo "foreground: $top"; return 0 ;;
    esac
    sleep 2
  done
  echo "WARN: 应用未到前台，当前 $top"
  return 1
}

reset_app() {
  wake
  dev pm clear "$PKG" >/dev/null 2>&1
  grant_notif
  sleep 1
  bring_app_front
  resolve_coords
}

# 点「开始/继续」并确认真的进入 PLAYING（最多重试 3 次）
tap_play() {
  local st
  for _ in 1 2 3; do
    ensure_awake >/dev/null 2>&1
    dev input tap "$PLAY"
    sleep 3
    st=$(dev dumpsys media_session 2>&1 | grep -A14 "package=$PKG" | grep -m1 'state=' | sed 's/^ *//')
    case "$st" in
      *PLAYING*) echo "play OK: $st"; return 0 ;;
    esac
    echo "play retry -> $st"
    resolve_coords
    sleep 1
  done
  echo "WARN: 未能进入 PLAYING"
  return 1
}

start_rec() { dev "rm -f /sdcard/$1.mp4; nohup screenrecord --bit-rate 8000000 --time-limit $2 /sdcard/$1.mp4 >/dev/null 2>&1 &" >/dev/null 2>&1; sleep 2; echo "rec -> /sdcard/$1.mp4 (limit ${2}s)"; }
stop_rec() { dev 'for p in $(pidof screenrecord); do kill -INT $p; done' >/dev/null 2>&1; sleep 3; dev "ls -la /sdcard/$1.mp4"; }
pull_rec() { "${ADB[@]}" pull "/sdcard/$1.mp4" "$EV/$1.mp4" >/dev/null 2>&1; echo "video -> $1.mp4 ($(stat -f%z "$EV/$1.mp4" 2>/dev/null) bytes)"; }
snap_log() { "${ADB[@]}" exec-out run-as "$PKG" cat files/poc-events.txt > "$EV/poc-events-$1.txt" 2>/dev/null; echo "log -> poc-events-$1.txt ($(wc -l < "$EV/poc-events-$1.txt") lines)"; }
dumps() { dev "dumpsys $1" > "$EV/$2" 2>&1; echo "dumpsys $1 -> $2"; }
shot() { "${ADB[@]}" exec-out screencap -p > "$EV/$1" 2>/dev/null; echo "shot -> $1 ($(stat -f%z "$EV/$1" 2>/dev/null) bytes)"; }

# 保持熄屏：MIUI 会在媒体通知更新时短暂点亮锁屏，故周期性补发 SLEEP(223) 抵消
hold_screen_off() {
  local secs="$1" i n=$(( $1 / 9 ))
  for ((i = 0; i < n; i++)); do sleep 9; dev input keyevent 223 >/dev/null 2>&1; done
}
power_state() { dev dumpsys power 2>&1 | grep -m1 'mWakefulness=' | sed 's/^ *//' | tee "$EV/$1"; }
kg_state() { dev dumpsys window 2>&1 | grep -m1 -i 'isKeyguardShowing=' | sed 's/^ *//' | tee -a "$EV/$1"; }
wake_to_keyguard() { dev input keyevent 224 >/dev/null 2>&1; sleep 3; }

case "${1:-}" in
  setup)
    say "setup：启用滑动锁屏 + 关闭 stayon + 清数据 + 授权 + 启动"
    dev svc power stayon false
    dev settings put secure lockscreen.disabled 0
    echo "lockscreen.disabled=$(dev settings get secure lockscreen.disabled)"
    reset_app
    ;;

  s1)
    say "S1 锁屏 ≥1 分钟持续播放（语音模式）"
    reset_app
    start_rec S1-锁屏持续播放 170
    tap_play
    sleep 8
    shot S1_01_playing.png
    dumps media_session S1_playing.txt
    say "熄屏锁屏并保持屏幕关闭 72 秒（每 9 秒补发 SLEEP，抵消锁屏通知亮屏）"
    dev input keyevent 26
    hold_screen_off 72
    power_state S1_power_off.txt
    dumps media_session S1_locked_72s.txt
    dumps audio S1_audio_locked.txt
    say "结束录屏（锁屏界面无法 screenrecord，改截图留证）"
    stop_rec S1-锁屏持续播放
    pull_rec S1-锁屏持续播放
    say "唤醒到锁屏界面（3 秒内截图，避免被锁屏通知自动解锁）"
    wake_to_keyguard
    shot S1_02_lockscreen.png
    kg_state S1_keyguard_75s.txt
    dumps media_session S1_locked_75s.txt
    sleep 3
    say "再次熄屏并保持 36 秒"
    dev input keyevent 26
    hold_screen_off 36
    power_state S1_power_off_2.txt
    wake_to_keyguard
    shot S1_03_lockscreen_110s.png
    kg_state S1_keyguard_110s.txt
    dumps media_session S1_locked_110s.txt
    say "解锁回前台"
    dev wm dismiss-keyguard >/dev/null 2>&1
    sleep 3
    shot S1_04_after_unlock.png
    dumps media_session S1_after_unlock.txt
    snap_log S1
    ;;

  s2)
    say "S2 耳机拔插（ACTION_AUDIO_BECOMING_NOISY）"
    reset_app
    tap_play
    sleep 5
    echo "-- 当前音频路由/设备 --"
    dev dumpsys audio 2>&1 | grep -i -E "headset|wired|a2dp|bluetooth|DeviceInfo" | head -6 | tee "$EV/S2_audio_route.txt"
    echo "-- 证据1：本应用已注册 becoming-noisy 接收器（ExoPlayer AudioBecomingNoisyManager） --"
    dev dumpsys activity broadcasts 2>&1 | grep -A4 "ReceiverList{.*com.orca.audiopoc" | grep -B1 -A3 "AUDIO_BECOMING_NOISY" | tee "$EV/S2_receiver_registration.txt"
    echo "-- 证据2：shell 无法注入该受保护广播（SecurityException） --"
    {
      echo "\$ adb shell am broadcast -a android.media.AUDIO_BECOMING_NOISY"
      dev am broadcast -a android.media.AUDIO_BECOMING_NOISY -p com.orca.audiopoc --user 0 2>&1
      echo
      echo "\$ adb shell am broadcast -a android.intent.action.HEADSET_PLUG --ei state 0 --ei microphone 1 --es name test"
      dev am broadcast -a android.intent.action.HEADSET_PLUG --ei state 0 --ei microphone 1 --es name test 2>&1
    } > "$EV/S2_protected_broadcast_proof.txt" 2>&1
    cat "$EV/S2_protected_broadcast_proof.txt"
    sleep 3
    echo "-- 触发后（预期仍未暂停，证明广播未送达） --"
    dumps media_session S2_after_broadcast.txt
    dumps audio S2_audio_after.txt
    snap_log S2
    ;;

  s3)
    say "S3 音频焦点抢占与返回（应用内模拟；真机无 SIM，来电通路不可复现）"
    reset_app
    start_rec S3-焦点抢占 70
    tap_play
    sleep 6
    shot S3_01_playing.png
    dumps media_session S3_playing.txt
    say "触发 GAIN_TRANSIENT 抢占"
    ensure_awake >/dev/null 2>&1
    dev input tap $GRAB
    sleep 4
    shot S3_02_focus_lost.png
    dumps media_session S3_focus_lost.txt
    dumps audio S3_audio_focus_lost.txt
    say "8 秒后抢占方归还焦点（等价来电结束）"
    sleep 8
    shot S3_03_after_return.png
    dumps media_session S3_after_return.txt
    say "用户显式继续"
    tap_play
    sleep 5
    shot S3_04_resumed.png
    dumps media_session S3_resumed.txt
    stop_rec S3-焦点抢占
    pull_rec S3-焦点抢占
    snap_log S3
    ;;

  s4)
    say "S4 运行通知 / 锁屏播控 暂停与恢复"
    reset_app
    start_rec S4-通知播控 60
    tap_play
    sleep 6
    dumps media_session S4_playing.txt
    say "下拉通知栏（媒体通知可见）"
    dev cmd statusbar expand-notifications
    sleep 3
    shot S4_01_notification_shade.png
    say "系统媒体键暂停（等同点通知播控暂停）"
    dev cmd media_session dispatch pause
    sleep 4
    shot S4_02_shade_paused.png
    dumps media_session S4_paused.txt
    say "系统媒体键恢复"
    dev cmd media_session dispatch play
    sleep 4
    shot S4_03_shade_resumed.png
    dumps media_session S4_resumed.txt
    dev cmd statusbar collapse
    sleep 2
    stop_rec S4-通知播控
    pull_rec S4-通知播控
    say "锁屏播控（锁屏界面无法 screenrecord，改截图留证）"
    dev input keyevent 26
    hold_screen_off 18
    wake_to_keyguard
    shot S4_04_lockscreen_playing.png
    dumps media_session S4_lockscreen_playing.txt
    say "锁屏界面播控：暂停"
    dev cmd media_session dispatch pause
    sleep 3
    shot S4_05_lockscreen_paused.png
    dumps media_session S4_lockscreen_paused.txt
    say "锁屏界面播控：恢复"
    dev cmd media_session dispatch play
    sleep 3
    shot S4_06_lockscreen_resumed.png
    dumps media_session S4_lockscreen_resumed.txt
    dev wm dismiss-keyguard >/dev/null 2>&1
    sleep 2
    snap_log S4
    ;;

  s5)
    say "S5 无声模式：锁屏暂停 + 解锁显式恢复"
    reset_app
    start_rec S5-无声锁屏 70
    say "打开无声模式"
    ensure_awake >/dev/null 2>&1
    dev input tap $SILENT
    sleep 2
    shot S5_01_silent_on.png
    tap_play
    sleep 6
    shot S5_02_silent_playing.png
    dumps media_session S5_silent_playing.txt
    say "熄屏锁屏（无声模式应自动暂停）"
    dev input keyevent 26
    hold_screen_off 12
    power_state S5_power_off.txt
    dumps media_session S5_silent_locked.txt
    stop_rec S5-无声锁屏
    pull_rec S5-无声锁屏
    say "唤醒到锁屏界面（应仍暂停）"
    wake_to_keyguard
    shot S5_03_lockscreen_paused.png
    kg_state S5_keyguard.txt
    dumps media_session S5_lockscreen.txt
    say "解锁（应用内应提示需显式继续）"
    dev wm dismiss-keyguard >/dev/null 2>&1
    sleep 3
    shot S5_04_unlocked_needs_resume.png
    dumps media_session S5_unlocked.txt
    say "用户显式继续"
    tap_play
    sleep 5
    shot S5_05_resumed.png
    dumps media_session S5_resumed.txt
    snap_log S5
    ;;

  teardown)
    say "teardown：还原设备锁屏设置 + 清理设备临时文件"
    dev settings put secure lockscreen.disabled 1
    dev svc power stayon false
    echo "lockscreen.disabled=$(dev settings get secure lockscreen.disabled)"
    dev 'for p in $(pidof screenrecord); do kill -INT $p; done' >/dev/null 2>&1
    dev rm -f /sdcard/S1-锁屏持续播放.mp4 /sdcard/S3-焦点抢占.mp4 /sdcard/S4-通知播控.mp4 /sdcard/S5-无声锁屏.mp4 /sdcard/*.png /sdcard/ui*.xml /sdcard/srtest.mp4 /sdcard/kg.mp4 /sdcard/kg2.mp4 >/dev/null 2>&1
    ls -la "$EV"
    ;;

  *)
    echo "usage: $0 <setup|s1|s2|s3|s4|s5|teardown>"
    exit 1
    ;;
esac
