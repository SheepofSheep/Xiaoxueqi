import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    defaultDistrict: '尖草坪区',
    defaultBusinessArea: '中北大学周边',
  }),
})

