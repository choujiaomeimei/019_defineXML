package com.stat.service.impl;

import com.stat.common.dto.CodelistDataDTO;
import com.stat.common.dto.VlmDataDTO;
import com.stat.common.result.PageCommonResult;
import com.stat.dal.mapper.CodelistDataMapper;
import com.stat.dal.po.CodelistDataPO;
import com.stat.common.security.UserContext;
import com.stat.service.ICodelistDataService;
import com.stat.service.IVlmDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CodeList数据服务实现类
 */
@Service
public class CodelistDataServiceImpl implements ICodelistDataService {

    @Autowired
    private CodelistDataMapper codelistDataMapper;
    
    @Autowired
    private IVlmDataService vlmDataService;

    @Override
    public boolean addCodelistData(CodelistDataDTO codelistDataDTO) {
        try {
            CodelistDataPO codelistDataPO = new CodelistDataPO();
            BeanUtils.copyProperties(codelistDataDTO, codelistDataPO);
            codelistDataPO.setUsername(UserContext.getUsername());
            return codelistDataMapper.insert(codelistDataPO) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateCodelistData(CodelistDataDTO codelistDataDTO) {
        try {
            CodelistDataPO codelistDataPO = new CodelistDataPO();
            BeanUtils.copyProperties(codelistDataDTO, codelistDataPO);
            return codelistDataMapper.updateById(codelistDataPO) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteCodelistData(Long id) {
        try {
            return codelistDataMapper.deleteById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CodelistDataDTO getCodelistDataById(Long id) {
        try {
            CodelistDataPO codelistDataPO = codelistDataMapper.selectById(id);
            if (codelistDataPO != null) {
                CodelistDataDTO codelistDataDTO = new CodelistDataDTO();
                BeanUtils.copyProperties(codelistDataPO, codelistDataDTO);
                return codelistDataDTO;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<CodelistDataDTO> getCodelistDataByProjectId(String projectId) {
        try {
            List<CodelistDataPO> codelistDataPOList = codelistDataMapper.selectByProjectId(projectId, UserContext.getUsername());
            return convertPOListToDTOList(codelistDataPOList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<CodelistDataDTO> getCodelistDataByProjectIdAndVcd(String projectId, String vcd) {
        try {
            List<CodelistDataPO> codelistDataPOList = codelistDataMapper.selectByProjectIdAndVcd(projectId, vcd, UserContext.getUsername());
            return convertPOListToDTOList(codelistDataPOList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public PageCommonResult<CodelistDataDTO> getCodelistDataByPage(Map<String, Object> params) {
        System.out.println("CodelistDataService.getCodelistDataByPage 被调用");
        System.out.println("传入参数: " + params);
        
        try {
            // 直接传递params中的值给MyBatis
            List<CodelistDataPO> codelistDataPOList = codelistDataMapper.selectByPage(params);
            System.out.println("数据库查询返回记录数: " + (codelistDataPOList != null ? codelistDataPOList.size() : 0));
            
            Integer total = codelistDataMapper.countByParams(params);
            System.out.println("总记录数: " + total);
            
            List<CodelistDataDTO> codelistDataDTOList = convertPOListToDTOList(codelistDataPOList);
            
            PageCommonResult<CodelistDataDTO> result = new PageCommonResult<>();
            result.setData(codelistDataDTOList);
            result.setTotalCount(total);
            Object pageObj = params.get("page");
            Object sizeObj = params.get("size");
            int page = (pageObj instanceof Integer) ? (Integer) pageObj : 1;
            int size = (sizeObj instanceof Integer) ? (Integer) sizeObj : 20;
            
            result.setPageNum(page);
            result.setPageSize(size);
            // 确保数据格式正确
            result.setSuccess(true);
            result.setCode("200");
            result.setMessage("");
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new PageCommonResult<>();
        }
    }

    @Override
    public boolean batchUpdateSortOrder(List<Map<String, Object>> sortOrderList) {
        try {
            return codelistDataMapper.batchUpdateSortOrder(sortOrderList) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getDistinctVcdsByProjectId(String projectId) {
        try {
            return codelistDataMapper.selectDistinctVcdsByProjectId(projectId, UserContext.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteByProjectId(String projectId) {
        try {
            return codelistDataMapper.deleteByProjectId(projectId, UserContext.getUsername()) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Object> getVcdDomainsMapping(String projectId) {
        try {
            System.out.println("开始获取VCD-Domain映射，项目ID: " + projectId);
            
            // 获取所有CodeList数据
            List<CodelistDataDTO> codelistData = this.getCodelistDataByProjectId(projectId);
            System.out.println("CodeList数据条数: " + codelistData.size());
            
            // 获取所有VLM数据来建立VCD与Dataset的映射关系
            List<VlmDataDTO> vlmData = vlmDataService.getVlmDataByProjectId(projectId);
            System.out.println("VLM数据条数: " + vlmData.size());
            
            // 建立VCD到Domain的映射
            Map<String, Set<String>> vcdToDomains = new HashMap<>();
            Map<String, String> vcdToLabel = new HashMap<>();
            Map<String, Integer> vcdToCount = new HashMap<>();
            
            // 从VLM数据中获取VCD在哪些dataset中使用
            for (VlmDataDTO vlm : vlmData) {
                String variable = vlm.getVariable();
                String dataset = vlm.getDataset();
                
                if (variable != null && dataset != null && !variable.trim().isEmpty() && !dataset.trim().isEmpty()) {
                    // 检查variable是否以dataset开头，如果是则可能是VCD
                    String possibleVcd = extractVcdFromVariable(variable, dataset);
                    if (possibleVcd != null) {
                        vcdToDomains.computeIfAbsent(possibleVcd, k -> new HashSet<>()).add(dataset);
                        vcdToLabel.put(possibleVcd, vlm.getLabel() != null ? vlm.getLabel() : "");
                    }
                }
            }
            
            // 从CodeList数据中获取VCD信息和统计
            for (CodelistDataDTO codelist : codelistData) {
                String vcd = codelist.getVcd();
                String vlabel = codelist.getVlabel();
                
                if (vcd != null && !vcd.trim().isEmpty()) {
                    vcdToCount.put(vcd, vcdToCount.getOrDefault(vcd, 0) + 1);
                    if (vlabel != null && !vlabel.trim().isEmpty()) {
                        vcdToLabel.put(vcd, vlabel);
                    }
                }
            }
            
            // 构建结果
            List<Map<String, Object>> vcdList = new ArrayList<>();
            for (String vcd : vcdToCount.keySet()) {
                Map<String, Object> vcdInfo = new HashMap<>();
                vcdInfo.put("vcd", vcd);
                vcdInfo.put("vlabel", vcdToLabel.getOrDefault(vcd, ""));
                vcdInfo.put("count", vcdToCount.get(vcd));
                
                Set<String> domains = vcdToDomains.get(vcd);
                if (domains != null && !domains.isEmpty()) {
                    vcdInfo.put("domains", new ArrayList<>(domains));
                } else {
                    vcdInfo.put("domains", new ArrayList<>());
                }
                
                vcdList.add(vcdInfo);
            }
            
            // 按VCD名称排序
            vcdList.sort((a, b) -> ((String) a.get("vcd")).compareTo((String) b.get("vcd")));
            
            Map<String, Object> result = new HashMap<>();
            result.put("vcdList", vcdList);
            result.put("totalVcds", vcdList.size());
            
            System.out.println("VCD-Domain映射完成，VCD总数: " + vcdList.size());
            return result;
            
        } catch (Exception e) {
            System.out.println("获取VCD-Domain映射异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取VCD域映射失败", e);
        }
    }
    
    /**
     * 从变量名中提取VCD
     * 例如: DMARM -> ARM, AESTDT -> EST等
     */
    private String extractVcdFromVariable(String variable, String dataset) {
        if (variable == null || dataset == null || variable.length() <= dataset.length()) {
            return null;
        }
        
        // 如果变量名以dataset开头，则提取后面的部分作为VCD
        if (variable.toUpperCase().startsWith(dataset.toUpperCase())) {
            String vcd = variable.substring(dataset.length());
            if (vcd.length() > 0) {
                return vcd.toUpperCase();
            }
        }
        
        return null;
    }

    /**
     * 将PO列表转换为DTO列表
     */
    private List<CodelistDataDTO> convertPOListToDTOList(List<CodelistDataPO> codelistDataPOList) {
        List<CodelistDataDTO> codelistDataDTOList = new ArrayList<>();
        for (CodelistDataPO codelistDataPO : codelistDataPOList) {
            CodelistDataDTO codelistDataDTO = new CodelistDataDTO();
            BeanUtils.copyProperties(codelistDataPO, codelistDataDTO);
            codelistDataDTOList.add(codelistDataDTO);
        }
        return codelistDataDTOList;
    }
}