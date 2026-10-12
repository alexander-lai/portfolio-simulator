package portfoliosim;

import portfoliosim.model.MarketData;

import java.time.LocalDate;

/** Small hand-made price set used across tests. Six consecutive "trading days" starting 2024-01-02. */
final class TestData {

    static final LocalDate DAY0 = LocalDate.of(2024, 1, 2);

    private static final double[] AAA = {100, 102, 98, 95, 105, 110};
    private static final double[] SPY = {400, 404, 396, 400, 408, 412};

    private TestData() { }

    static MarketData market() {
        MarketData m = new MarketData();
        for (int i = 0; i < AAA.length; i++) {
            m.add("AAA", DAY0.plusDays(i), AAA[i]);
            m.add("SPY", DAY0.plusDays(i), SPY[i]);
        }
        return m;
    }
}
