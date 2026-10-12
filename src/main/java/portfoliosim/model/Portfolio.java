package portfoliosim.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cash and share positions, built up from executed transactions. Cost basis uses the
 * average-cost method: selling shares removes a proportional slice of the basis.
 */
public class Portfolio {

    private final double startingCash;
    private double cash;
    private double realizedPnl;
    private final Map<String, Integer> shares = new TreeMap<>();
    private final Map<String, Double> costBasis = new TreeMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public Portfolio(double startingCash) {
        if (!(startingCash > 0)) {
            throw new IllegalArgumentException("Starting cash must be positive");
        }
        this.startingCash = startingCash;
        this.cash = startingCash;
    }

    /** Applies a transaction, or throws {@link TradeException} and leaves the portfolio untouched. */
    public void apply(Transaction t) {
        int held = shares.getOrDefault(t.ticker(), 0);
        if (t.side() == Side.BUY) {
            if (t.value() > cash + 1e-9) {
                throw new TradeException(String.format(
                        "Insufficient cash: need $%,.2f but have $%,.2f", t.value(), cash));
            }
            cash -= t.value();
            shares.merge(t.ticker(), t.quantity(), Integer::sum);
            costBasis.merge(t.ticker(), t.value(), Double::sum);
        } else {
            if (t.quantity() > held) {
                throw new TradeException(String.format(
                        "Insufficient shares: tried to sell %d %s but hold %d", t.quantity(), t.ticker(), held));
            }
            double basis = costBasis.getOrDefault(t.ticker(), 0.0);
            double soldBasis = basis * t.quantity() / held;
            realizedPnl += t.value() - soldBasis;
            cash += t.value();
            int remaining = held - t.quantity();
            if (remaining == 0) {
                shares.remove(t.ticker());
                costBasis.remove(t.ticker());
            } else {
                shares.put(t.ticker(), remaining);
                costBasis.put(t.ticker(), basis - soldBasis);
            }
        }
        transactions.add(t);
    }

    public double startingCash() { return startingCash; }
    public double cash() { return cash; }
    public double realizedPnl() { return realizedPnl; }
    public int sharesOf(String ticker) { return shares.getOrDefault(ticker.toUpperCase(), 0); }
    public double costBasis(String ticker) { return costBasis.getOrDefault(ticker.toUpperCase(), 0.0); }
    public Map<String, Integer> holdings() { return Collections.unmodifiableMap(shares); }
    public List<Transaction> transactions() { return Collections.unmodifiableList(transactions); }

    public double marketValue(MarketData market, LocalDate date) {
        double total = 0;
        for (Map.Entry<String, Integer> e : shares.entrySet()) {
            total += e.getValue() * market.priceAsOf(e.getKey(), date).orElse(0);
        }
        return total;
    }

    public double equity(MarketData market, LocalDate date) {
        return cash + marketValue(market, date);
    }

    public double unrealizedPnl(MarketData market, LocalDate date) {
        double basis = costBasis.values().stream().mapToDouble(Double::doubleValue).sum();
        return marketValue(market, date) - basis;
    }
}
