import { del, get, post, put } from './request'
import type { SkillForm, SkillItem, SkillNode } from './types'

/** 整棵技能树 */
export function fetchSkillTree() {
  return get<SkillNode[]>('/skill/tree')
}

/** 只要分类 */
export function fetchSkillCategories() {
  return get<SkillItem[]>('/skill/categories')
}

/** 某个分类下的技能；不传 parentId 就是查顶级分类 */
export function fetchSkillChildren(parentId?: number) {
  return get<SkillItem[]>('/skill/children', parentId ? { parentId } : undefined)
}

/** 搜技能 */
export function searchSkills(keyword: string) {
  return get<SkillItem[]>('/skill/search', { keyword })
}

/** 新增分类（parentId 传 0）或子技能 */
export function createSkill(form: SkillForm) {
  return post<number>('/skill', form)
}

/** 修改技能名称/说明/排序/状态，也可以换分类 */
export function updateSkill(id: number, form: SkillForm) {
  return put<void>(`/skill/${id}`, form)
}

/** 删除技能或分类 */
export function deleteSkill(id: number) {
  return del<void>(`/skill/${id}`)
}
