package com.stat.service;

import com.stat.common.dto.DocumentsDataDTO;

import java.util.List;

public interface IDocumentsDataService {

    boolean addDocumentsData(DocumentsDataDTO dto);

    boolean updateDocumentsData(DocumentsDataDTO dto);

    boolean deleteDocumentsData(Long id);

    DocumentsDataDTO getDocumentsDataById(Long id);

    List<DocumentsDataDTO> getDocumentsDataByProjectId(String projectId);

    boolean deleteByProjectId(String projectId);
}
