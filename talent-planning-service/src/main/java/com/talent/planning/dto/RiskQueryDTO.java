package com.talent.planning.dto;

import lombok.Data;

@Data
public class RiskQueryDTO {
    private long pageNum = 1;
    private long pageSize = 10;
    private String warningLevel;
    private String handleStatus;
    private String department;
    private String keyword;
}
