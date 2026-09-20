#!/usr/bin/env bash
#
# 导出「给外部 AI 的项目上下文包」（M15 后新增）
#
# 用途：把项目简报 + README + 指定代码文件 + 当前 git 状态拼成一份可直接粘贴给
#       网页版 GPT 等外部 AI 的 markdown，解决"外部 AI 看不到仓库"的问题。
#
# 用法：
#   ./scripts/export-ai-context.sh --list                    # 列出推荐材料
#   ./scripts/export-ai-context.sh <文件...>                 # 输出到终端
#   ./scripts/export-ai-context.sh --clip <文件...>          # 输出并复制到剪贴板（macOS）
#   ./scripts/export-ai-context.sh --out ctx.md <文件...>    # 写入文件
#   ./scripts/export-ai-context.sh --no-readme <文件...>     # 不带 README（默认带）
#
# 安全：显式拒绝 .env 等敏感文件；脚本本身不读取任何密钥。
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BRIEF="$ROOT/docs/graduation/ai-agent-upgrade/external-ai-briefing.md"
README="$ROOT/README.md"
PROGRESS="$ROOT/docs/graduation/ai-agent-upgrade/progress.md"

# 单文件最大行数，避免一次塞给外部 AI 的上下文过长
MAX_LINES_PER_FILE=1200

CLIP=0
OUT=""
INCLUDE_README=1
FILES=()

usage() {
  sed -n '3,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

list_materials() {
  cat <<'EOF'
推荐材料（按当次问题挑 1-2 个即可，不要一次全给）：

  项目底座:
    README.md

  文档（都很长，建议只取相关小节，或直接用 --no-readme 自己拼）:
    docs/graduation/ai-agent-upgrade/external-ai-briefing.md   # 本脚本会自动带上
    docs/graduation/ai-agent-upgrade/findings.md               # 技术证据，按 6.x 分节
    docs/graduation/ai-agent-upgrade/progress.md               # 执行记录，按 M15-x 分节
    docs/graduation/ai-agent-upgrade/task_plan.md              # 总体计划
    docs/graduation/ai-agent-upgrade/m15-handoff.md            # 交接说明

  M15（RAG 知识库与向量检索）:
    src/main/java/com/bitforum/ai/rag/RagService.java
    src/main/java/com/bitforum/ai/rag/KbIndexService.java
    src/main/java/com/bitforum/ai/rag/ArticleChunkingService.java
    src/main/java/com/bitforum/ai/rag/KbIndexMessageListener.java
    src/main/java/com/bitforum/ai/config/VectorStoreConfig.java
    src/main/resources/db/migration/V14__add_ai_kb_document.sql

  M14（工具调用）:
    src/main/java/com/bitforum/ai/agent/QaAgent.java
    src/main/java/com/bitforum/ai/tool/ArticleTools.java
    src/main/java/com/bitforum/ai/tool/ToolRegistry.java

  配置与流程:
    src/main/resources/application.yml      # 只有环境变量占位符，无真实密钥
    .github/workflows/ci.yml                # CI 编排

用法示例:
  ./scripts/export-ai-context.sh --list
  ./scripts/export-ai-context.sh src/main/java/com/bitforum/ai/rag/RagService.java
  ./scripts/export-ai-context.sh --clip src/main/java/com/bitforum/ai/rag/RagService.java
  ./scripts/export-ai-context.sh --out /tmp/ctx.md --no-readme src/main/resources/application.yml
EOF
}

# ---------- 参数解析 ----------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --clip) CLIP=1; shift ;;
    --no-readme) INCLUDE_README=0; shift ;;
    --out)
      [[ $# -ge 2 ]] || { echo "错误：--out 需要一个文件路径" >&2; exit 2; }
      OUT="$2"; shift 2 ;;
    --list) list_materials; exit 0 ;;
    -h|--help) usage; exit 0 ;;
    --) shift; while [[ $# -gt 0 ]]; do FILES+=("$1"); shift; done ;;
    -*) echo "错误：未知参数 $1" >&2; usage >&2; exit 2 ;;
    *) FILES+=("$1"); shift ;;
  esac
done

# ---------- 敏感文件保护 ----------
for f in ${FILES[@]+"${FILES[@]}"}; do
  base="$(basename "$f")"
  case "$base" in
    .env|.env.*|*.pem|*.key|*secret*|*credential*|*.p12|*.jks)
      echo "拒绝导出敏感文件：$f" >&2
      echo "（提示：请勿把密钥类文件交给外部 AI）" >&2
      exit 3 ;;
  esac
done

lang_for() {
  case "${1##*.}" in
    java) echo java ;;
    js) echo javascript ;;
    jsx) echo jsx ;;
    ts|tsx) echo typescript ;;
    sql) echo sql ;;
    yml|yaml) echo yaml ;;
    md) echo markdown ;;
    css) echo css ;;
    sh) echo bash ;;
    json) echo json ;;
    xml) echo xml ;;
    html) echo html ;;
    *) echo text ;;
  esac
}

# 把被包含文档的标题整体降两级（# → ###），使其成为上下文包 `##` 章节的子级，
# 避免出现两个"一、二"。跳过 ``` 代码块内部，否则其中的 # 注释会被误加前缀。
shift_headings() {
  awk '/^```/ { in_code = !in_code; print; next }
       { if (!in_code && $0 ~ /^#/) print "##" $0; else print }'
}

# 易变指标检查（**只提示、不改写**）：
# 内嵌文档里的 Git 快照（领先提交数 / push 状态 / 工作区）若没标注日期，会和页眉的实时值
# 并列出现在同一份上下文包里，读的人无法判断哪个是当前状态。这里只在 stderr 打印提示，
# 不修改任何文件、不影响退出码，也不阻断导出 —— 历史记录本身是允许保留的。
VOLATILE_PATTERN='领先 [0-9]+ 个提交|尚未 push|未 push|工作区干净'
warn_volatile_snapshots() {
  local target="$1" label="$2" hits
  [[ -f "$target" ]] || return 0
  hits="$(grep -nE "$VOLATILE_PATTERN" "$target" 2>/dev/null | head -3 || true)"
  [[ -z "$hits" ]] && return 0
  # 已经标了日期/快照/历史字样的文档视为已消歧，不再提示
  if grep -qE '截至 [0-9]{4}-[0-9]{2}-[0-9]{2}|快照|历史记录|写于 [0-9]{4}-[0-9]{2}-[0-9]{2}' "$target" 2>/dev/null; then
    return 0
  fi
  {
    echo "[warn] $label 含易变 Git 指标但未见日期/快照标注，可能与页眉实时值并列造成误读："
    echo "$hits" | sed 's/^/        /'
  } >&2
}

git_info() {  BRANCH="$(git -C "$ROOT" branch --show-current 2>/dev/null || echo '?')"
  LAST="$(git -C "$ROOT" log -1 --oneline 2>/dev/null || echo '?')"
  DIRTY="$(git -C "$ROOT" status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
  if git -C "$ROOT" rev-parse --verify --quiet main >/dev/null 2>&1; then
    AHEAD="$(git -C "$ROOT" rev-list --count main..HEAD 2>/dev/null || echo '?')"
  else
    AHEAD="?"
  fi
  TEST_LINE="$(grep -m1 '^| 测试状态' "$PROGRESS" 2>/dev/null | sed -E 's/^\| 测试状态 \|[[:space:]]*//; s/[[:space:]]*\|$//' || true)"
  [[ -n "$TEST_LINE" ]] || TEST_LINE="（见 progress.md 顶部状态快照）"
}

emit_header() {
  git_info
  echo "# BitForum 项目上下文包"
  echo
  echo "- 生成时间：$(date '+%Y-%m-%d %H:%M')"
  echo "- 分支：\`$BRANCH\`（相对 main 领先 $AHEAD 个提交）"
  echo "- 最新提交：\`$LAST\`"
  if [[ "$DIRTY" = "0" ]]; then
    echo "- 工作区：干净"
  else
    echo "- 工作区：$DIRTY 个未提交改动"
  fi
  echo "- 测试基线：$TEST_LINE"
  echo
  echo "> **本页眉的 5 项是实时值**：分支/提交/工作区/测试基线与下方正文里内嵌文档中的同类指标"
  echo "> 可能来自不同时间点 —— 那些是「截至其标注日期」的历史快照，冲突时**一律以本页眉为准**。"
  echo
  echo "> **给 AI 的话**：以下是只读上下文。请**先复述你对项目的理解再回答**；"
  echo "> 未提供的文件不要假设其内容；需要更多材料时请明确说明需要哪个文件。"
  echo
  echo "---"
  echo
  echo "## 一、项目简报"
  echo
  warn_volatile_snapshots "$BRIEF" "项目简报"
  shift_headings < "$BRIEF"
  echo
  echo "---"
  echo
}

emit_readme() {
  echo "## 二、项目 README（已与 M13-M18 收口状态同步）"
  echo
  if [[ -f "$README" ]]; then
    warn_volatile_snapshots "$README" "README"
    shift_headings < "$README"
  else
    echo "（README.md 不存在）"
  fi
  echo
  echo "---"
  echo
}

emit_files() {
  echo "## 三、相关文件"
  echo
  if [[ ${#FILES[@]} -eq 0 ]]; then
    echo "（未指定文件。用 \`--list\` 查看推荐材料）"
    echo
    return
  fi

  local idx=1
  local f abs rel lines
  for f in "${FILES[@]}"; do
    if [[ "$f" = /* ]]; then abs="$f"; else abs="$ROOT/$f"; fi
    if [[ ! -f "$abs" ]]; then
      echo "### 3.$idx \`$f\`" >&2
      echo "**（文件不存在，已跳过）**" >&2
      echo
      continue
    fi
    rel="${abs#"$ROOT"/}"
    lines="$(wc -l < "$abs" | tr -d ' ')"
    echo "### 3.$idx \`$rel\`"
    warn_volatile_snapshots "$abs" "相关文件 $rel"
    echo
    echo '```'"$(lang_for "$abs")"
    if [[ "$lines" -gt "$MAX_LINES_PER_FILE" ]]; then
      head -n "$MAX_LINES_PER_FILE" "$abs"
      echo
      echo "...（本文件共 $lines 行，此处截断到前 $MAX_LINES_PER_FILE 行）"
    else
      cat "$abs"
    fi
    echo '```'
    echo
    idx=$((idx + 1))
  done
  echo "---"
  echo
}

emit_footer() {
  cat <<'EOF'
## 四、我的问题

（在这里写你的问题）

---

请先复述你对项目的理解，再回答；不确定的地方请直接说不确定，不要编造。
EOF
}

emit_all() {
  emit_header
  [[ "$INCLUDE_README" = "1" ]] && emit_readme
  emit_files
  emit_footer
}

# ---------- 输出 ----------
if [[ -n "$OUT" ]]; then
  emit_all > "$OUT"
  echo "已写入：${OUT}（$(wc -l < "$OUT" | tr -d ' ') 行）" >&2
  if [[ "$CLIP" = "1" ]]; then
    if command -v pbcopy >/dev/null 2>&1; then
      pbcopy < "$OUT" && echo "已复制到剪贴板" >&2
    else
      echo "提示：未找到 pbcopy，未复制到剪贴板" >&2
    fi
  fi
elif [[ "$CLIP" = "1" ]]; then
  TMP="$(mktemp)"
  emit_all > "$TMP"
  if command -v pbcopy >/dev/null 2>&1; then
    pbcopy < "$TMP" && echo "已复制到剪贴板（$(wc -l < "$TMP" | tr -d ' ') 行）" >&2
  else
    echo "提示：未找到 pbcopy，改为输出到终端" >&2
    cat "$TMP"
  fi
  rm -f "$TMP"
else
  emit_all
fi
