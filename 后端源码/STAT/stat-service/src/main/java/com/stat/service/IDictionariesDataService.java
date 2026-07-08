package com.stat.service;

import com.stat.common.dto.DictionariesDataDTO;

import java.util.List;

public interface IDictionariesDataService {

    boolean addDictionariesData(DictionariesDataDTO dto);

    boolean updateDictionariesData(DictionariesDataDTO dto);

    boolean deleteDictionariesData(Long id);

    DictionariesDataDTO getDictionariesDataById(Long id);

    List<DictionariesDataDTO> getDictionariesDataByProjectId(String projectId);

    boolean deleteByProjectId(String projectId);
}
