package com.stat.service.impl;

import com.stat.common.dto.VlmDataDTO;
import com.stat.common.result.PageCommonResult;
import com.stat.dal.mapper.VlmDataMapper;
import com.stat.dal.po.VlmDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.IVlmDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VLM数据服务实现类
 */
@Service
public class VlmDataServiceImpl implements IVlmDataService {

    @Autowired
    private VlmDataMapper vlmDataMapper;

    @Override
    public boolean addVlmData(VlmDataDTO vlmDataDTO) {
        try {
            VlmDataPO vlmDataPO = new VlmDataPO();
            BeanUtils.copyProperties(vlmDataDTO, vlmDataPO);
            vlmDataPO.setUsername(UserContext.getUsername());
            return vlmDataMapper.insert(vlmDataPO) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateVlmData(VlmDataDTO vlmDataDTO) {
        try {
            VlmDataPO vlmDataPO = new VlmDataPO();
            BeanUtils.copyProperties(vlmDataDTO, vlmDataPO);
            return vlmDataMapper.updateById(vlmDataPO) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteVlmData(Long id) {
        try {
            return vlmDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public VlmDataDTO getVlmDataById(Long id) {
        try {
            VlmDataPO vlmDataPO = vlmDataMapper.selectById(id);
            if (vlmDataPO != null) {
                VlmDataDTO vlmDataDTO = new VlmDataDTO();
                BeanUtils.copyProperties(vlmDataPO, vlmDataDTO);
                return vlmDataDTO;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<VlmDataDTO> getVlmDataByProjectId(String projectId) {
        try {
            List<VlmDataPO> vlmDataPOList = vlmDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            return convertPOListToDTOList(vlmDataPOList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<VlmDataDTO> getVlmDataByProjectIdAndDataset(String projectId, String dataset) {
        try {
            List<VlmDataPO> vlmDataPOList = vlmDataMapper.selectByProjectIdAndDataset(projectId, dataset, UserContext.getUsername());
            return convertPOListToDTOList(vlmDataPOList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public PageCommonResult<VlmDataDTO> getVlmDataByPage(Map<String, Object> params) {
        try {
            List<VlmDataPO> vlmDataPOList = vlmDataMapper.selectByPage(params);
            Integer total = vlmDataMapper.countByParams(params);
            
            List<VlmDataDTO> vlmDataDTOList = convertPOListToDTOList(vlmDataPOList);
            
            // Extract page parameters
            Integer limit = (Integer) params.get("limit");
            Integer offset = (Integer) params.get("offset");
            int pageSize = limit != null ? limit : 20;
            int currentPage = offset != null && pageSize > 0 ? (offset / pageSize) + 1 : 1;
            
            PageCommonResult<VlmDataDTO> result = new PageCommonResult<>();
            result.setData(vlmDataDTOList);
            result.setPageSize(pageSize);
            result.setPageNum(currentPage);
            result.setTotalCount(total != null ? total.longValue() : 0);
            result.setSuccess(true);
            result.setCode("0");
            result.setMessage("");
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            PageCommonResult<VlmDataDTO> errorResult = new PageCommonResult<>();
            errorResult.setSuccess(false);
            errorResult.setCode("500");
            errorResult.setMessage("查询失败: " + e.getMessage());
            return errorResult;
        }
    }

    @Override
    public boolean batchUpdateSortOrder(List<Map<String, Object>> sortOrderList) {
        try {
            return vlmDataMapper.batchUpdateSortOrder(sortOrderList) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getDistinctDatasetsByProjectId(String projectId) {
        try {
            return vlmDataMapper.selectDistinctDatasetsByProjectId(projectId, UserContext.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteByProjectId(String projectId) {
        try {
            return vlmDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将PO列表转换为DTO列表
     */
    private List<VlmDataDTO> convertPOListToDTOList(List<VlmDataPO> vlmDataPOList) {
        List<VlmDataDTO> vlmDataDTOList = new ArrayList<>();
        for (VlmDataPO vlmDataPO : vlmDataPOList) {
            VlmDataDTO vlmDataDTO = new VlmDataDTO();
            BeanUtils.copyProperties(vlmDataPO, vlmDataDTO);
            vlmDataDTOList.add(vlmDataDTO);
        }
        return vlmDataDTOList;
    }
}