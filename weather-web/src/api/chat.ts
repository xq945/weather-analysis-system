import { ElMessage } from 'element-plus'

export interface ChatRequest {
  question: string
  city?: string
  dateRange?: { start: string; end: string }
  stream?: boolean
}

export interface SourceInfo {
  city: string
  date: string
  section: string
}

// SSE 流式问答
export async function askStream(
  params: ChatRequest,
  onDelta: (text: string) => void,
  onDone: (sources: SourceInfo[]) => void,
  onError: (err: Error) => void
): Promise<void> {
  const token = localStorage.getItem('token')
  try {
    const response = await fetch('/api/chat/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ ...params, stream: true })
    })

    if (!response.ok) {
      throw new Error('请求失败: ' + response.status)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.substring(6).trim()
        } else if (trimmed.startsWith('data:')) {
          const data = trimmed.substring(5).trim()
          if (currentEvent === 'delta') {
            onDelta(data)
          } else if (currentEvent === 'done') {
            try {
              const sources = data ? JSON.parse(data) : []
              onDone(sources)
            } catch {
              onDone([])
            }
          }
          currentEvent = ''
        }
      }
    }
  } catch (err: any) {
    if (err.name === 'AbortError') return
    ElMessage.error('连接失败: ' + (err.message || '未知错误'))
    onError(err)
  }
}

// 非流式问答
export async function askSync(params: ChatRequest): Promise<{ content: string; sources: SourceInfo[] }> {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/chat/ask', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ ...params, stream: false })
  })

  // Parse SSE-style response
  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let content = ''
  let sources: SourceInfo[] = []

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    let eventType = ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventType = line.substring(6).trim()
      } else if (line.startsWith('data:')) {
        const data = line.substring(5).trim()
        if (eventType === 'delta') {
          content += data
        } else if (eventType === 'done') {
          try { sources = JSON.parse(data) } catch { /* */ }
        }
        eventType = ''
      }
    }
  }

  return { content, sources }
}
