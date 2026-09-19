#!/usr/bin/env bash
#
# 导出「项目全景」给外部 AI（M15 后新增）
#
# 与 export-ai-context.sh 的区别：
#   - export-ai-context.sh：**按需**上下文（简报 + README + 你指定的文件），用于问某个具体问题
#   - project-overview.sh ：**全景**上下文（额外包含自动提取的代码地图、数据库结构、
#                           接口清单、AI 模块要点），用于让外部 AI 先"通读"整个项目
#
# 用法：
#   ./scripts/project-overview.sh                 # 概览版（约 1000 行）→ 终端
#   ./scripts/project-overview.sh --clip          # 概览版并复制到剪贴板（macOS）
#   ./scripts/project-overview.sh --with-code     # 追加核心类全文（约 2800 行）
#   ./scripts/project-overview.sh --out ctx.md    # 写入文件
#
# 安全：只读取源码与文档，不触碰 .env 等凭据文件。
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BRIEF="$ROOT/docs/graduation/ai-agent-upgrade/external-ai-briefing.md"
README="$ROOT/README.md"
PROGRESS="$ROOT/docs/graduation/ai-agent-upgrade/progress.md"

MAX_DOC_LINES=40   # 数据库/接口清单里单文件的展示上限，防止某张表字段过多刷屏

CLIP=0
WITH_CODE=0
OUT=""

usage() {
  sed -n '3,18p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

# 被包含文档的标题整体降两级（# → ###），使其成为全景包 `##` 章节的子级；
# 跳过 ``` 代码块内部，否则其中的 # 注释会被误加前缀。
shift_headings() {
  awk '/^```/ { in_code = !in_code; print; next }
       { if (!in_code && $0 ~ /^#/) print "##" $0; else print }'
}

lang_for() {
  case "${1##*.}" in
    java) echo java ;; js) echo javascript ;; jsx) echo jsx ;; ts|tsx) echo typescript ;;
    sql) echo sql ;; yml|yaml) echo yaml ;; md) echo markdown ;; css) echo css ;;
    sh) echo bash ;; json) echo json ;; xml) echo xml ;; html) echo html ;; *) echo text ;;
  esac
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --clip) CLIP=1; shift ;;
    --with-code) WITH_CODE=1; shift ;;
    --out)
      [[ $# -ge 2 ]] || { echo "错误：--out 需要一个文件路径" >&2; exit 2; }
      OUT="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "错误：未知参数 $1" >&2; usage >&2; exit 2 ;;
  esac
done

# ---------- 自动提取 ----------

# 取 Java 文件类级 Javadoc 的第一行有效文本作为职责说明
class_doc() {
  local doc
  doc="$(sed -n '/^\/\*\*/,/\*\//p' "$1" 2>/dev/null \
    | sed 's|^[[:space:]]*\*[[:space:]]*||; s|^/\*\*[[:space:]]*||' \
    | grep -v '^$' | head -1)"
  if [[ -n "$doc" ]]; then
    echo "$doc"
  else
    echo "（无类注释）"
  fi
}

section_code_map() {
  echo "## 三、后端代码地图"
  echo
  echo "按包分组；每条为「文件 — 类职责（取自类级 Javadoc 首句）」。"
  echo "完整实现未包含在内，需要时可按文件名索取。"
  echo

  local total
  total="$(find "$ROOT/src/main/java" -name '*.java' | wc -l | tr -d ' ')"
  echo "共 ${total} 个 Java 文件。"
  echo

  local pkg dir rel name doc count
  for dir in $(find "$ROOT/src/main/java/com/bitforum" -type d | sort); do
    count="$(find "$dir" -maxdepth 1 -name '*.java' | wc -l | tr -d ' ')"
    [[ "$count" -eq 0 ]] && continue
    pkg="${dir#"$ROOT/src/main/java/"}"
    echo "### \`${pkg}\`（${count} 个）"
    echo
    for f in "$dir"/*.java; do
      [[ -f "$f" ]] || continue
      name="$(basename "$f")"
      doc="$(class_doc "$f")"
      echo "- \`${name}\` — ${doc}"
    done
    echo
  done
}

section_frontend_map() {
  echo "## 四、前端代码地图"
  echo

  local dir rel count
  for dir in pages pages/admin components layouts api utils; do
    [[ -d "$ROOT/frontend/src/$dir" ]] || continue
    count="$(find "$ROOT/frontend/src/$dir" -maxdepth 1 \( -name '*.jsx' -o -name '*.js' \) ! -name '*.test.*' | wc -l | tr -d ' ')"
    [[ "$count" -eq 0 ]] && continue
    echo "### \`src/${dir}\`（${count} 个）"
    echo
    for f in "$ROOT/frontend/src/$dir"/*.jsx "$ROOT/frontend/src/$dir"/*.js; do
      [[ -f "$f" ]] || continue
      case "$(basename "$f")" in *.test.*) continue ;; esac
      echo "- \`$(basename "$f")\`"
    done
    echo
  done
}

section_database() {
  echo "## 五、数据库结构（Flyway 迁移）"
  echo
  echo "约定：只新增迁移，绝不修改历史迁移。"
  echo
  local f base
  for f in "$ROOT"/src/main/resources/db/migration/V*.sql; do
    [[ -f "$f" ]] || continue
    base="$(basename "$f")"
    echo "### \`${base}\`"
    echo
    awk '
      /^CREATE TABLE/ {
        if (table != "") print "- `" table "`: " cols
        t = $0
        sub(/.*EXISTS[[:space:]]+/, "", t)
        sub(/[[:space:](].*/, "", t)
        table = t; cols = ""; next
      }
      /^ALTER TABLE/ { t = $3; next }
      /ADD COLUMN/ && t != "" {
        line = $0
        sub(/.*ADD COLUMN[[:space:]]+/, "", line)
        sub(/[[:space:]].*/, "", line)
        print "- `" t "`（ALTER 新增字段）: " line
        next
      }
      table != "" && /^[[:space:]]+[a-z_]+[[:space:]]+(BIGINT|VARCHAR|INT|TEXT|DATETIME|TINYINT|DECIMAL|TIMESTAMP)/ {
        c = $1
        if (cols == "") cols = c; else cols = cols ", " c
      }
      END { if (table != "") print "- `" table "`: " cols }
    ' "$f" | head -n "$MAX_DOC_LINES"
    echo
  done
}

section_endpoints() {
  echo "## 六、HTTP 接口清单"
  echo
  echo "从 Controller 的映射注解与 \`@Operation\` 摘要自动提取。"
  echo
  local f base
  for f in "$ROOT"/src/main/java/com/bitforum/controller/*.java; do
    [[ -f "$f" ]] || continue
    base="$(basename "$f" .java)"
    echo "### \`${base}\`"
    echo
    awk '
      /@RequestMapping\(/ {
        if (match($0, /"[^"]*"/)) base = substr($0, RSTART+1, RLENGTH-2)
        next
      }
      /@(Get|Post|Put|Delete)Mapping/ {
        method = ""
        if ($0 ~ /@GetMapping/) method = "GET"
        else if ($0 ~ /@PostMapping/) method = "POST"
        else if ($0 ~ /@PutMapping/) method = "PUT"
        else if ($0 ~ /@DeleteMapping/) method = "DELETE"
        path = ""
        if (match($0, /"[^"]*"/)) path = substr($0, RSTART+1, RLENGTH-2)
        pending = method " " base path
        next
      }
      /@Operation\(summary = "/ {
        if (pending != "") {
          s = $0
          sub(/.*summary = "/, "", s); sub(/".*/, "", s)
          print "- `" pending "` — " s
          pending = ""
        }
      }
    ' "$f" | head -n "$MAX_DOC_LINES"
    echo
  done
}

section_ai_modules() {
  cat <<'EOF'
## 七、AI 模块实现要点（M13–M15）

### 7.1 分层

```text
controller/  AiConversationController（对话接口）、AdminAiKbController（知识库管理）
   ↓
orchestrator/AgentOrchestrator   加载历史 → 路由 Agent → 落库消息与统计
   ↓
agent/       QaAgent（问答，含工具调用与 RAG 注入） / M16 将新增 ModerationAgent
   ↓
rag/         RagService（检索） KbIndexService（索引） ArticleChunkingService（分块）
             KbIndexMessageListener（异步索引消费）
   ↓
memory/      MysqlChatMemoryRepository（会话记忆持久化）
tool/        ArticleTools（查询类 5 个） UserInteractionTools（写操作类 7 个） ToolRegistry（按 Agent 装配）
```

### 7.2 关键设计决策

1. **对话记忆存 MySQL**：Spring AI 默认内存仓储重启即丢，替换为 `MysqlChatMemoryRepository`。
2. **工具身份用 ToolContext 隐式注入**：当前登录用户不作为工具参数暴露给模型
   （否则模型会向用户索要 userId，实测踩过），改由应用侧通过 ToolContext 传入。
3. **RAG 不用 `QuestionAnswerAdvisor` 而自己拼上下文**：M14 的工具调用已占用 prompt 组装，
   框架 advisor 会再改写一遍 prompt，叠加后 token 不可控、引用来源也难精确记录。
4. **检索三重约束**：请求带 `status == 'PUBLISHED'` 过滤 → 召回后按 `article` 表**当前**状态二次校验
   → topK=5、每段截断 300 字。
5. **索引一致性**：向量在 Redis、元数据在 MySQL，无法同一事务，顺序固定为
   「先删旧 → 写向量 → 写记录 → 标记完成」，失败标记 FAILED 由下次重建修复。
6. **异步索引消息在事务提交后发送**：否则消费者会读到旧状态导致永久漏索引。
7. **降级优先**：AI 不可用时返回兜底文案，绝不影响论坛主流程。

### 7.3 数据流（RAG 问答）

```text
用户提问
  → 检索：问题向量化（bge-base-zh-v1.5, 768 维）→ Redis RediSearch KNN + status 过滤 → 二次校验
  → 注入：把 topK=5 片段作为本轮 SystemMessage（不写入会话记忆）
  → 生成：DeepSeek + 工具调用，提示词要求标注引用编号
  → 落库：回答 + 引用文章 id（ai_message.retrieved_doc_ids）
  → 展示：前端渲染回答并在下方列出可点击的原帖链接
```

### 7.4 结构化输出（M16 将用到，尚未实测）

M16 审核 Agent 需要模型返回结构化结果（各维度分数 + 决策 + 理由）并映射为 Java 对象。
这是 `findings.md` 待验证事项 T7，按 M15 的经验应先做最小验证再写业务代码。
EOF
}

section_core_code() {
  echo "## 八、核心代码选摘"
  echo
  echo "以下是最能反映项目实现风格与关键逻辑的文件全文。"
  echo
  local f rel lines idx=1
  local files=(
    "src/main/java/com/bitforum/ai/agent/QaAgent.java"
    "src/main/java/com/bitforum/ai/rag/RagService.java"
    "src/main/java/com/bitforum/ai/rag/KbIndexService.java"
    "src/main/java/com/bitforum/ai/rag/ArticleChunkingService.java"
    "src/main/java/com/bitforum/ai/config/VectorStoreConfig.java"
    "src/main/java/com/bitforum/ai/tool/ToolRegistry.java"
    "src/main/java/com/bitforum/service/ArticleService.java"
    "src/main/resources/application.yml"
    ".github/workflows/ci.yml"
  )
  for rel in "${files[@]}"; do
    f="$ROOT/$rel"
    if [[ ! -f "$f" ]]; then
      echo "### 8.${idx} \`${rel}\`" >&2
      echo "**（文件不存在，已跳过）**" >&2
      echo
      continue
    fi
    lines="$(wc -l < "$f" | tr -d ' ')"
    echo "### 8.${idx} \`${rel}\`（${lines} 行）"
    echo
    echo '```'"$(lang_for "$f")"
    cat "$f"
    echo '```'
    echo
    idx=$((idx + 1))
  done
}

git_info() {
  BRANCH="$(git -C "$ROOT" branch --show-current 2>/dev/null || echo '?')"
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

emit_all() {
  git_info
  cat <<EOF
# BitForum 项目全景（给外部 AI 通读用）

- 生成时间：$(date '+%Y-%m-%d %H:%M')
- 分支：\`$BRANCH\`（相对 main 领先 $AHEAD 个提交）
- 最新提交：\`$LAST\`
- 工作区：$([[ "$DIRTY" = "0" ]] && echo "干净" || echo "$DIRTY 个未提交改动")
- 测试基线：$TEST_LINE

> **给 AI 的话**：这是项目的完整概览，用于让你先通读全项目再回答我的问题。
> 请**先复述你对项目的理解**（定位、架构分层、当前进度、技术约束）再回答；
> 未提供的具体实现不要假设细节；需要某个文件的完整代码时，明确告诉我是哪个文件。

---

## 一、项目简报

EOF
  shift_headings < "$BRIEF"
  echo
  echo "---"
  echo
  echo "## 二、项目 README"
  echo
  if [[ -f "$README" ]]; then
    shift_headings < "$README"
  else
    echo "（README.md 不存在）"
  fi
  echo
  echo "---"
  echo

  section_code_map
  echo "---"
  echo
  section_frontend_map
  echo "---"
  echo
  section_database
  echo "---"
  echo
  section_endpoints
  echo "---"
  echo
  section_ai_modules
  echo
  echo "---"
  echo

  if [[ "$WITH_CODE" = "1" ]]; then
    section_core_code
    echo "---"
    echo
  fi

  cat <<'EOF'
## 我的问题

（在这里写你的问题；如果只是让 AI 通读，可以写"请复述你对这个项目的理解，并指出你认为最值得改进的三处"）
EOF
}

if [[ -n "$OUT" ]]; then
  emit_all > "$OUT"
  echo "已写入：${OUT}（$(wc -l < "$OUT" | tr -d ' ') 行 / $(du -h "$OUT" | cut -f1 | tr -d ' ')）" >&2
  if [[ "$CLIP" = "1" ]]; then
    command -v pbcopy >/dev/null 2>&1 && pbcopy < "$OUT" && echo "已复制到剪贴板" >&2
  fi
elif [[ "$CLIP" = "1" ]]; then
  TMP="$(mktemp)"
  emit_all > "$TMP"
  if command -v pbcopy >/dev/null 2>&1; then
    pbcopy < "$TMP" && echo "已复制到剪贴板（$(wc -l < "$TMP" | tr -d ' ') 行 / $(du -h "$TMP" | cut -f1 | tr -d ' ')）" >&2
  else
    echo "提示：未找到 pbcopy，改为输出到终端" >&2
    cat "$TMP"
  fi
  rm -f "$TMP"
else
  emit_all
fi
