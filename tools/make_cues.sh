#!/usr/bin/env bash
# P045 男性盆底肌语音训练器：离线语音提示资产生成（macOS，全程不联网）
#
# 产物：
#   app/src/main/res/raw/cue_*.wav      zh-CN（默认语言）
#   app/src/main/res/raw-en/cue_*.wav   en（英文，按语言限定符自动切换）
#   app/src/main/res/raw/silent_loop.wav 无声承载音（保持前台媒体服务不被打断）
#
# 依赖：macOS `say`、`ffmpeg`
# 用法：bash tools/make_cues.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
RAW_ZH="$HERE/app/src/main/res/raw"
RAW_EN="$HERE/app/src/main/res/raw-en"
TMP="$(mktemp -d)"
SR=44100
VOICE_ZH="Tingting"
VOICE_EN="Samantha"

mkdir -p "$RAW_ZH" "$RAW_EN"

# 名称|中文文本|英文文本
CUES=(
  "cue_ready|准备|Ready"
  "cue_tighten|收紧|Tighten"
  "cue_hold|保持|Hold"
  "cue_relax|放松|Relax"
  "cue_rest|休息|Rest"
  "cue_next|下一次|Next"
  "cue_fast_tighten|紧|Squeeze"
  "cue_fast_relax|松|Release"
  "cue_done|训练完成|Complete"
  "cue_fast_intro|接下来，快速收紧|Next, quick squeezes"
  "cue_quality|正常呼吸，不憋气，不夹臀|Breathe normally, do not hold your breath"
  "cue_num3|三|Three"
  "cue_num2|二|Two"
  "cue_num1|一|One"
)

trim_filter="silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse"

render() { # $1=voice $2=text $3=out
  local voice="$1" text="$2" out="$3"
  say -v "$voice" -o "$TMP/raw.aiff" "$text"
  ffmpeg -hide_banner -loglevel error -y -i "$TMP/raw.aiff" \
    -af "$trim_filter" -ar "$SR" -ac 1 -c:a pcm_s16le "$out"
}

echo "== 生成中文提示音（${VOICE_ZH}）=="
for item in "${CUES[@]}"; do
  IFS='|' read -r name zh en <<<"$item"
  render "$VOICE_ZH" "$zh" "$RAW_ZH/$name.wav"
done

echo "== 生成英文提示音（${VOICE_EN}）=="
for item in "${CUES[@]}"; do
  IFS='|' read -r name zh en <<<"$item"
  render "$VOICE_EN" "$en" "$RAW_EN/$name.wav"
done

echo "== 生成无声承载音 =="
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anullsrc=r=$SR:cl=mono" \
  -t 10 -c:a pcm_s16le "$RAW_ZH/silent_loop.wav"

echo "== 时长核对（秒）=="
for item in "${CUES[@]}"; do
  IFS='|' read -r name zh en <<<"$item"
  dz=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$RAW_ZH/$name.wav")
  de=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$RAW_EN/$name.wav")
  printf "  %-18s zh=%6.2fs  en=%6.2fs   (%s / %s)\n" "$name" "$dz" "$de" "$zh" "$en"
done

rm -rf "$TMP"
echo "== 完成：zh $(ls -1 "$RAW_ZH"/*.wav | wc -l | tr -d ' ') 个，en $(ls -1 "$RAW_EN"/*.wav | wc -l | tr -d ' ') 个 =="