package portfoliosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import portfoliosim.engine.TradingEngine;
import portfoliosim.model.MarketData;
import portfoliosim.model.Order;
import portfoliosim.model.OrderType;
import portfoliosim.model.Side;
import portfoliosim.model.TradeException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AAA closes: day0 100, day1 102, day2 98, day3 95, day4 105, day5 110. */
class TradingEngineTest {

    private MarketData market;
    private TradingEngine engine;

    @BeforeEach
    void setUp() {
        market = TestData.market();
        engine = new TradingEngine(market, 100_000, TestData.DAY0);
    }

    @Test
    void marketBuyFillsAtTheCurrentClose() {
        engine.submit(Side.BUY, "AAA", OrderType.MARKET, 10, 0);
        assertEquals(10, engine.portfolio().sharesOf("AAA"));
        assertEquals(99_000.0, engine.portfolio().cash(), 1e-9);
        assertEquals(1, engine.equityCurve().size());
    }

    @Test
    void unknownTickerIsRejected() {
        assertThrows(TradeException.class, () -> engine.submit(Side.BUY, "ZZZ", OrderType.MARKET, 1, 0));
    }

    @Test
    void marketBuyWithoutEnoughCashIsRejected() {
        assertThrows(TradeException.class, () -> engine.submit(Side.BUY, "AAA", OrderType.MARKET, 10_000, 0));
        assertEquals(0, engine.portfolio().transactions().size());
    }

    @Test
    void limitBuyWaitsUntilThePriceIsLowEnough() {
        engine.submit(Side.BUY, "AAA", OrderType.LIMIT, 5, 98);
        assertEquals(1, engine.pendingOrders().size());

        engine.advanceOneDay(); // 102: not triggered
        assertEquals(1, engine.pendingOrders().size());
        assertEquals(0, engine.portfolio().sharesOf("AAA"));

        engine.advanceOneDay(); // 98: triggered
        assertEquals(0, engine.pendingOrders().size());
        assertEquals(5, engine.portfolio().sharesOf("AAA"));
    }

    @Test
    void limitBuyFillsAtTheCloseNotTheLimit() {
        engine.submit(Side.BUY, "AAA", OrderType.LIMIT, 5, 99);
        engine.advanceDays(2); // close 98 is below the 99 limit
        assertEquals(98.0, engine.portfolio().transactions().get(0).price(), 1e-9);
    }

    @Test
    void stopSellProtectsAgainstADrop() {
        engine.submit(Side.BUY, "AAA", OrderType.MARKET, 10, 0);
        engine.submit(Side.SELL, "AAA", OrderType.STOP, 10, 96);

        engine.advanceDays(2); // 102, 98: stop not hit
        assertEquals(10, engine.portfolio().sharesOf("AAA"));

        engine.advanceOneDay(); // 95: stop hit, fills at the close
        assertEquals(0, engine.portfolio().sharesOf("AAA"));
        assertEquals(95.0, engine.portfolio().transactions().get(1).price(), 1e-9);
    }

    @Test
    void pendingBuyIsRejectedIfCashRanOut() {
        TradingEngine small = new TradingEngine(market, 1_000, TestData.DAY0);
        small.submit(Side.BUY, "AAA", OrderType.LIMIT, 20, 98); // 20 * 98 = 1960 > 1000
        small.advanceDays(2);

        assertEquals(0, small.pendingOrders().size());
        assertEquals(0, small.portfolio().sharesOf("AAA"));
        assertTrue(small.events().stream().anyMatch(e -> e.contains("rejected")));
    }

    @Test
    void cancelRemovesAPendingOrder() {
        Order o = engine.submit(Side.BUY, "AAA", OrderType.LIMIT, 5, 50);
        assertTrue(engine.cancel(o.id()));
        assertFalse(engine.cancel(o.id()));
        assertEquals(0, engine.pendingOrders().size());
    }

    @Test
    void advancingStopsAtTheEndOfTheData() {
        assertEquals(5, engine.advanceDays(100));
        assertTrue(engine.isAtEnd());
        assertFalse(engine.advanceOneDay());
        assertEquals(LocalDate.of(2024, 1, 7), engine.currentDate());
    }

    @Test
    void equityCurveTracksHoldingsEachDay() {
        engine.submit(Side.BUY, "AAA", OrderType.MARKET, 10, 0);
        engine.advanceOneDay();

        assertEquals(2, engine.equityCurve().size());
        assertEquals(100_000.0, engine.equityCurve().get(0).equity(), 1e-9);
        assertEquals(100_020.0, engine.equityCurve().get(1).equity(), 1e-9);
    }
}
