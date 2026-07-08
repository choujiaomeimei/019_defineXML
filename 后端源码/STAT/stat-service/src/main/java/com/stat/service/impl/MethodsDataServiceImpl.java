package com.stat.service.impl;

import com.stat.common.dto.MethodsDataDTO;
import com.stat.dal.mapper.MethodsDataMapper;
import com.stat.dal.po.MethodsDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.IMethodsDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MethodsDataServiceImpl implements IMethodsDataService {

    @Autowired
    private MethodsDataMapper methodsDataMapper;

    @Override
    public boolean addMethodsData(MethodsDataDTO dto) {
        try {
            MethodsDataPO po = new MethodsDataPO();
            BeanUtils.copyProperties(dto, po);
            po.setUsername(UserContext.getUsername());
            return methodsDataMapper.insert(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateMethodsData(MethodsDataDTO dto) {
        try {
            MethodsDataPO po = new MethodsDataPO();
            BeanUtils.copyProperties(dto, po);
            return methodsDataMapper.updateById(po) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteMethodsData(Long id) {
        try {
            return methodsDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public MethodsDataDTO getMethodsDataById(Long id) {
        try {
            MethodsDataPO po = methodsDataMapper.selectById(id);
            if (po != null) {
                MethodsDataDTO dto = new MethodsDataDTO();
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
    public List<MethodsDataDTO> getMethodsDataByProjectId(String projectId) {
        try {
            List<MethodsDataPO> poList = methodsDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            List<MethodsDataDTO> dtoList = new ArrayList<>();
            for (MethodsDataPO po : poList) {
                MethodsDataDTO dto = new MethodsDataDTO();
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
            return methodsDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
