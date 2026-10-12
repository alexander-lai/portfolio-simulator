package portfoliosim;

import org.junit.jupiter.api.Test;
import portfoliosim.data.CsvPriceLoader;
import portfoliosim.model.MarketData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvPriceLoaderTest {

    @Test
    void loadsYahooStyleAndMinimalFiles() throws IOException {
        Path dir = Files.createTempDirectory("prices");
        Files.write(dir.resolve("aaa.csv"), List.of(
                "Date,Open,High,Low,Close,Adj Close,Volume",
                "2024-01-02,99,101,98,100.50,99.00,1000",
                "2024-01-03,100,103,99,102.25,101.00,1200",
                "2024-01-04,null,null,null,null,null,null"));
        Files.write(dir.resolve("BBB.csv"), List.of("Date,Close", "2024-01-02,50.00", ""));

        MarketData market = CsvPriceLoader.loadDirectory(dir);

        assertTrue(market.hasTicker("AAA"));
        assertTrue(market.hasTicker("BBB"));
        // "Close", not "Adj Close"
        assertEquals(100.50, market.closeOn("AAA", LocalDate.of(2024, 1, 2)).getAsDouble(), 1e-9);
        assertEquals(102.25, market.closeOn("AAA", LocalDate.of(2024, 1, 3)).getAsDouble(), 1e-9);
        // the all-null row is skipped
        assertTrue(market.closeOn("AAA", LocalDate.of(2024, 1, 4)).isEmpty());
        assertEquals(2, market.tradingDays().size());
    }

    @Test
    void reportsAMissingCloseColumn() throws IOException {
        Path dir = Files.createTempDirectory("prices");
        Files.write(dir.resolve("AAA.csv"), List.of("Date,Price", "2024-01-02,100"));
        IOException e = assertThrows(IOException.class, () -> CsvPriceLoader.loadDirectory(dir));
        assertTrue(e.getMessage().contains("Close"));
    }

    @Test
    void reportsTheLineNumberOfABadValue() throws IOException {
        Path dir = Files.createTempDirectory("prices");
        Files.write(dir.resolve("AAA.csv"), List.of("Date,Close", "2024-01-02,100", "2024-01-03,abc"));
        IOException e = assertThrows(IOException.class, () -> CsvPriceLoader.loadDirectory(dir));
        assertTrue(e.getMessage().contains("line 3"));
    }

    @Test
    void failsOnAnEmptyDirectory() throws IOException {
        Path dir = Files.createTempDirectory("prices");
        assertThrows(IOException.class, () -> CsvPriceLoader.loadDirectory(dir));
    }
}
