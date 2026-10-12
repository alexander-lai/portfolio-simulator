package portfoliosim;

import org.junit.jupiter.api.Test;
import portfoliosim.data.SaveFile;
import portfoliosim.engine.TradingEngine;
import portfoliosim.model.MarketData;
import portfoliosim.model.OrderType;
import portfoliosim.model.Side;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaveFileTest {

    @Test
    void roundTripRestoresPortfolioPendingOrdersAndEquityCurve() throws IOException {
        MarketData market = TestData.market();
        TradingEngine original = new TradingEngine(market, 100_000, TestData.DAY0);
        original.submit(Side.BUY, "AAA", OrderType.MARKET, 10, 0);
        original.submit(Side.BUY, "AAA", OrderType.LIMIT, 5, 50);   // never fills
        original.advanceDays(3);
        original.submit(Side.SELL, "AAA", OrderType.MARKET, 4, 0);
        original.advanceOneDay();

        Path file = Files.createTempFile("portfolio", ".save");
        SaveFile.save(file, original);
        TradingEngine loaded = SaveFile.load(file, market);

        assertEquals(original.currentDate(), loaded.currentDate());
        assertEquals(original.portfolio().cash(), loaded.portfolio().cash(), 1e-9);
        assertEquals(original.portfolio().sharesOf("AAA"), loaded.portfolio().sharesOf("AAA"));
        assertEquals(original.portfolio().realizedPnl(), loaded.portfolio().realizedPnl(), 1e-9);
        assertEquals(original.pendingOrders(), loaded.pendingOrders());
        assertEquals(original.nextOrderId(), loaded.nextOrderId());
        assertEquals(original.equityCurve(), loaded.equityCurve());
    }

    @Test
    void corruptFilesAreReportedAsIoErrors() throws IOException {
        Path file = Files.createTempFile("portfolio", ".save");
        Files.write(file, List.of("START_CASH,100000", "BOGUS,1"));
        assertThrows(IOException.class, () -> SaveFile.load(file, TestData.market()));
    }
}
