package com.talent.analysis.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表生成结果：表头 + 明细 + 图表数据 + 溯源信息。
 */
@Data
public class ReportResultVO {

    private String reportName;
    private List<ColumnVO> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private ChartVO chart = new ChartVO();

    private Long reportId;
    private String generatedAt;
    /** 生成这份报表时用的数据版本和模型版本（溯源） */
    private String dataVersion;
    private String modelVersion;

    @Data
    public static class ColumnVO {
        private String key;
        private String label;
        /** dimension / number */
        private String type;

        public ColumnVO(String key, String label, String type) {
            this.key = key;
            this.label = label;
            this.type = type;
        }
    }

    @Data
    public static class ChartVO {
        private List<String> categories = new ArrayList<>();
        private List<SeriesVO> series = new ArrayList<>();
    }

    @Data
    public static class SeriesVO {
        private String name;
        private List<Object> data = new ArrayList<>();

        public SeriesVO(String name) {
            this.name = name;
        }
    }

    public void addColumn(String key, String label, String type) {
        columns.add(new ColumnVO(key, label, type));
    }

    public Map<String, Object> newRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        rows.add(row);
        return row;
    }
}
