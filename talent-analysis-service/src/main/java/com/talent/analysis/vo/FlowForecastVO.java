package com.talent.analysis.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 未来人才流入、流出和晋升趋势预测。
 */
@Data
public class FlowForecastVO {

    private String runCode;
    private Integer horizonMonths;
    private String modelVersion;
    private String generatedAt;
    private String status;
    private String source;
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private List<String> notes = new ArrayList<>();
    private List<Series> series = new ArrayList<>();

    @Data
    public static class Series {
        private String type;
        private String name;
        private String method;
        private String confidence;
        private Double totalPredicted;
        private List<Point> points = new ArrayList<>();
    }

    @Data
    public static class Point {
        private String month;
        private Double predictedValue;
        private Double lowerBound;
        private Double upperBound;
    }
}
