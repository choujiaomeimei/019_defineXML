/**
 * API模块类型声明
 */

export function ApiGetSearchLog(projectId: string, username?: string): Promise<any>;

export function getBaseCrfExcel(formOid?: string): Promise<any>;

export function ApiPostExportCrf(params: any): Promise<any>;

export function ApiPostAddOrUpdate(params: any): Promise<any>;

// 密码修改
export function postPassword(params: {
  username: string;
  oldPassword: string;
  newPassword: string;
}): Promise<any>;

export function ApiGetCrfSearchCRF(projectId: string): Promise<any>;

export function ApiPostCrfPreviewObject(crfConfigId: string, projectId: string): Promise<any>;

export function ApiGetTemplateList(category: string, username: string): Promise<any>;

export function ApiGetTemplateDetail(id: string): Promise<any>;

export function ApiPostCrfAddOrUpdate(data: any): Promise<any>;

export function ApiGetProjects(username: string): Promise<any>;

export function ApiCreateProject(data: any): Promise<any>;

export function ApiCopyProject(data: any): Promise<any>;

export function ApiDeleteProject(params: any): Promise<any>;

export function ApiGetProjectDetail(projectId: string): Promise<any>;

export function ApiGetProject(projectId: string): Promise<any>;

export function ApiUpdateProject(id: string, data: any): Promise<any>;

// 添加缺失的API函数定义
export function ApiDeleteExportCrf(params: any): Promise<any>;
export function ApiDeleteDeleteCRFField(params: any): Promise<any>;
export function apiGetUploadCFR(file: any): Promise<any>;
export function apiGetSearchCRFField(formOid: string, projectId: string): Promise<any>;
export function apiPostAddOrUpdateField(params: any): Promise<any>;
export function apiGetConfChangeOrder(params: any, projectId: string, username: string): Promise<any>;
export function apiGetFieldChangeOrder(params: any, projectId: string, username: string): Promise<any>;
export function apiGetSearchCRFFieldById(crfConfigId: number, projectId: string): Promise<any>;
export function ApiPostSaveTemplate(params: any): Promise<any>;
export function ApiBatchCopyTemplates(params: any): Promise<any>;
export function ApiGetTemplateCategoryList(username: string): Promise<any>;

// 访视相关API函数定义
export function ApiGetOrSearchEVENT(projectId: string): Promise<any>;
export function ApiDeleteEVENT(params: any): Promise<any>;
export function ApiGetSearchEventCrf(params: { projectId: string, username: string }): Promise<any>;
export function ApiPostUpdateVisit(params: any, projectId: string): Promise<any>;
export function ApiPostVisitConfigAddOrUpdate(params: any, projectId: string): Promise<any>;
export function apiGetChangeEventOrder(params: any, projectId: string): Promise<any>; 