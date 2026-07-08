package com.stat.service;

import com.stat.common.dto.PagesDataDTO;

import java.util.List;

public interface IPagesDataService {

    boolean addPagesData(PagesDataDTO dto);

    boolean updatePagesData(PagesDataDTO dto);

    boolean deletePagesData(Long id);

    PagesDataDTO getPagesDataById(Long id);

    List<PagesDataDTO> getPagesDataByProjectId(String projectId);

    List<PagesDataDTO> getPagesDataByProjectIdAndDataset(String projectId, String dataset);

    List<String> getDistinctDatasetsByProjectId(String projectId);

    boolean deleteByProjectId(String projectId);
}
