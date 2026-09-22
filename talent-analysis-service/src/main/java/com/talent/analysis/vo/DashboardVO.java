package com.talent.analysis.vo;

import com.talent.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

/**
 * 人才盘点看板。
 */
@Data
public class DashboardVO {

    private Long totalEmployees;
    private Long positionCount;
    private Long keyPositionCount;
    private Long highRiskCount;

    private Double avgSalary;
    private Double avgTenure;
    private Double avgAge;

    private List<NameValueVO> departmentDistribution;
    private List<NameValueVO> positionTop;
    private List<NameValueVO> talentTagDistribution;
    private List<NameValueVO> potentialDistribution;
    private List<NameValueVO> warningDistribution;
    private List<NameValueVO> perfDistribution;
}
