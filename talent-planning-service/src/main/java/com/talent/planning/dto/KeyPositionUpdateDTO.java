package com.talent.planning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KeyPositionUpdateDTO {
    @NotNull(message = "关键岗位标识不能为空")
    private Boolean key;
}
