import {
  createRouter,
  createWebHashHistory,
} from 'vue-router'
import ProjectList from '../views/ProjectList/index.vue'

const routerHistory = createWebHashHistory()

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login/index.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    name: 'Home',
    meta: {
      title: '项目列表',
    },
    component: ProjectList, // 首页直接显示项目列表
  },
  {
    path: '/ProjectList',
    redirect: '/',
  },
  {
    path: '/project/:projectId',
    name: 'ProjectWorkspace',
    meta: {
      title: '项目工作台',
    },
    component: () => import('../views/ProjectWorkspace/index.vue'),
    children: [
      {
        path: 'info',
        name: 'ProjectInfo',
        meta: { title: '项目信息' },
        component: () => import('../views/ProjectWorkspace/ProjectInfo.vue'),
      },
      {
        path: 'file-upload',
        name: 'FileUpload',
        meta: { title: '文件上传' },
        component: () => import('../views/ProjectWorkspace/ProjectConfig.vue'),
      },
      {
        path: 'study',
        name: 'StudyView',
        meta: { title: 'Study' },
        component: () => import('../views/ProjectWorkspace/StudyView.vue'),
      },
      {
        path: 'datasets',
        name: 'DatasetsView',
        meta: { title: 'Datasets' },
        component: () => import('../views/ProjectWorkspace/DatasetsView.vue'),
      },
      {
        path: 'variables',
        name: 'VariablesView',
        meta: { title: 'Variables' },
        component: () => import('../views/ProjectWorkspace/SpecEdit.vue'),
      },
      {
        path: 'valuelevel',
        name: 'ValueLevelView',
        meta: { title: 'ValueLevel' },
        component: () => import('../views/ProjectWorkspace/VlmEdit.vue'),
      },
      {
        path: 'codelists',
        name: 'CodelistsView',
        meta: { title: 'Codelists' },
        component: () => import('../views/ProjectWorkspace/CodelistEdit.vue'),
      },
      {
        path: 'codelist-merge',
        name: 'CodelistMerge',
        meta: { title: 'Codelist合并' },
        component: () => import('../views/ProjectWorkspace/CodelistMerge.vue'),
      },
      {
        path: 'dictionaries',
        name: 'DictionariesView',
        meta: { title: 'Dictionaries' },
        component: () => import('../views/ProjectWorkspace/DictionariesEdit.vue'),
      },
      {
        path: 'methods',
        name: 'MethodsView',
        meta: { title: 'Methods' },
        component: () => import('../views/ProjectWorkspace/MethodsEdit.vue'),
      },
      {
        path: 'comments',
        name: 'CommentsView',
        meta: { title: 'Comments' },
        component: () => import('../views/ProjectWorkspace/CommentsEdit.vue'),
      },
      {
        path: 'documents',
        name: 'DocumentsView',
        meta: { title: 'Documents' },
        component: () => import('../views/ProjectWorkspace/DocumentsEdit.vue'),
      },
      {
        path: 'pages',
        name: 'PagesView',
        meta: { title: 'Pages' },
        component: () => import('../views/ProjectWorkspace/PagesEdit.vue'),
      },
      {
        path: 'define',
        name: 'DefineXlsx',
        meta: { title: 'Define制作' },
        component: () => import('../views/ProjectWorkspace/DefineXlsx.vue'),
      },
      {
        path: 'define-editor',
        name: 'DefineEditor',
        meta: { title: 'Define 在线编辑' },
        component: () => import('../views/ProjectWorkspace/DefineEditor.vue'),
      },
      {
        path: 'sdrg',
        name: 'SdrgEditor',
        meta: { title: 'SDRG撰写' },
        component: () => import('../views/ProjectWorkspace/SdrgEditor.vue'),
      },
      // Legacy redirects
      { path: 'config', redirect: 'file-upload' },
      { path: 'spec-edit', redirect: 'variables' },
      { path: 'vlm-edit', redirect: 'valuelevel' },
      { path: 'codelist-edit', redirect: 'codelists' },
      { path: 'pages-edit', redirect: 'pages' },
      {
        path: '',
        redirect: 'info'
      }
    ]
  }
]

const router = createRouter({
  history: routerHistory,
  // mode: 'hash',
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.path === '/login' || to.path === '/register') {
    next()
  } else if (!user) {
    next('/login')
  } else {
    next()
  }
})

router.onError((error, to) => {
  if (error.message?.includes('Failed to fetch dynamically imported module') ||
      error.message?.includes('Importing a module script failed')) {
    console.warn('Dynamic import failed, reloading page for route:', to.fullPath)
    window.location.assign(window.location.origin + '/#' + to.fullPath)
  }
})

export default router
