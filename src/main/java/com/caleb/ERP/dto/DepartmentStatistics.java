package com.caleb.ERP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentStatistics {
    private String departmentName;
    private long employeeCount;
}