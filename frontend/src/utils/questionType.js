/**
 * 面试题类型的唯一前端定义。
 * 后端同样只允许四种中文新值。已确认的历史别名仅用于兼容显示，
 * 选择题、空白和未知值必须显示“未分类”，不可伪装为八股题。
 */
export const QUESTION_TYPE_OPTIONS = [
  { label: '八股题', value: '八股题' },
  { label: '场景题', value: '场景题' },
  { label: '项目题', value: '项目题' },
  { label: '算法题', value: '算法题' },
]

export const DEFAULT_QUESTION_TYPE = '八股题'

const LEGACY_TYPE_ALIASES = new Map([
  ['八股题', '八股题'], ['SHORT_ANSWER', '八股题'], ['简答题', '八股题'], ['八股', '八股题'],
  ['场景题', '场景题'], ['SCENARIO', '场景题'], ['场景', '场景题'],
  ['项目题', '项目题'], ['PROJECT', '项目题'], ['项目', '项目题'],
  ['算法题', '算法题'], ['ALGORITHM', '算法题'], ['算法', '算法题'],
])

/** 已确认别名返回规范中文类型；未知类型返回 null，要求管理员人工分类。 */
export function normalizeQuestionType(value) {
  const normalized = typeof value === 'string' ? value.trim() : ''
  return LEGACY_TYPE_ALIASES.get(normalized) || null
}

/** 展示用标签：未知、选择题或空白值不得被误显示为八股题。 */
export function questionTypeLabel(value) {
  return normalizeQuestionType(value) || '未分类'
}
