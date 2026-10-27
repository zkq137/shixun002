package com.talent.employee.service;

import com.talent.employee.dto.PotentialSaveDTO;
import com.talent.employee.vo.PotentialModelVO;
import com.talent.employee.vo.PotentialVO;

import java.util.List;

/**
 * 潜力评估与量化建模。
 */
public interface PotentialService {

    /** 指标定义、权重、等级规则 */
    PotentialModelVO model();

    /** 查询某员工潜力 */
    PotentialVO get(Long employeeId);

    /** 录入/覆盖某员工指标 */
    PotentialVO save(PotentialSaveDTO dto);

    /** 批量录入 */
    int batchSave(List<PotentialSaveDTO> records);

    /** 用现有数据初始化量化指标（force=false 时只补空记录） */
    int initModel(boolean force);
}
