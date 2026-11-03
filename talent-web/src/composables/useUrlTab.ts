import { ref, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export function useUrlTab<T extends string>(tabs: readonly T[], fallback: T): Ref<T> {
  const route = useRoute()
  const router = useRouter()

  function fromQuery(value: unknown): T | null {
    const current = String(value ?? '')
    return tabs.find((tab) => tab === current) ?? null
  }

  const activeTab = ref<T>(fromQuery(route.query.tab) ?? fallback) as Ref<T>

  watch(
    () => route.query.tab,
    (value) => {
      const tab = fromQuery(value) ?? fallback
      if (tab !== activeTab.value) activeTab.value = tab
    },
  )

  watch(activeTab, (tab) => {
    if (route.query.tab === tab) return
    router.replace({ query: { ...route.query, tab } })
  })

  return activeTab
}
