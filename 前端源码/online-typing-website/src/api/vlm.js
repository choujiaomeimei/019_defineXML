import request from '@/axios'

// VLM数据管理API

/**
 * 分页查询VLM数据
 */
export function getVlmList(params) {
  return request({
    url: '/api/vlm/list',
    method: 'get',
    params
  })
}

/**
 * 根据项目ID查询所有VLM数据
 */
export function getVlmByProject(projectId) {
  return request({
    url: `/api/vlm/project/${projectId}`,
    method: 'get'
  })
}

/**
 * 根据项目ID和数据集查询VLM数据
 */
export function getVlmByProjectAndDataset(projectId, dataset) {
  return request({
    url: `/api/vlm/project/${projectId}/dataset/${dataset}`,
    method: 'get'
  })
}

/**
 * 根据ID查询VLM数据详情
 */
export function getVlmById(id) {
  return request({
    url: `/api/vlm/${id}`,
    method: 'get'
  })
}

/**
 * 新增VLM数据
 */
export function createVlmData(data) {
  return request({
    url: '/api/vlm',
    method: 'post',
    data
  })
}

/**
 * 更新VLM数据
 */
export function updateVlmData(id, data) {
  return request({
    url: `/api/vlm/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除VLM数据
 */
export function deleteVlmData(id) {
  return request({
    url: `/api/vlm/${id}`,
    method: 'delete'
  })
}

/**
 * 批量更新排序顺序
 */
export function updateVlmSortOrder(sortOrderList) {
  return request({
    url: '/api/vlm/sort-order',
    method: 'put',
    data: sortOrderList
  })
}

/**
 * 获取项目中所有不同的数据集列表
 */
export function getVlmDatasets(projectId) {
  return request({
    url: `/api/vlm/datasets/${projectId}`,
    method: 'get'
  })
}