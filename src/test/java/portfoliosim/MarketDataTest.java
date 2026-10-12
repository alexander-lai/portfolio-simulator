package portfoliosim;

import org.junit.jupiter.api.Test;
import portfoliosim.model.MarketData;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataTest {

    @Test
    void closeOnRequiresExactDateButPriceAsOfFallsBack() {
        MarketData m = new MarketData();
        m.add("AAA", LocalDate.of(2024, 1, 2), 100);
        m.add("AAA", LocalDate.of(2024, 1, 5), 105);

        assertTrue(m.closeOn("AAA", LocalDate.of(2024, 1, 3)).isEmpty());
        assertEquals(100.0, m.priceAsOf("AAA", LocalDate.of(2024, 1, 4)).getAsDouble(), 1e-9);
        assertTrue(m.priceAsOf("AAA", LocalDate.of(2024, 1, 1)).isEmpty());
    }

    @Test
    void nextTradingDaySkipsToTheFollowingDataPoint() {
        MarketData m = new MarketData();
        m.add("AAA", LocalDate.of(2024, 1, 2), 100);
        m.add("AAA", LocalDate.of(2024, 1, 5), 105);

        assertEquals(LocalDate.of(2024, 1, 5), m.nextTradingDay(LocalDate.of(2024, 1, 2)).get());
        assertTrue(m.nextTradingDay(LocalDate.of(2024, 1, 5)).isEmpty());
    }

    @Test
    void tickersAreCaseInsensitiveAndPricesMustBePositive() {
        MarketData m = new MarketData();
        m.add("aaa", LocalDate.of(2024, 1, 2), 100);
        assertTrue(m.hasTicker("AAA"));
        assertFalse(m.hasTicker("BBB"));
        assertThrows(IllegalArgumentException.class, () -> m.add("AAA", LocalDate.of(2024, 1, 3), 0));
    }
}
