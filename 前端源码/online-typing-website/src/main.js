import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style/index.css'
import Bus from './bus'
import router from './router'
import VueCookies from 'vue-cookies'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Bus)
app.use(ElementPlus)
app.use(VueCookies)
app.mount('#app')
