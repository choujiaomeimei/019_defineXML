package com.stat.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stat.common.entity.SnapshotFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SnapshotFileMapper extends BaseMapper<SnapshotFile> {

    List<SnapshotFile> selectBySnapshotId(@Param("snapshotId") Long snapshotId);
}
