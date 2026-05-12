import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') ?? '')
  const username = ref<string>(localStorage.getItem('username') ?? '')
  const isLoggedIn = computed(() => !!token.value)

  function setAuth(t: string, name: string) {
    token.value = t
    username.value = name
    localStorage.setItem('token', t)
    localStorage.setItem('username', name)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  return { token, username, isLoggedIn, setAuth, logout }
})
