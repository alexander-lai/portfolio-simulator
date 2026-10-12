package portfoliosim;

import portfoliosim.data.CsvPriceLoader;
import portfoliosim.engine.TradingEngine;
import portfoliosim.model.MarketData;
import portfoliosim.ui.AppPanel;
import portfoliosim.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Entry point.
 * <pre>
 * java -jar portfolio-simulator.jar [--data DIR] [--cash AMOUNT] [--start YYYY-MM-DD] [--benchmark TICKER]
 * </pre>
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        Path dataDir = Path.of("data", "prices");
        double cash = 100_000;
        LocalDate start = null;
        String benchmark = "SPY";

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--data" -> dataDir = Path.of(args[++i]);
                    case "--cash" -> cash = Double.parseDouble(args[++i]);
                    case "--start" -> start = LocalDate.parse(args[++i]);
                    case "--benchmark" -> benchmark = args[++i].toUpperCase();
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Invalid arguments: " + e.getMessage());
            System.err.println("Usage: java -jar portfolio-simulator.jar [--data DIR] [--cash AMOUNT] "
                    + "[--start YYYY-MM-DD] [--benchmark TICKER]");
            System.exit(2);
            return;
        }

        try {
            MarketData market = CsvPriceLoader.loadDirectory(dataDir);
            LocalDate first = start != null ? start : market.firstTradingDay().orElseThrow();
            TradingEngine engine = new TradingEngine(market, cash, first);
            boolean synthetic = Files.exists(dataDir.resolve("SYNTHETIC_DATA.txt"));
            String title = "Portfolio Simulator" + (synthetic ? "  -  SYNTHETIC SAMPLE DATA" : "");
            String bench = benchmark;
            SwingUtilities.invokeLater(() -> new MainFrame(title,
                    new AppPanel(market, engine, bench, Path.of("saves", "portfolio.save"))).setVisible(true));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not start: " + e.getMessage());
            System.exit(1);
        }
    }
}
