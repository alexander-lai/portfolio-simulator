package portfoliosim.ui;

import portfoliosim.model.EquityPoint;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Line chart comparing the portfolio's equity curve with a buy-and-hold benchmark. */
public class EquityChartPanel extends JPanel {

    private static final Color PORTFOLIO_COLOR = new Color(0x1F6FEB);
    private static final Color BENCHMARK_COLOR = new Color(0x8B949E);
    private static final Color GRID_COLOR = new Color(0xE1E4E8);

    private List<EquityPoint> portfolio = Collections.emptyList();
    private List<EquityPoint> benchmark = Collections.emptyList();
    private String benchmarkLabel = "Benchmark";

    public EquityChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(700, 320));
    }

    public void setData(List<EquityPoint> portfolio, List<EquityPoint> benchmark, String benchmarkLabel) {
        this.portfolio = portfolio;
        this.benchmark = benchmark;
        this.benchmarkLabel = benchmarkLabel;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int left = 80, right = 20, top = 30, bottom = 30;
        int w = getWidth() - left - right;
        int h = getHeight() - top - bottom;
        FontMetrics fm = g.getFontMetrics();

        if (portfolio.size() < 2 || w <= 0 || h <= 0) {
            g.setColor(Color.GRAY);
            String msg = "Advance the simulation to see performance";
            g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            g.dispose();
            return;
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (EquityPoint p : portfolio) {
            min = Math.min(min, p.equity());
            max = Math.max(max, p.equity());
        }
        for (EquityPoint p : benchmark) {
            min = Math.min(min, p.equity());
            max = Math.max(max, p.equity());
        }
        double pad = Math.max((max - min) * 0.08, 1);
        min -= pad;
        max += pad;

        LocalDate first = portfolio.get(0).date();
        long span = Math.max(1, portfolio.get(portfolio.size() - 1).date().toEpochDay() - first.toEpochDay());

        // grid + y-axis labels
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            double value = min + (max - min) * i / 4;
            int y = top + h - (int) Math.round(h * i / 4.0);
            g.setColor(GRID_COLOR);
            g.drawLine(left, y, left + w, y);
            g.setColor(Color.DARK_GRAY);
            String label = String.format("$%,.0f", value);
            g.drawString(label, left - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 2);
        }

        // x-axis labels
        g.setColor(Color.DARK_GRAY);
        String start = first.toString();
        String end = portfolio.get(portfolio.size() - 1).date().toString();
        g.drawString(start, left, top + h + 18);
        g.drawString(end, left + w - fm.stringWidth(end), top + h + 18);

        drawSeries(g, benchmark, BENCHMARK_COLOR, first, span, min, max, left, top, w, h);
        drawSeries(g, portfolio, PORTFOLIO_COLOR, first, span, min, max, left, top, w, h);

        // legend
        int lx = left + 10;
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(PORTFOLIO_COLOR);
        g.drawLine(lx, 15, lx + 20, 15);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Portfolio", lx + 26, 19);
        if (!benchmark.isEmpty()) {
            int bx = lx + 26 + fm.stringWidth("Portfolio") + 24;
            g.setColor(BENCHMARK_COLOR);
            g.drawLine(bx, 15, bx + 20, 15);
            g.setColor(Color.DARK_GRAY);
            g.drawString(benchmarkLabel + " (buy & hold)", bx + 26, 19);
        }
        g.dispose();
    }

    private static void drawSeries(Graphics2D g, List<EquityPoint> series, Color color, LocalDate first, long span,
                                   double min, double max, int left, int top, int w, int h) {
        if (series.size() < 2) {
            return;
        }
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < series.size(); i++) {
            EquityPoint p = series.get(i);
            double x = left + w * (p.date().toEpochDay() - first.toEpochDay()) / (double) span;
            double y = top + h - h * (p.equity() - min) / (max - min);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }
}
