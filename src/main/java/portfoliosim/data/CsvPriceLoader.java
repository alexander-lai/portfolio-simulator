package portfoliosim.data;

import portfoliosim.model.MarketData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads one CSV file per ticker from a directory (file name = ticker, e.g. {@code AAPL.csv}).
 * Only the {@code Date} and {@code Close} columns are used, so files downloaded from Yahoo Finance
 * ({@code Date,Open,High,Low,Close,Adj Close,Volume}) work as-is, as do minimal {@code Date,Close} files.
 */
public final class CsvPriceLoader {

    private CsvPriceLoader() { }

    public static MarketData loadDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IOException("Price directory not found: " + dir);
        }
        MarketData market = new MarketData();
        List<Path> files;
        try (Stream<Path> listing = Files.list(dir)) {
            files = listing.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .sorted()
                    .toList();
        }
        for (Path file : files) {
            loadFile(file, market);
        }
        if (market.isEmpty()) {
            throw new IOException("No .csv price files found in " + dir);
        }
        return market;
    }

    public static void loadFile(Path file, MarketData market) throws IOException {
        String name = file.getFileName().toString();
        String ticker = name.substring(0, name.lastIndexOf('.')).toUpperCase();
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) {
            throw new IOException(name + ": file is empty");
        }

        String[] header = lines.get(0).replace("\uFEFF", "").split(",");
        int dateCol = -1;
        int closeCol = -1;
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().toLowerCase();
            if (h.equals("date")) {
                dateCol = i;
            } else if (h.equals("close")) {
                closeCol = i;
            }
        }
        if (dateCol < 0 || closeCol < 0) {
            throw new IOException(name + ": header must contain Date and Close columns");
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split(",");
            try {
                String rawClose = fields[closeCol].trim();
                if (rawClose.equalsIgnoreCase("null") || rawClose.isEmpty()) {
                    continue; // Yahoo exports occasionally contain empty rows
                }
                market.add(ticker, LocalDate.parse(fields[dateCol].trim()), Double.parseDouble(rawClose));
            } catch (RuntimeException e) {
                throw new IOException(name + " line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }
}
