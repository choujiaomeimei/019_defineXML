package com.stat.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stat.dal.po.DefineSheetDataPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DefineSheetDataMapper extends BaseMapper<DefineSheetDataPO> {

    @Select("SELECT * FROM define_sheet_data WHERE project_id = #{projectId} AND file_name = #{fileName} ORDER BY updated_time DESC LIMIT 1")
    DefineSheetDataPO selectByProjectAndFile(@Param("projectId") String projectId, @Param("fileName") String fileName);
}
