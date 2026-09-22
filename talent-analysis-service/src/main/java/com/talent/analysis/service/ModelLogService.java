package com.talent.analysis.service;

import com.talent.analysis.vo.ModelEffectVO;
import com.talent.analysis.vo.ModelLogVO;
import com.talent.common.result.PageResult;

import java.util.List;

/**
 * 模型运行效果分析与日志溯源。
 */
public interface ModelLogService {

    /** 效果分析：运行次数、成功率、平均/最好指标、各模型汇总、版本趋势 */
    ModelEffectVO effect();

    /** 日志分页查询（溯源入口） */
    PageResult<ModelLogVO> page(String modelName, String status, String version, long pageNum, long pageSize);

    /** 某次训练的完整信息：参数、指标、数据版本 */
    ModelLogVO detail(Long id);

    /** 所有模型名，给筛选下拉用 */
    List<String> modelNames();
}
