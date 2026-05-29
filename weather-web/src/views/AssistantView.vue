<template>
  <div class="assistant-page">
    <div class="chat-container">
      <div class="chat-header">
        <h2>天气助手</h2>
        <div class="header-controls">
          <el-button @click="clearChat" :icon="'Delete'">清空对话</el-button>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <el-empty v-if="messages.length === 0" description="向我提问天气相关问题吧！" />

        <div v-for="(msg, idx) in messages" :key="idx" class="message-item" :class="msg.role">
          <div class="message-avatar">
            {{ msg.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="message-bubble">
            <div class="message-content" v-text="msg.content"></div>
            <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
              <span class="sources-label">参考报告：</span>
              <el-tag
                v-for="(s, si) in msg.sources"
                :key="si"
                size="small"
                type="info"
                style="margin-right: 4px; margin-bottom: 2px;"
              >
                {{ s.city }} {{ s.date }} {{ s.section }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 流式输出中的消息 -->
        <div v-if="streaming" class="message-item assistant">
          <div class="message-avatar">AI</div>
          <div class="message-bubble">
            <div class="message-content">{{ streamingContent || '思考中...' }}</div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="question"
          placeholder="请输入天气相关问题，例如：北京这周温度变化大吗？"
          @keyup.enter.exact="sendMessage"
          :disabled="streaming"
          clearable
        >
          <template #append>
            <el-button @click="sendMessage" :disabled="streaming || !question.trim()" type="primary">
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// AI 天气助手页面：RAG 流式问答，实时展示 AI 回复和引用来源
import { ref, nextTick } from 'vue'
import { askStream, type SourceInfo } from '../api/chat'

interface Message {
  role: 'user' | 'assistant'
  content: string
  sources?: SourceInfo[]
}

const question = ref('')
const messages = ref<Message[]>([])
const streaming = ref(false)
const streamingContent = ref('')
const messagesContainer = ref<HTMLElement>()

/** 发送问题到 AI 助手，SSE 流式展示回答 */
async function sendMessage() {
  const q = question.value.trim()
  if (!q || streaming.value) return

  // 将用户问题加入对话列表，准备接收流式响应
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  streaming.value = true
  streamingContent.value = ''

  await nextTick()
  scrollToBottom()

  const params: any = { question: q }

  await askStream(
    params,
    // delta：逐字追加到当前流式内容
    (delta) => {
      streamingContent.value += delta
      nextTick(() => scrollToBottom())
    },
    // done：流结束，将完整回答加入对话列表
    (sources) => {
      messages.value.push({
        role: 'assistant',
        content: streamingContent.value,
        sources
      })
      streaming.value = false
      streamingContent.value = ''
      nextTick(() => scrollToBottom())
    },
    // error：中断处理，有部分内容则追加中断标记
    () => {
      if (streamingContent.value) {
        messages.value.push({
          role: 'assistant',
          content: streamingContent.value + '\n\n[回答中断，请重试]',
        })
      } else {
        messages.value.push({
          role: 'assistant',
          content: '抱歉，智能分析服务暂时不可用，请稍后重试。',
        })
      }
      streaming.value = false
      streamingContent.value = ''
    }
  )
}

function clearChat() {
  messages.value = []
  streaming.value = false
  streamingContent.value = ''
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.assistant-page {
  height: calc(100vh - 120px);
  display: flex;
  justify-content: center;
}

.chat-container {
  width: 100%;
  max-width: 900px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
  min-height: 300px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
  flex-shrink: 0;
  color: #fff;
}

.message-item.user .message-avatar {
  background: #409EFF;
}

.message-item.assistant .message-avatar {
  background: #67c23a;
}

.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-item.user .message-bubble {
  background: #409EFF;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

.message-sources {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
  font-size: 12px;
}

.sources-label {
  color: #909399;
  margin-right: 4px;
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}
</style>
