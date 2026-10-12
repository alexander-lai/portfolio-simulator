package portfoliosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import portfoliosim.model.MarketData;
import portfoliosim.model.Portfolio;
import portfoliosim.model.Side;
import portfoliosim.model.TradeException;
import portfoliosim.model.Transaction;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioTest {

    private static final LocalDate D = TestData.DAY0;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(10_000);
    }

    private static Transaction buy(int qty, double price) {
        return new Transaction(D, "AAA", Side.BUY, qty, price);
    }

    private static Transaction sell(int qty, double price) {
        return new Transaction(D, "AAA", Side.SELL, qty, price);
    }

    @Test
    void buyReducesCashAndAddsShares() {
        portfolio.apply(buy(10, 100));
        assertEquals(9_000.0, portfolio.cash(), 1e-9);
        assertEquals(10, portfolio.sharesOf("AAA"));
        assertEquals(1_000.0, portfolio.costBasis("AAA"), 1e-9);
    }

    @Test
    void sellRealizesProfitOnTheSoldSlice() {
        portfolio.apply(buy(10, 100));
        portfolio.apply(sell(4, 120));

        assertEquals(9_480.0, portfolio.cash(), 1e-9);
        assertEquals(6, portfolio.sharesOf("AAA"));
        assertEquals(600.0, portfolio.costBasis("AAA"), 1e-9);
        assertEquals(80.0, portfolio.realizedPnl(), 1e-9);
    }

    @Test
    void costBasisUsesTheAverageOfMultipleBuys() {
        portfolio.apply(buy(10, 100));
        portfolio.apply(buy(10, 120));
        assertEquals(2_200.0, portfolio.costBasis("AAA"), 1e-9);

        portfolio.apply(sell(10, 130));
        assertEquals(200.0, portfolio.realizedPnl(), 1e-9);
        assertEquals(1_100.0, portfolio.costBasis("AAA"), 1e-9);
    }

    @Test
    void sellingEverythingClearsThePosition() {
        portfolio.apply(buy(10, 100));
        portfolio.apply(sell(10, 90));
        assertEquals(0, portfolio.sharesOf("AAA"));
        assertEquals(true, portfolio.holdings().isEmpty());
        assertEquals(-100.0, portfolio.realizedPnl(), 1e-9);
    }

    @Test
    void rejectedTradesLeaveTheStateUntouched() {
        assertThrows(TradeException.class, () -> portfolio.apply(buy(1_000, 100)));
        assertThrows(TradeException.class, () -> portfolio.apply(sell(1, 100)));
        assertEquals(10_000.0, portfolio.cash(), 1e-9);
        assertEquals(0, portfolio.transactions().size());
    }

    @Test
    void equityIsCashPlusHoldingsAtMarketPrice() {
        MarketData market = TestData.market();
        portfolio.apply(buy(10, 100));

        // AAA closes at 102 on the second day
        LocalDate day1 = D.plusDays(1);
        assertEquals(1_020.0, portfolio.marketValue(market, day1), 1e-9);
        assertEquals(10_020.0, portfolio.equity(market, day1), 1e-9);
        assertEquals(20.0, portfolio.unrealizedPnl(market, day1), 1e-9);
    }
}
