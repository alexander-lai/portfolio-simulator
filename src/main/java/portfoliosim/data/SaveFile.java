package portfoliosim.data;

import portfoliosim.engine.TradingEngine;
import portfoliosim.model.MarketData;
import portfoliosim.model.Order;
import portfoliosim.model.OrderType;
import portfoliosim.model.Side;
import portfoliosim.model.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-text save format, one record per line:
 * <pre>
 * START_CASH,100000.0
 * START_DATE,2022-01-03
 * CURRENT_DATE,2022-03-01
 * NEXT_ID,4
 * TXN,2022-01-03,AAPL,BUY,10,129.5
 * ORDER,3,AAPL,LIMIT,BUY,5,120.0
 * </pre>
 * Prices are not saved; they come from the CSV data, and the equity curve is rebuilt on load.
 */
public final class SaveFile {

    private SaveFile() { }

    public static void save(Path path, TradingEngine engine) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("START_CASH," + engine.portfolio().startingCash());
        lines.add("START_DATE," + engine.startDate());
        lines.add("CURRENT_DATE," + engine.currentDate());
        lines.add("NEXT_ID," + engine.nextOrderId());
        for (Transaction t : engine.portfolio().transactions()) {
            lines.add(String.join(",", "TXN", t.date().toString(), t.ticker(), t.side().name(),
                    Integer.toString(t.quantity()), Double.toString(t.price())));
        }
        for (Order o : engine.pendingOrders()) {
            lines.add(String.join(",", "ORDER", Long.toString(o.id()), o.ticker(), o.type().name(),
                    o.side().name(), Integer.toString(o.quantity()), Double.toString(o.price())));
        }
        Files.write(path, lines);
    }

    public static TradingEngine load(Path path, MarketData market) throws IOException {
        double startCash = 0;
        LocalDate startDate = null;
        LocalDate currentDate = null;
        long nextId = 1;
        List<Transaction> txns = new ArrayList<>();
        List<Order> pending = new ArrayList<>();

        List<String> lines = Files.readAllLines(path);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(",");
            try {
                switch (f[0]) {
                    case "START_CASH" -> startCash = Double.parseDouble(f[1]);
                    case "START_DATE" -> startDate = LocalDate.parse(f[1]);
                    case "CURRENT_DATE" -> currentDate = LocalDate.parse(f[1]);
                    case "NEXT_ID" -> nextId = Long.parseLong(f[1]);
                    case "TXN" -> txns.add(new Transaction(LocalDate.parse(f[1]), f[2], Side.valueOf(f[3]),
                            Integer.parseInt(f[4]), Double.parseDouble(f[5])));
                    case "ORDER" -> pending.add(new Order(Long.parseLong(f[1]), f[2], Side.valueOf(f[4]),
                            OrderType.valueOf(f[3]), Integer.parseInt(f[5]), Double.parseDouble(f[6])));
                    default -> throw new IllegalArgumentException("unknown record type " + f[0]);
                }
            } catch (RuntimeException e) {
                throw new IOException(path.getFileName() + " line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        if (startDate == null || currentDate == null || startCash <= 0) {
            throw new IOException(path.getFileName() + ": missing START_CASH, START_DATE or CURRENT_DATE");
        }
        try {
            return TradingEngine.restore(market, startCash, startDate, currentDate, txns, pending, nextId);
        } catch (RuntimeException e) {
            throw new IOException("Could not restore " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
