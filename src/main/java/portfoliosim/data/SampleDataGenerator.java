package portfoliosim.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Writes SYNTHETIC daily prices (geometric Brownian motion with a shared market factor) so the app
 * runs out of the box. These are NOT real prices. Replace the CSVs in data/prices with real downloads
 * (e.g. from Yahoo Finance) and delete SYNTHETIC_DATA.txt to use real history.
 *
 * <p>Usage: {@code java -cp target/classes portfoliosim.data.SampleDataGenerator [outputDir]}
 */
public final class SampleDataGenerator {

    private record Spec(String ticker, double startPrice, double annualDrift, double annualVol, double marketCorrelation) { }

    private static final List<Spec> SPECS = List.of(
            new Spec("SPY", 450, 0.08, 0.16, 1.00),
            new Spec("AAPL", 130, 0.12, 0.28, 0.80),
            new Spec("MSFT", 280, 0.12, 0.26, 0.80),
            new Spec("GOOGL", 130, 0.10, 0.30, 0.75),
            new Spec("AMZN", 150, 0.10, 0.34, 0.75),
            new Spec("NVDA", 30, 0.30, 0.50, 0.70),
            new Spec("JPM", 150, 0.07, 0.24, 0.70),
            new Spec("V", 220, 0.09, 0.22, 0.75),
            new Spec("JNJ", 160, 0.04, 0.16, 0.40),
            new Spec("WMT", 140, 0.06, 0.18, 0.45),
            new Spec("XOM", 60, 0.10, 0.30, 0.45),
            new Spec("KO", 60, 0.05, 0.16, 0.40),
            new Spec("DIS", 150, 0.02, 0.30, 0.65));

    private SampleDataGenerator() { }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "data/prices");
        generate(out, LocalDate.of(2022, 1, 3), LocalDate.of(2024, 12, 31), 42L);
        System.out.println("Wrote synthetic prices for " + SPECS.size() + " tickers to " + out);
    }

    public static void generate(Path outDir, LocalDate from, LocalDate to, long seed) throws IOException {
        Files.createDirectories(outDir);
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(d);
            }
        }

        Random rng = new Random(seed);
        double[] marketShock = new double[days.size()];
        for (int i = 0; i < marketShock.length; i++) {
            marketShock[i] = rng.nextGaussian();
        }

        double dt = 1.0 / 252;
        for (Spec s : SPECS) {
            Random own = new Random(seed + s.ticker().hashCode());
            List<String> rows = new ArrayList<>();
            rows.add("Date,Close");
            double price = s.startPrice();
            for (int i = 0; i < days.size(); i++) {
                if (i > 0) {
                    double z = s.marketCorrelation() * marketShock[i]
                            + Math.sqrt(1 - s.marketCorrelation() * s.marketCorrelation()) * own.nextGaussian();
                    price *= Math.exp((s.annualDrift() - 0.5 * s.annualVol() * s.annualVol()) * dt
                            + s.annualVol() * Math.sqrt(dt) * z);
                }
                rows.add(days.get(i) + "," + String.format("%.2f", Math.max(price, 1.0)));
            }
            Files.write(outDir.resolve(s.ticker() + ".csv"), rows);
        }
        Files.writeString(outDir.resolve("SYNTHETIC_DATA.txt"),
                "These prices are randomly generated for demonstration. They are not real market data.\n"
                        + "Replace the CSV files with real downloads and delete this file to use real history.\n");
    }
}
