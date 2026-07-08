// /**
//  * Created by bootdo.
//  */
import axios, { exportService } from '../axios'

//提交
export function ApiPostAddOrUpdate(params) {
  return axios({
    url: `/planInfo/addOrUpdate`,
    method: 'post',
    data: params,
  })
}

// 修改密码
export function postPassword(params) {
  return axios({
    url: '/user/password',
    method: 'post',
    data: params,
  })
}


//保存
export function ApiGetSearch() {
  return axios({
    url: `/planInfo/search`,
    method: 'get',
  })
}
//添加/修改EVENT
export function ApiPostConfigAddOrUpdate(params) {
  return axios({
    url: `/visitConfig/addOrUpdate`,
    method: 'post',
    data: params,
  })
}
//删除EVENT
export function ApiPostDeleteOrUpdate(params) {
  return axios({
    url: `/visitConfig/deleteEVENT`,
    method: 'delete',
    data: params,
  })
}
//搜索EVENT
export function ApiGetOrSearchEVENT(projectId) {
  console.log('搜索访视EVENT请求参数:', { projectId });  // 添加日志
  return axios({
    url: `/visitConfig/searchEVENT`,
    method: 'get',
    params: {
      projectId
    }
  })

}//导出CRF
export function ApiPostExportCrf(params) {
  console.log('导出CRF请求参数(格式化):', JSON.stringify(params, null, 2))
  return exportService({
    url: `/export/crf`,
    method: 'post',
    data: params
  })
}
//预览表单
export function ApiPostCrfPreviewObject(crfConfigId, projectId) {
  // 添加时间戳参数，避免缓存，确保每次都获取最新数据
  const timestamp = new Date().getTime();
  console.log('预览请求参数:', { crfConfigId, projectId, timestamp });
  return axios({
    url: `/crf/crfPreviewObject`,
    method: 'get',
    params: {
      crfConfigId,
      projectId,
      timestamp // 添加时间戳确保不使用缓存
    }
  })
}
//搜索日志
export function ApiGetSearchLog(projectId,) {
  return exportService({
    url: `/export/searchLog`,
    method: 'get',
    params: {
      projectId
    }
  })
}
//删除CRF
export function ApiDeleteExportCrf(params) {
  return axios({
    url: `/crf/deleteCRFFrom`,
    method: 'delete',
    params,
  })
}
//删除CRF子项
export function ApiDeleteDeleteCRFField(params) {
  return axios({
    url: `/crf/deleteCRFField`,
    method: 'delete',
    params,
  })
}
//删除访视矩阵
export function ApiDeleteEVENT(params) {
  return axios({
    url: `/visitConfig/deleteEVENT`,
    method: 'delete',
    params,
  })
}
//EVENT 和crf配置

export function ApiGetSearchEventCrf({ projectId, username }) {
  return axios({
    url: `/visitConfig/searchEventCrf`,
    method: 'get',
    params: {
      projectId,
      username
    }
  })
}

//EVENT 和crf配置
export function ApiPostUpdateVisit(params, projectId) {
  const username = JSON.parse(localStorage.getItem('user')).username;
  return axios({
    url: `/visitConfig/updateVisit`,
    method: 'post',
    data: params,
    params: { 
      projectId,
      username 
    }
  })
}
//新增修改EVENT
export function ApiPostVisitConfigAddOrUpdate(params, projectId) {
  const username = JSON.parse(localStorage.getItem('user')).username;
  return axios({
    url: `/visitConfig/addOrUpdate`,
    method: 'post',
    data: params,
    params: { 
      projectId,
      username 
    }
  })
}

// //添加/修改EVENT
// export function ApiPostAddOrUpdate(params) {
//   return axios({
//     url: `/visitConfig/addOrUpdate`,
//     method: 'post',
//     data: params
//   })
// }
// //添加/修改EVENT
// export function ApiPostAddOrUpdate(params) {
//   return axios({
//     url: `/visitConfig/addOrUpdate`,
//     method: 'post',
//     data: params
//   })
// }
//添加/修改CRF
export function ApiPostCrfAddOrUpdate(params) {
  return axios({
    url: `/crf/addOrUpdate`,
    method: 'post',
    data: params,
  })
}

//保存为模板
export function ApiPostSaveTemplate(params) {
  return axios({
    url: `/api/template/save`,
    method: 'post',
    data: params,
  })
}

//获取模板列表
export function ApiGetTemplateList(category, username) {
  return axios({
    url: `/api/template/list`,
    method: 'get',
    params: { 
      category,
      username
    }
  })
}

//获取模板详情(包含字段信息)
export function ApiGetTemplateDetail(templateId) {
  return axios({
    url: `/api/template/detail/${templateId}`,
    method: 'get'
  })
}

//批量复制模板
export function ApiBatchCopyTemplates(params) {
  return axios({
    url: `/api/template/batchCopy`,
    method: 'post',
    data: params,
  })
}

//查询CRF
export const ApiGetCrfSearchCRF = (projectId) => {
  return axios({
    url: '/crf/searchCRF',
    method: 'get',
    params: { projectId }
  })
}
//导出CRF
export function getCrfExcelTemplete() {
  return exportService({
    url: `/export/crfExcelTemplete`,
    method: 'get',
  })
}
//查询CRF
export function getCrfPreview(crfConfigId) {
  return exportService({
    url: `/crf/crfPreview?crfConfigId=${crfConfigId}`,
    method: 'get',
    responseType: 'blob',
    Headers: {
      'Content-Type': 'application/octet-stream',
    },
  })
}
//导出基础原始CRF Exce
export function getBaseCrfExcel(formOid) {
  return exportService({
    url: `/export/baseCrfExcel`,
    method: 'get',
  })
}
//导入添加CRF模板
export function uploadCrfExcelTemplete(file) {
  return exportService({
    url: `/export/uploadCrfExcelTemplete`,
    method: 'post',
  })
}
//导入添加CRF模板
export function apiGetUploadCFR(file) {
  return axios({
    url: `/crf/uploadCFR`,
    method: 'get',
  })
}


//导出添加CRF模板
export function apiGetcrfSpec(projectId, username) {
  return exportService({
    url: `/export/crfSpec`,
    method: 'get',
    params: {
      projectId,
      username
    }
  })
}

//查询CRF配置Field 新
export function apiGetSearchCRFField(formOid, projectId) {
  return axios({
    url: `/crf/searchCRFField?formOid=${formOid}&projectId=${projectId}`,
    method: 'get',
  })
}

//根据CRF配置ID查询字段 新
export function apiGetSearchCRFFieldById(crfConfigId, projectId) {
  return axios({
    url: `/crf/searchCRFFieldById?crfConfigId=${crfConfigId}&projectId=${projectId}`,
    method: 'get',
  })
}

//查询CFR配置Field 新
export function apiPostAddOrUpdateField(params) {
  return axios({
    url: `/crf/addOrUpdateField`,
    method: 'post',
    data: params,
  })
}

//字段调整序号
export function apiGetFieldChangeOrder({ fromFieldId, toFieldId }, projectId, username) {
  // 确保将ID转换为字符串
  const params = {
    fromFieldId: String(fromFieldId),
    toFieldId: String(toFieldId),
    projectId,
    username
  };
  
  return axios({
    url: `/crf-conf/changeFieldOrder`,
    method: 'get',
    params
  })
}

//crf调整序号 新
export function apiGetConfChangeOrder(params, projectId, username) {
  // 确保将ID转换为字符串
  const convertedParams = {
    fromCrfConfigId: String(params.fromCrfConfigId),
    toArmCrfConfigId: params.toArmCrfConfigId ? String(params.toArmCrfConfigId) : null,
    projectId,
    username
  };
  
  return axios({
    url: `/crf-conf/changeOrder`,
    method: 'get',
    params: convertedParams
  })
}

//Event调整序号
export function apiGetChangeEventOrder({ fromEventId, toEventId }, projectId) {
  return axios({
    url: `/visitConfig/changeEventOrder`,
    method: 'get',
    params: {
      fromEventId,
      toEventId,
      projectId
    }
  })
}

// 获取项目列表
export const ApiGetProjects = (username) => {
  return axios({
    url: 'http://localhost:9201/project-management/list/active/my',
    method: 'get',
    headers: {
      'username': username
    }
  })
}

// 创建项目
export const ApiCreateProject = (data) => {
  const username = data.username || (localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : '');
  return axios({
    url: 'http://localhost:9201/project-management/create',
    method: 'post',
    data,
    headers: {
      'username': username
    }
  })
}

// 复制项目
// export const ApiCopyProject = (data) => {
//   return axios({
//     url: '/api/projects/copy',
//     method: 'post',
//     data
//   })
// }

// 获取项目详情
export const ApiGetProjectDetail = (projectId) => {
  return axios({
    url: `/planInfo/getByProjectId/${projectId}`,
    method: 'get'
  })
}

// 获取单个项目信息
export const ApiGetProject = (projectId) => {
  return axios({
    url: 'http://localhost:9201/project-management/info',
    method: 'get',
    params: { projectId }
  })
}


// 更新项目
export const ApiUpdateProject = (id, data) => {
  const username = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : '';
  return axios({
    url: 'http://localhost:9201/project-management/update',
    method: 'put',
    data: {
      projectId: id,
      ...data
    },
    headers: {
      'username': username
    }
  })
}

// 删除项目
export function ApiDeleteProject(params) {
  const username = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : '';
  return axios({
    url: `http://localhost:9201/project-management/delete`,
    method: 'delete',
    params,
    headers: {
      'username': username
    }
  })
}

// 模板类别相关API
export function ApiGetTemplateCategoryList(username) {
  return axios({
    url: `/api/template-category/list`,
    method: 'get',
    params: { username }
  })
}

export function ApiGetTemplateCategoryDetail() {
  return axios({
    url: `/api/template-category/detail/list`,
    method: 'get'
  })
}

export function ApiCreateTemplateCategory(params) {
  return axios({
    url: `/api/template-category/create`,
    method: 'post',
    params
  })
}

// 项目配置相关API
// 获取项目配置
export const ApiGetProjectConfig = (projectId) => {
  return axios({
    url: `http://localhost:9201/project-config/get`,
    method: 'get',
    params: { projectId }
  })
}

// 保存项目配置
export const ApiSaveProjectConfig = (data) => {
  const username = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : '';
  return axios({
    url: `http://localhost:9201/project-config/save`,
    method: 'post',
    data: {
      ...data,
      creator: username
    },
    headers: {
      'username': username
    }
  })
}

// 更新项目配置
export const ApiUpdateProjectConfig = (data) => {
  const username = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : '';
  return axios({
    url: `http://localhost:9201/project-config/update`,
    method: 'put',
    data,
    headers: {
      'username': username
    }
  })
}

// ========== 统一文件管理 API (新版) ==========

export const ApiGetProjectFiles = (projectId, fileCategory) => {
  const categoryMap = {
    'acrf': 'ACRF', 'p21_spec': 'P21_SPEC', 'project_spec': 'PROJECT_SPEC', 'xpt': 'XPT',
    'send_acrf': 'ACRF', 'adam_acrf': 'ACRF',
    'send_p21_spec': 'P21_SPEC', 'adam_p21_spec': 'P21_SPEC',
    'send_project_spec': 'PROJECT_SPEC', 'adam_project_spec': 'PROJECT_SPEC',
    'send_xpt': 'XPT', 'adam_xpt': 'XPT'
  }
  const normalized = categoryMap[fileCategory] || fileCategory
  return axios({
    url: 'http://localhost:9201/files/list',
    method: 'get',
    params: { projectId, fileCategory: normalized }
  })
}

export const ApiGetCurrentFile = (projectId, fileCategory) => {
  return axios({
    url: 'http://localhost:9201/files/current',
    method: 'get',
    params: { projectId, fileCategory }
  })
}

export const ApiDeleteProjectFile = (fileId) => {
  return axios({
    url: 'http://localhost:9201/files/delete',
    method: 'delete',
    params: { fileId }
  })
}

export const ApiProcessProjectFile = (fileId, fileType) => {
  const username = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : ''
  const urlMap = {
    acrf: 'http://localhost:9201/acrf/process',
    p21spec: 'http://localhost:9201/p21-spec/process',
    projectspec: 'http://localhost:9201/project-spec/process',
    xpt: 'http://localhost:9201/xpt/process'
  }
  const url = urlMap[fileType] || 'http://localhost:9201/acrf/process'
  return axios({ url, method: 'post', data: { fileId }, headers: { 'username': username } })
}

