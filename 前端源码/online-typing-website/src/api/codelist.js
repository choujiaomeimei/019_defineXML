import request from '@/axios'

// CodeList数据管理API

/**
 * 分页查询CodeList数据
 */
export function getCodelistList(params) {
  return request({
    url: '/api/codelist/list',
    method: 'get',
    params
  }).then(response => {
    console.log('API原始响应:', response)
    // 返回response.data，因为axios已经解包了一层
    return response.data
  })
}

/**
 * 根据项目ID查询所有CodeList数据
 */
export function getCodelistByProject(projectId) {
  return request({
    url: `/api/codelist/project/${projectId}`,
    method: 'get'
  }).then(response => response.data)
}

/**
 * 根据项目ID和VCD查询CodeList数据
 */
export function getCodelistByProjectAndVcd(projectId, vcd) {
  return request({
    url: `/api/codelist/project/${projectId}/vcd/${vcd}`,
    method: 'get'
  })
}

/**
 * 根据ID查询CodeList数据详情
 */
export function getCodelistById(id) {
  return request({
    url: `/api/codelist/${id}`,
    method: 'get'
  })
}

/**
 * 新增CodeList数据
 */
export function createCodelistData(data) {
  return request({
    url: '/api/codelist',
    method: 'post',
    data
  }).then(response => response.data)
}

/**
 * 更新CodeList数据
 */
export function updateCodelistData(id, data) {
  return request({
    url: `/api/codelist/${id}`,
    method: 'put',
    data
  }).then(response => response.data)
}

/**
 * 删除CodeList数据
 */
export function deleteCodelistData(id) {
  return request({
    url: `/api/codelist/${id}`,
    method: 'delete'
  }).then(response => response.data)
}

/**
 * 批量更新排序顺序
 */
export function updateCodelistSortOrder(sortOrderList) {
  return request({
    url: '/api/codelist/sort-order',
    method: 'put',
    data: sortOrderList
  }).then(response => response.data)
}

/**
 * 获取项目中所有不同的VCD列表
 */
export function getCodelistVcds(projectId) {
  return request({
    url: `/api/codelist/vcds/${projectId}`,
    method: 'get'
  }).then(response => response.data)
}

/**
 * 获取VCD与Domain的映射关系
 */
export function getVcdDomainsMapping(projectId) {
  return request({
    url: `/api/codelist/vcd-domains/${projectId}`,
    method: 'get'
  }).then(response => response.data)
}