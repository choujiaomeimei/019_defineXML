package com.stat.service.impl;

import com.stat.common.dto.DocumentsDataDTO;
import com.stat.dal.mapper.DocumentsDataMapper;
import com.stat.dal.po.DocumentsDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.IDocumentsDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentsDataServiceImpl implements IDocumentsDataService {

    @Autowired
    private DocumentsDataMapper documentsDataMapper;

    @Override
    public boolean addDocumentsData(DocumentsDataDTO dto) {
        try {
            DocumentsDataPO po = new DocumentsDataPO();
            BeanUtils.copyProperties(dto, po);
            po.setUsername(UserContext.getUsername());
            return documentsDataMapper.insert(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateDocumentsData(DocumentsDataDTO dto) {
        try {
            DocumentsDataPO po = new DocumentsDataPO();
            BeanUtils.copyProperties(dto, po);
            return documentsDataMapper.updateById(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteDocumentsData(Long id) {
        try {
            return documentsDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public DocumentsDataDTO getDocumentsDataById(Long id) {
        try {
            DocumentsDataPO po = documentsDataMapper.selectById(id);
            if (po != null) {
                DocumentsDataDTO dto = new DocumentsDataDTO();
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
    public List<DocumentsDataDTO> getDocumentsDataByProjectId(String projectId) {
        try {
            List<DocumentsDataPO> poList = documentsDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            List<DocumentsDataDTO> dtoList = new ArrayList<>();
            for (DocumentsDataPO po : poList) {
                DocumentsDataDTO dto = new DocumentsDataDTO();
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
            return documentsDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
