package com.stat.service.impl;

import com.stat.common.dto.PagesDataDTO;
import com.stat.dal.mapper.PagesDataMapper;
import com.stat.dal.po.PagesDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.IPagesDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PagesDataServiceImpl implements IPagesDataService {

    @Autowired
    private PagesDataMapper pagesDataMapper;

    @Override
    public boolean addPagesData(PagesDataDTO dto) {
        try {
            PagesDataPO po = new PagesDataPO();
            BeanUtils.copyProperties(dto, po);
            po.setUsername(UserContext.getUsername());
            return pagesDataMapper.insert(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updatePagesData(PagesDataDTO dto) {
        try {
            PagesDataPO po = new PagesDataPO();
            BeanUtils.copyProperties(dto, po);
            return pagesDataMapper.updateById(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deletePagesData(Long id) {
        try {
            return pagesDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public PagesDataDTO getPagesDataById(Long id) {
        try {
            PagesDataPO po = pagesDataMapper.selectById(id);
            if (po != null) {
                PagesDataDTO dto = new PagesDataDTO();
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
    public List<PagesDataDTO> getPagesDataByProjectId(String projectId) {
        try {
            List<PagesDataPO> poList = pagesDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            return convertList(poList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<PagesDataDTO> getPagesDataByProjectIdAndDataset(String projectId, String dataset) {
        try {
            List<PagesDataPO> poList = pagesDataMapper.selectByProjectIdAndDataset(projectId, dataset, UserContext.getUsername());
            return convertList(poList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getDistinctDatasetsByProjectId(String projectId) {
        try {
            return pagesDataMapper.selectDistinctDatasetsByProjectId(projectId, UserContext.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteByProjectId(String projectId) {
        try {
            return pagesDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<PagesDataDTO> convertList(List<PagesDataPO> poList) {
        List<PagesDataDTO> dtoList = new ArrayList<>();
        for (PagesDataPO po : poList) {
            PagesDataDTO dto = new PagesDataDTO();
            BeanUtils.copyProperties(po, dto);
            dtoList.add(dto);
        }
        return dtoList;
    }
}
