#!/usr/bin/env bash
# PRE-02 技术样机：离线语音序列生成脚本（macOS，离线，无需联网）
#
# 目的：用 macOS 内置离线 TTS（say）合成中文语音提示，再用 ffmpeg 补齐到指定时长，
#       并混入一条低音量连续正弦音（便于在锁屏录屏里听出「音频确实在持续播放」，
#       也避免提示音之间出现完全静音造成证据歧义）。全程不联网。
#
# 产物：app/src/main/res/raw/cueNN_*.wav （16bit PCM / 44.1kHz / 单声道）
# 依赖：macOS `say`、`ffmpeg`、`ffprobe`
#
# 用法：bash tools/make_cues.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../app/src/main/res/raw"
TMP="$(mktemp -d)"
VOICE="Tingting"   # zh_CN 离线语音
SR=44100

mkdir -p "$OUT"

# 名称|文本|目标时长ms|正弦频率Hz
CUES=(
  "cue01_ready|准备|8000|660"
  "cue02_tighten|收紧|5000|880"
  "cue03_hold|保持|10000|494"
  "cue04_relax|完全放松|12000|392"
  "cue05_rest|休息|12000|330"
  "cue06_next|下一次|4000|740"
  "cue07_quick_tighten|快速收紧|4000|988"
  "cue08_quick_relax|快速放松|8000|587"
)

echo "== 生成离线语音序列（voice=${VOICE}, sr=${SR}）=="
TOTAL_MS=0
for item in "${CUES[@]}"; do
  IFS='|' read -r name text target_ms freq <<<"$item"
  tsec=$(awk "BEGIN{printf \"%.3f\", $target_ms/1000}")
  say -v "$VOICE" -o "$TMP/$name.aiff" "$text"
  ffmpeg -hide_banner -loglevel error -y -i "$TMP/$name.aiff" -ar "$SR" -ac 1 "$TMP/$name.sp.wav"
  # 语音后面补静音到目标时长
  ffmpeg -hide_banner -loglevel error -y -i "$TMP/$name.sp.wav" \
    -af "apad=whole_dur=$tsec" -t "$tsec" -ar "$SR" -ac 1 "$TMP/$name.pad.wav"
  # 目标时长连续低音量正弦音（保证整段可听）
  ffmpeg -hide_banner -loglevel error -y -f lavfi \
    -i "sine=frequency=$freq:sample_rate=$SR:duration=$tsec" \
    -af "volume=0.10" -ac 1 "$TMP/$name.tone.wav"
  # 混合（不归一化，保留语音清晰度）
  ffmpeg -hide_banner -loglevel error -y -i "$TMP/$name.pad.wav" -i "$TMP/$name.tone.wav" \
    -filter_complex "[0:a][1:a]amix=inputs=2:duration=first:normalize=0,volume=1.5" \
    -ar "$SR" -ac 1 -c:a pcm_s16le "$OUT/$name.wav"
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$name.wav")
  printf "  %-22s %6.2fs  (%s)\n" "$name.wav" "$dur" "$text"
  TOTAL_MS=$((TOTAL_MS + target_ms))
done

echo "== 完成：单次序列总时长约 $((TOTAL_MS / 1000)).$(( (TOTAL_MS % 1000) / 100 ))s =="
rm -rf "$TMP"
ls -la "$OUT"
