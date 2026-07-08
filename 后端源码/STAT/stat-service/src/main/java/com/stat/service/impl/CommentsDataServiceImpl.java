package com.stat.service.impl;

import com.stat.common.dto.CommentsDataDTO;
import com.stat.dal.mapper.CommentsDataMapper;
import com.stat.dal.po.CommentsDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.ICommentsDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentsDataServiceImpl implements ICommentsDataService {

    @Autowired
    private CommentsDataMapper commentsDataMapper;

    @Override
    public boolean addCommentsData(CommentsDataDTO dto) {
        try {
            CommentsDataPO po = new CommentsDataPO();
            BeanUtils.copyProperties(dto, po);
            po.setUsername(UserContext.getUsername());
            return commentsDataMapper.insert(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateCommentsData(CommentsDataDTO dto) {
        try {
            CommentsDataPO po = new CommentsDataPO();
            BeanUtils.copyProperties(dto, po);
            return commentsDataMapper.updateById(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteCommentsData(Long id) {
        try {
            return commentsDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CommentsDataDTO getCommentsDataById(Long id) {
        try {
            CommentsDataPO po = commentsDataMapper.selectById(id);
            if (po != null) {
                CommentsDataDTO dto = new CommentsDataDTO();
                BeanUtils.copyProperties(po, dto);
                return dto;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<CommentsDataDTO> getCommentsDataByProjectId(String projectId) {
        try {
            List<CommentsDataPO> poList = commentsDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            List<CommentsDataDTO> dtoList = new ArrayList<>();
            for (CommentsDataPO po : poList) {
                CommentsDataDTO dto = new CommentsDataDTO();
                BeanUtils.copyProperties(po, dto);
                dtoList.add(dto);
            }
            return dtoList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteByProjectId(String projectId) {
        try {
            return commentsDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
