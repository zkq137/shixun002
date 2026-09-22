package com.talent.analysis.mapper;

import com.talent.analysis.vo.ReportVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 报表与报表模板的存取。
 */
@Mapper
public interface ReportMapper {

    @Insert("""
            insert into report (template_id, report_name, dimensions, metrics, filters,
                                report_data, chart_data, row_count, model_version, data_version, generated_by)
            values (#{templateId}, #{reportName}, #{dimensions}, #{metrics}, #{filters},
                    #{reportData}, #{chartData}, #{rowCount}, #{modelVersion}, #{dataVersion}, #{generatedBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(ReportVO report);

    @Select("""
            select id, template_id, report_name, dimensions, metrics, filters, report_data,
                   chart_data, row_count, model_version, data_version, generated_by,
                   date_format(created_at, '%Y-%m-%d %H:%i') as created_at
            from report
            order by created_at desc, id desc
            limit 100
            """)
    List<ReportVO> selectReports();

    @Select("""
            select id, template_id, report_name, dimensions, metrics, filters, report_data,
                   chart_data, row_count, model_version, data_version, generated_by,
                   date_format(created_at, '%Y-%m-%d %H:%i') as created_at
            from report where id = #{id}
            """)
    ReportVO selectReportById(@Param("id") Long id);

    @Delete("delete from report where id = #{id}")
    int deleteReport(@Param("id") Long id);

    @Select("""
            select id, template_name as report_name, dimensions, content,
                   date_format(created_at, '%Y-%m-%d %H:%i') as created_at
            from report_template
            order by id
            """)
    List<ReportVO> selectTemplates();

    @Select("""
            select id, template_name as report_name, dimensions, content,
                   date_format(created_at, '%Y-%m-%d %H:%i') as created_at
            from report_template where id = #{id}
            """)
    ReportVO selectTemplateById(@Param("id") Long id);

    @Insert("""
            insert into report_template (template_name, dimensions, content)
            values (#{reportName}, #{dimensions}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTemplate(ReportVO template);

    @Delete("delete from report_template where id = #{id}")
    int deleteTemplate(@Param("id") Long id);
}
