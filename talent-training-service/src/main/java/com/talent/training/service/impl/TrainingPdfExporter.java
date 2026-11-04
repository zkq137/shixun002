package com.talent.training.service.impl;
import com.talent.training.vo.TrainingReportVO;
import org.springframework.stereotype.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 使用系统中文字体绘制分页报告。PDF 内嵌 JPEG，不依赖浏览器打印或外部 PDF 服务。 */
@Component
public class TrainingPdfExporter {
    public byte[] export(TrainingReportVO report) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("智能培训效果分析报告"); lines.add("生成时间：" + report.generatedAt());
        lines.add(report.note()); lines.add("历史培训效果（参与率、通过率单位：%）");
        for (var d : report.dimensions()) {
            lines.add("[" + d.dimension() + "] " + d.name());
            lines.add("记录 " + d.recordCount() + " / 员工 " + d.employeeCount() + " / 参与率 " + value(d.attendRate())
                    + " / 通过率 " + value(d.passRate()) + " / 提升分 " + value(d.improveScore()));
        }
        if (report.simulation() != null) {
            var s = report.simulation(); var a = s.assumptions();
            lines.add("方案模拟（假设计算，非历史事实）");
            lines.add("输入：计划 " + a.learners() + " 人，参与率 " + a.attendRate() + "%，通过率 " + a.passRate() + "%，单人提升 " + a.improveScore());
            lines.add("预计参与 " + s.estimatedAttendees() + " 人，通过 " + s.estimatedPassed() + " 人，总提升 " + s.totalImprovement());
            lines.add("计划人员整体通过率 " + s.overallPassRate() + "%；人数四舍五入，提升分=通过人数×单人提升分。");
        }
        Font font = new Font("Dialog", Font.PLAIN, 22);
        if (!font.canDisplay('培')) {
            for (Font candidate : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
                if (candidate.canDisplay('培') && candidate.canDisplay('训')) { font = candidate.deriveFont(22f); break; }
            }
        }
        if (!font.canDisplay('培')) throw new IOException("PDF 导出需要服务器安装中文字体（例如 Noto Sans CJK）");
        List<String> wrapped = new ArrayList<>();
        BufferedImage probe = new BufferedImage(1190, 1684, BufferedImage.TYPE_INT_RGB);
        Graphics2D measure = probe.createGraphics(); measure.setFont(font);
        for (String line : lines) {
            StringBuilder part = new StringBuilder();
            for (int offset = 0; offset < line.length();) {
                int cp = line.codePointAt(offset); offset += Character.charCount(cp);
                String next = new String(Character.toChars(cp));
                if (measure.getFontMetrics().stringWidth(part + next) > 1030) { wrapped.add(part.toString()); part.setLength(0); }
                part.append(next);
            }
            wrapped.add(part.toString());
        }
        measure.dispose();
        int pages = Math.max(1, (wrapped.size() + 43) / 44);
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pages; p++) kids.append(3 + p * 3).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Count " + pages + " /Kids [" + kids + "] >>"));
        for (int p = 0; p < pages; p++) {
            BufferedImage page = new BufferedImage(1190, 1684, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = page.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, 1190, 1684);
            g.setColor(new Color(32, 60, 87)); g.setFont(font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int i = p * 44; i < Math.min(wrapped.size(), (p + 1) * 44); i++) g.drawString(wrapped.get(i), 80, 90 + (i % 44) * 34);
            g.drawString((p + 1) + " / " + pages, 555, 1610); g.dispose();
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream(); ImageIO.write(page, "jpg", jpeg);
            int imageId = 4 + p * 3, contentId = imageId + 1;
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /XObject << /Img " + imageId + " 0 R >> >> /Contents " + contentId + " 0 R >>"));
            objects.add(stream("/Type /XObject /Subtype /Image /Width 1190 /Height 1684 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode", jpeg.toByteArray()));
            objects.add(stream("", bytes("q 595 0 0 842 0 0 cm /Img Do Q")));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(bytes("%PDF-1.4\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) { offsets.add(out.size()); out.write(bytes((i + 1) + " 0 obj\n")); out.write(objects.get(i)); out.write(bytes("\nendobj\n")); }
        int xref = out.size(); out.write(bytes("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (int offset : offsets) out.write(bytes(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset)));
        out.write(bytes("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"));
        return out.toByteArray();
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }
    private static String value(Object value) { return value == null ? "—" : value.toString(); }
    private static byte[] stream(String dictionary, byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bytes("<< " + dictionary + " /Length " + data.length + " >>\nstream\n")); out.write(data); out.write(bytes("\nendstream")); return out.toByteArray();
    }
}
