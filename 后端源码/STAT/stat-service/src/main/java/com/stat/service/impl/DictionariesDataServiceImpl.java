package com.stat.service.impl;

import com.stat.common.dto.DictionariesDataDTO;
import com.stat.dal.mapper.DictionariesDataMapper;
import com.stat.dal.po.DictionariesDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.IDictionariesDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DictionariesDataServiceImpl implements IDictionariesDataService {

    @Autowired
    private DictionariesDataMapper dictionariesDataMapper;

    @Override
    public boolean addDictionariesData(DictionariesDataDTO dto) {
        try {
            DictionariesDataPO po = new DictionariesDataPO();
            BeanUtils.copyProperties(dto, po);
            po.setUsername(UserContext.getUsername());
            return dictionariesDataMapper.insert(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateDictionariesData(DictionariesDataDTO dto) {
        try {
            DictionariesDataPO po = new DictionariesDataPO();
            BeanUtils.copyProperties(dto, po);
            return dictionariesDataMapper.updateById(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteDictionariesData(Long id) {
        try {
            return dictionariesDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public DictionariesDataDTO getDictionariesDataById(Long id) {
        try {
            DictionariesDataPO po = dictionariesDataMapper.selectById(id);
            if (po != null) {
                DictionariesDataDTO dto = new DictionariesDataDTO();
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
    public List<DictionariesDataDTO> getDictionariesDataByProjectId(String projectId) {
        try {
            List<DictionariesDataPO> poList = dictionariesDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            List<DictionariesDataDTO> dtoList = new ArrayList<>();
            for (DictionariesDataPO po : poList) {
                DictionariesDataDTO dto = new DictionariesDataDTO();
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
            return dictionariesDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
