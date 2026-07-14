import { ElMessage } from 'element-plus'

const batches = new Map()

const TOAST_OPTIONS = {
  grouping: true,
  duration: 3000,
  offset: 72,
}

/**
 * Queue success/fail counts and flush a single aggregated toast per group.
 */
export function queueBatchMessage(group, { success = false, fail = false, delay = 400 } = {}) {
  if (!batches.has(group)) {
    batches.set(group, { success: 0, fail: 0, timer: null })
  }
  const batch = batches.get(group)
  if (success) batch.success += 1
  if (fail) batch.fail += 1

  if (batch.timer) clearTimeout(batch.timer)
  batch.timer = setTimeout(() => flushBatch(group), delay)
}

function flushBatch(group) {
  const batch = batches.get(group)
  if (!batch) return

  const { success, fail } = batch
  batches.delete(group)

  const total = success + fail
  if (total === 0) return

  if (group === 'upload') {
    if (fail === 0) {
      ElMessage({
        ...TOAST_OPTIONS,
        type: 'success',
        message: success > 1 ? `${success} 个文件上传成功，正在自动处理...` : '文件上传成功，正在自动处理...',
      })
    }
    return
  }

  if (group === 'process') {
    if (fail === 0) {
      ElMessage({
        ...TOAST_OPTIONS,
        type: 'success',
        message: success > 1 ? `${success} 个文件处理完成` : '文件处理完成',
      })
    } else if (success === 0) {
      ElMessage({
        ...TOAST_OPTIONS,
        type: 'warning',
        message: fail > 1 ? `${fail} 个文件处理失败，可手动重新处理` : '自动处理失败，可手动重新处理',
      })
    } else {
      ElMessage({
        ...TOAST_OPTIONS,
        type: 'warning',
        message: `${success} 个处理完成，${fail} 个失败`,
      })
    }
  }
}

export function showToast(type, message) {
  ElMessage({ ...TOAST_OPTIONS, type, message })
}
