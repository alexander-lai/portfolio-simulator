package portfoliosim.ui;

import portfoliosim.analytics.PerformanceAnalyzer;
import portfoliosim.data.SaveFile;
import portfoliosim.engine.TradingEngine;
import portfoliosim.model.EquityPoint;
import portfoliosim.model.MarketData;
import portfoliosim.model.Order;
import portfoliosim.model.OrderType;
import portfoliosim.model.Portfolio;
import portfoliosim.model.Side;
import portfoliosim.model.TradeException;
import portfoliosim.model.Transaction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;

/** Main screen: header stats, equity chart, tables, and the trade ticket / time controls. */
public class AppPanel extends JPanel {

    private final MarketData market;
    private final String benchmark;
    private final Path savePath;
    private TradingEngine engine;

    private final JLabel summaryLabel = new JLabel();
    private final JLabel statsLabel = new JLabel();
    private final JLabel quoteLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final EquityChartPanel chart = new EquityChartPanel();

    private final JComboBox<String> tickerBox;
    private final JComboBox<Side> sideBox = new JComboBox<>(Side.values());
    private final JComboBox<OrderType> typeBox = new JComboBox<>(OrderType.values());
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1_000_000, 1));
    private final JTextField priceField = new JTextField();

    private final DefaultTableModel holdingsModel =
            readOnlyModel("Ticker", "Shares", "Avg cost", "Price", "Value", "Unrealized P&L");
    private final DefaultTableModel pendingModel =
            readOnlyModel("ID", "Ticker", "Side", "Type", "Qty", "Price");
    private final DefaultTableModel historyModel =
            readOnlyModel("Date", "Ticker", "Side", "Qty", "Price", "Value");
    private final JTable pendingTable = new JTable(pendingModel);
    private final JTextArea eventLog = new JTextArea();

    public AppPanel(MarketData market, TradingEngine engine, String benchmark, Path savePath) {
        this.market = market;
        this.engine = engine;
        this.benchmark = benchmark;
        this.savePath = savePath;
        this.tickerBox = new JComboBox<>(market.tickers().toArray(new String[0]));

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.add(summaryLabel);
        header.add(statsLabel);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Holdings", new JScrollPane(new JTable(holdingsModel)));
        tabs.addTab("Pending orders", new JScrollPane(pendingTable));
        tabs.addTab("Trade history", new JScrollPane(new JTable(historyModel)));
        eventLog.setEditable(false);
        tabs.addTab("Activity log", new JScrollPane(eventLog));
        tabs.setPreferredSize(new Dimension(700, 200));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, tabs);
        split.setResizeWeight(0.65);
        add(split, BorderLayout.CENTER);
        add(buildControls(), BorderLayout.EAST);

        tickerBox.addActionListener(e -> updateQuote());
        typeBox.addActionListener(e -> updatePriceFieldState());
        updatePriceFieldState();
        refresh();
    }

    // ---------------------------------------------------------------- layout

    private JPanel buildControls() {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setPreferredSize(new Dimension(310, 0));

        JPanel form = new JPanel(new GridLayout(0, 2, 4, 6));
        form.add(new JLabel("Ticker"));
        form.add(tickerBox);
        form.add(new JLabel("Side"));
        form.add(sideBox);
        form.add(new JLabel("Type"));
        form.add(typeBox);
        form.add(new JLabel("Quantity"));
        form.add(quantitySpinner);
        form.add(new JLabel("Limit/stop $"));
        form.add(priceField);

        JButton submit = new JButton("Submit order");
        submit.addActionListener(e -> submitOrder());

        JPanel ticket = new JPanel(new BorderLayout(0, 8));
        ticket.setBorder(BorderFactory.createTitledBorder("Trade ticket"));
        ticket.add(form, BorderLayout.NORTH);
        JPanel ticketBottom = new JPanel(new GridLayout(2, 1, 0, 4));
        ticketBottom.add(quoteLabel);
        ticketBottom.add(submit);
        ticket.add(ticketBottom, BorderLayout.SOUTH);

        JPanel time = new JPanel(new GridLayout(1, 3, 4, 0));
        time.setBorder(BorderFactory.createTitledBorder("Advance time"));
        time.add(timeButton("1 day", 1));
        time.add(timeButton("1 week", 5));
        time.add(timeButton("1 month", 21));

        JPanel orders = new JPanel(new GridLayout(1, 1));
        orders.setBorder(BorderFactory.createTitledBorder("Pending orders"));
        JButton cancel = new JButton("Cancel selected order");
        cancel.addActionListener(e -> cancelSelected());
        orders.add(cancel);

        JPanel file = new JPanel(new GridLayout(1, 2, 4, 0));
        file.setBorder(BorderFactory.createTitledBorder("Session"));
        JButton save = new JButton("Save");
        save.addActionListener(e -> save());
        JButton load = new JButton("Load");
        load.addActionListener(e -> load());
        file.add(save);
        file.add(load);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 8, 8, 0);
        c.gridy = 0;
        controls.add(ticket, c);
        c.gridy = 1;
        controls.add(time, c);
        c.gridy = 2;
        controls.add(orders, c);
        c.gridy = 3;
        controls.add(file, c);
        c.gridy = 4;
        controls.add(messageLabel, c);
        c.gridy = 5;
        c.weighty = 1;
        controls.add(new JPanel(), c);
        return controls;
    }

    private JButton timeButton(String text, int days) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            int moved = engine.advanceDays(days);
            if (moved == 0) {
                message("Reached the end of the price data.", true);
            } else if (moved < days) {
                message("Reached the end of the price data after " + moved + " day(s).", false);
            } else {
                message(" ", false);
            }
            refresh();
        });
        return b;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // ---------------------------------------------------------------- actions

    private void submitOrder() {
        OrderType type = (OrderType) typeBox.getSelectedItem();
        double price = 0;
        if (type != OrderType.MARKET) {
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                message("Enter a numeric limit/stop price.", true);
                return;
            }
        }
        try {
            Order order = engine.submit((Side) sideBox.getSelectedItem(), (String) tickerBox.getSelectedItem(),
                    type, (Integer) quantitySpinner.getValue(), price);
            message(type == OrderType.MARKET ? "Order #" + order.id() + " filled." : "Order #" + order.id() + " placed.",
                    false);
        } catch (TradeException | IllegalArgumentException ex) {
            message(ex.getMessage(), true);
        }
        refresh();
    }

    private void cancelSelected() {
        int row = pendingTable.getSelectedRow();
        if (row < 0) {
            message("Select a pending order first.", true);
            return;
        }
        long id = Long.parseLong(pendingModel.getValueAt(row, 0).toString());
        message(engine.cancel(id) ? "Order #" + id + " cancelled." : "Order not found.", false);
        refresh();
    }

    private void save() {
        try {
            if (savePath.getParent() != null) {
                Files.createDirectories(savePath.getParent());
            }
            SaveFile.save(savePath, engine);
            message("Saved to " + savePath, false);
        } catch (IOException ex) {
            message("Save failed: " + ex.getMessage(), true);
        }
    }

    private void load() {
        try {
            engine = SaveFile.load(savePath, market);
            message("Loaded " + savePath, false);
            refresh();
        } catch (IOException ex) {
            message("Load failed: " + ex.getMessage(), true);
        }
    }

    private void message(String text, boolean error) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;");
        messageLabel.setText("<html><body style='width:260px'>" + safe + "</body></html>");
        messageLabel.setForeground(error ? new Color(0xB42318) : new Color(0x1A7F37));
    }

    private void updatePriceFieldState() {
        boolean needsPrice = typeBox.getSelectedItem() != OrderType.MARKET;
        priceField.setEnabled(needsPrice);
        if (!needsPrice) {
            priceField.setText("");
        }
    }

    private void updateQuote() {
        String ticker = (String) tickerBox.getSelectedItem();
        if (ticker == null) {
            return;
        }
        OptionalDouble px = market.priceAsOf(ticker, engine.currentDate());
        quoteLabel.setText(px.isPresent()
                ? String.format("%s close on %s: $%,.2f", ticker, engine.currentDate(), px.getAsDouble())
                : ticker + ": no data yet");
    }

    // ---------------------------------------------------------------- rendering

    void refresh() {
        Portfolio p = engine.portfolio();
        LocalDate date = engine.currentDate();
        List<EquityPoint> curve = engine.equityCurve();
        List<EquityPoint> bench = PerformanceAnalyzer.benchmarkCurve(market, benchmark, p.startingCash(), curve);

        summaryLabel.setText(String.format("%s   |   Cash %s   |   Equity %s   |   Realized P&L %s",
                date, money(p.cash()), money(p.equity(market, date)), money(p.realizedPnl())));

        PerformanceAnalyzer.Summary s = PerformanceAnalyzer.summarize(curve);
        StringBuilder stats = new StringBuilder(String.format("Return %+.2f%%", s.totalReturn() * 100));
        if (!bench.isEmpty()) {
            stats.append(String.format("   |   %s %+.2f%%", benchmark, PerformanceAnalyzer.totalReturn(bench) * 100));
        }
        stats.append(String.format("   |   Max drawdown %.2f%%   |   Volatility %.1f%%   |   Sharpe %.2f",
                s.maxDrawdown() * 100, s.annualizedVolatility() * 100, s.sharpeRatio()));
        statsLabel.setText(stats.toString());

        chart.setData(curve, bench, benchmark);

        holdingsModel.setRowCount(0);
        for (var holding : p.holdings().entrySet()) {
            String ticker = holding.getKey();
            int shares = holding.getValue();
            double price = market.priceAsOf(ticker, date).orElse(0);
            double basis = p.costBasis(ticker);
            holdingsModel.addRow(new Object[]{ticker, shares, money(basis / shares), money(price),
                    money(shares * price), money(shares * price - basis)});
        }

        pendingModel.setRowCount(0);
        for (Order o : engine.pendingOrders()) {
            pendingModel.addRow(new Object[]{o.id(), o.ticker(), o.side(), o.type(), o.quantity(), money(o.price())});
        }

        historyModel.setRowCount(0);
        List<Transaction> txns = p.transactions();
        for (int i = txns.size() - 1; i >= 0; i--) {
            Transaction t = txns.get(i);
            historyModel.addRow(new Object[]{t.date(), t.ticker(), t.side(), t.quantity(), money(t.price()),
                    money(t.value())});
        }

        List<String> events = engine.events();
        eventLog.setText(String.join("\n", events.subList(Math.max(0, events.size() - 200), events.size())));
        eventLog.setCaretPosition(eventLog.getDocument().getLength());

        updateQuote();
    }

    private static String money(double v) {
        return String.format("$%,.2f", v);
    }
}
