import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import ElementPlus from 'element-plus'
import 'vant/lib/index.css'
import 'element-plus/dist/index.css'
import './styles/global.scss'
import './pwa'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Vant)
app.use(ElementPlus)
app.mount('#app')
