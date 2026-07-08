package com.stat.service;

import com.stat.common.dto.CommentsDataDTO;

import java.util.List;

public interface ICommentsDataService {

    boolean addCommentsData(CommentsDataDTO dto);

    boolean updateCommentsData(CommentsDataDTO dto);

    boolean deleteCommentsData(Long id);

    CommentsDataDTO getCommentsDataById(Long id);

    List<CommentsDataDTO> getCommentsDataByProjectId(String projectId);

    boolean deleteByProjectId(String projectId);
}
