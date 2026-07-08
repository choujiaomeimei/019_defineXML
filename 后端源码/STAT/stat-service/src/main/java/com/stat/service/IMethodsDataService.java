package com.stat.service;

import com.stat.common.dto.MethodsDataDTO;

import java.util.List;

public interface IMethodsDataService {

    boolean addMethodsData(MethodsDataDTO dto);

    boolean updateMethodsData(MethodsDataDTO dto);

    boolean deleteMethodsData(Long id);

    MethodsDataDTO getMethodsDataById(Long id);

    List<MethodsDataDTO> getMethodsDataByProjectId(String projectId);

    boolean deleteByProjectId(String projectId);
}
