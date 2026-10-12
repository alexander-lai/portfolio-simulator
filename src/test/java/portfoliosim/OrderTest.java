package portfoliosim;

import org.junit.jupiter.api.Test;
import portfoliosim.model.Order;
import portfoliosim.model.OrderType;
import portfoliosim.model.Side;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void limitBuyTriggersAtOrBelowLimit() {
        Order o = new Order(1, "aaa", Side.BUY, OrderType.LIMIT, 5, 100);
        assertTrue(o.triggeredBy(100));
        assertTrue(o.triggeredBy(95));
        assertFalse(o.triggeredBy(100.01));
    }

    @Test
    void limitSellTriggersAtOrAboveLimit() {
        Order o = new Order(1, "AAA", Side.SELL, OrderType.LIMIT, 5, 100);
        assertTrue(o.triggeredBy(100));
        assertTrue(o.triggeredBy(120));
        assertFalse(o.triggeredBy(99.99));
    }

    @Test
    void stopSellTriggersAtOrBelowStop() {
        Order o = new Order(1, "AAA", Side.SELL, OrderType.STOP, 5, 90);
        assertTrue(o.triggeredBy(90));
        assertTrue(o.triggeredBy(80));
        assertFalse(o.triggeredBy(90.5));
    }

    @Test
    void stopBuyTriggersAtOrAboveStop() {
        Order o = new Order(1, "AAA", Side.BUY, OrderType.STOP, 5, 110);
        assertTrue(o.triggeredBy(110));
        assertFalse(o.triggeredBy(109));
    }

    @Test
    void tickerIsNormalizedToUpperCase() {
        assertEquals("AAA", new Order(1, " aaa ", Side.BUY, OrderType.MARKET, 1, 0).ticker());
    }

    @Test
    void rejectsNonPositiveQuantityAndMissingPrice() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, "AAA", Side.BUY, OrderType.MARKET, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Order(1, "AAA", Side.BUY, OrderType.LIMIT, 1, 0));
    }
}
