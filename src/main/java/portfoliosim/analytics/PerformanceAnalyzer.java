package portfoliosim.analytics;

import portfoliosim.model.EquityPoint;
import portfoliosim.model.MarketData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

/** Performance statistics computed from an equity curve of daily closes. */
public final class PerformanceAnalyzer {

    public static final int TRADING_DAYS_PER_YEAR = 252;

    private PerformanceAnalyzer() { }

    public record Summary(double totalReturn, double maxDrawdown, double annualizedVolatility, double sharpeRatio) { }

    public static Summary summarize(List<EquityPoint> curve) {
        return new Summary(totalReturn(curve), maxDrawdown(curve), annualizedVolatility(curve), sharpeRatio(curve));
    }

    /** (last / first) - 1, e.g. 0.10 for +10%. */
    public static double totalReturn(List<EquityPoint> curve) {
        if (curve.size() < 2 || curve.get(0).equity() == 0) {
            return 0;
        }
        return curve.get(curve.size() - 1).equity() / curve.get(0).equity() - 1;
    }

    /** Largest peak-to-trough decline as a positive fraction (0.25 = a 25% drop). */
    public static double maxDrawdown(List<EquityPoint> curve) {
        double peak = Double.NEGATIVE_INFINITY;
        double worst = 0;
        for (EquityPoint p : curve) {
            peak = Math.max(peak, p.equity());
            if (peak > 0) {
                worst = Math.max(worst, (peak - p.equity()) / peak);
            }
        }
        return worst;
    }

    public static double[] dailyReturns(List<EquityPoint> curve) {
        if (curve.size() < 2) {
            return new double[0];
        }
        double[] r = new double[curve.size() - 1];
        for (int i = 1; i < curve.size(); i++) {
            double prev = curve.get(i - 1).equity();
            r[i - 1] = prev == 0 ? 0 : curve.get(i).equity() / prev - 1;
        }
        return r;
    }

    public static double annualizedVolatility(List<EquityPoint> curve) {
        double[] r = dailyReturns(curve);
        return stdDev(r) * Math.sqrt(TRADING_DAYS_PER_YEAR);
    }

    /** Annualized Sharpe ratio assuming a 0% risk-free rate. Returns 0 when volatility is zero. */
    public static double sharpeRatio(List<EquityPoint> curve) {
        double[] r = dailyReturns(curve);
        double sd = stdDev(r);
        if (r.length < 2 || sd < 1e-12) {
            return 0;
        }
        return mean(r) / sd * Math.sqrt(TRADING_DAYS_PER_YEAR);
    }

    /**
     * What the starting cash would be worth if fully invested in {@code ticker} on the first date of
     * {@code reference} and held. Empty if the ticker has no data.
     */
    public static List<EquityPoint> benchmarkCurve(MarketData market, String ticker, double startingCash,
                                                   List<EquityPoint> reference) {
        if (reference.isEmpty() || !market.hasTicker(ticker)) {
            return Collections.emptyList();
        }
        OptionalDouble base = market.priceAsOf(ticker, reference.get(0).date());
        if (base.isEmpty()) {
            return Collections.emptyList();
        }
        List<EquityPoint> out = new ArrayList<>();
        for (EquityPoint p : reference) {
            double px = market.priceAsOf(ticker, p.date()).orElse(base.getAsDouble());
            out.add(new EquityPoint(p.date(), startingCash * px / base.getAsDouble()));
        }
        return out;
    }

    private static double mean(double[] x) {
        double sum = 0;
        for (double v : x) {
            sum += v;
        }
        return x.length == 0 ? 0 : sum / x.length;
    }

    /** Sample standard deviation (n - 1). */
    private static double stdDev(double[] x) {
        if (x.length < 2) {
            return 0;
        }
        double m = mean(x);
        double ss = 0;
        for (double v : x) {
            ss += (v - m) * (v - m);
        }
        return Math.sqrt(ss / (x.length - 1));
    }
}
