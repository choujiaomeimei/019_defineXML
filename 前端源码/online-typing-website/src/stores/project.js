import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useProjectStore = defineStore('project', () => {
  const currentProjectId = ref('')
  const currentProjectName = ref('')
  const projectConfig = ref(null)

  function setCurrentProject(projectId, projectName) {
    currentProjectId.value = projectId
    currentProjectName.value = projectName || ''
  }

  function setProjectConfig(config) {
    projectConfig.value = config
  }

  function clearProject() {
    currentProjectId.value = ''
    currentProjectName.value = ''
    projectConfig.value = null
  }

  return { currentProjectId, currentProjectName, projectConfig, setCurrentProject, setProjectConfig, clearProject }
})
