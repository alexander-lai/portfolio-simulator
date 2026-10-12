package portfoliosim;

import org.junit.jupiter.api.Test;
import portfoliosim.analytics.PerformanceAnalyzer;
import portfoliosim.model.EquityPoint;
import portfoliosim.model.MarketData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceAnalyzerTest {

    private static List<EquityPoint> curve(double... values) {
        List<EquityPoint> out = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            out.add(new EquityPoint(TestData.DAY0.plusDays(i), values[i]));
        }
        return out;
    }

    @Test
    void totalReturnIsLastOverFirstMinusOne() {
        assertEquals(0.10, PerformanceAnalyzer.totalReturn(curve(100, 105, 110)), 1e-9);
        assertEquals(0.0, PerformanceAnalyzer.totalReturn(curve(100)), 1e-9);
    }

    @Test
    void maxDrawdownIsTheWorstPeakToTroughDrop() {
        // 120 -> 90 is a 25% drop; the later 130 -> 117 is only 10%
        assertEquals(0.25, PerformanceAnalyzer.maxDrawdown(curve(100, 120, 90, 130, 117)), 1e-9);
        assertEquals(0.0, PerformanceAnalyzer.maxDrawdown(curve(100, 101, 102)), 1e-9);
    }

    @Test
    void flatCurveHasZeroVolatilityAndSharpe() {
        List<EquityPoint> flat = curve(100, 100, 100, 100);
        assertEquals(0.0, PerformanceAnalyzer.annualizedVolatility(flat), 1e-9);
        assertEquals(0.0, PerformanceAnalyzer.sharpeRatio(flat), 1e-9);
    }

    @Test
    void volatilityMatchesAHandCalculation() {
        // daily returns +10% and -10% -> mean 0, sample std dev sqrt(0.02) ~ 0.141421
        List<EquityPoint> c = curve(100, 110, 99);
        double[] r = PerformanceAnalyzer.dailyReturns(c);
        assertEquals(0.10, r[0], 1e-9);
        assertEquals(-0.10, r[1], 1e-9);
        assertEquals(Math.sqrt(0.02) * Math.sqrt(252), PerformanceAnalyzer.annualizedVolatility(c), 1e-9);
    }

    @Test
    void sharpeIsPositiveWhenReturnsAreOnAverageGains() {
        assertTrue(PerformanceAnalyzer.sharpeRatio(curve(100, 102, 101, 103, 105)) > 0);
        assertTrue(PerformanceAnalyzer.sharpeRatio(curve(100, 98, 99, 97, 95)) < 0);
    }

    @Test
    void benchmarkCurveScalesStartingCashByThePriceRatio() {
        MarketData market = TestData.market();
        List<EquityPoint> reference = curve(100_000, 100_000, 100_000);
        List<EquityPoint> bench = PerformanceAnalyzer.benchmarkCurve(market, "SPY", 100_000, reference);

        assertEquals(3, bench.size());
        assertEquals(100_000.0, bench.get(0).equity(), 1e-9);
        assertEquals(101_000.0, bench.get(1).equity(), 1e-9);  // 404 / 400
        assertEquals(99_000.0, bench.get(2).equity(), 1e-9);   // 396 / 400
        assertTrue(PerformanceAnalyzer.benchmarkCurve(market, "NOPE", 100_000, reference).isEmpty());
    }
}
