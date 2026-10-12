package portfoliosim.engine;

import portfoliosim.model.EquityPoint;
import portfoliosim.model.MarketData;
import portfoliosim.model.Order;
import portfoliosim.model.OrderType;
import portfoliosim.model.Portfolio;
import portfoliosim.model.Side;
import portfoliosim.model.TradeException;
import portfoliosim.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;

/**
 * Replays history one trading day at a time. Only closing prices are available, so every
 * order fills at a day's close:
 * <ul>
 *   <li>MARKET orders fill immediately at the current simulation date's close.</li>
 *   <li>LIMIT and STOP orders wait as pending orders and are checked against each later day's close.</li>
 * </ul>
 */
public class TradingEngine {

    private final MarketData market;
    private final Portfolio portfolio;
    private final LocalDate startDate;
    private LocalDate currentDate;
    private long nextOrderId = 1;
    private final List<Order> pending = new ArrayList<>();
    private final List<EquityPoint> equityCurve = new ArrayList<>();
    private final List<String> events = new ArrayList<>();

    public TradingEngine(MarketData market, double startingCash, LocalDate startDate) {
        if (!market.tradingDays().contains(startDate)) {
            throw new IllegalArgumentException("Start date is not a trading day in the data set: " + startDate);
        }
        this.market = market;
        this.portfolio = new Portfolio(startingCash);
        this.startDate = startDate;
        this.currentDate = startDate;
        snapshot();
    }

    /** Submits an order. MARKET orders fill now; others become pending. Price is ignored for MARKET. */
    public Order submit(Side side, String ticker, OrderType type, int quantity, double price) {
        String t = ticker.trim().toUpperCase();
        if (!market.hasTicker(t)) {
            throw new TradeException("Unknown ticker: " + t);
        }
        OptionalDouble px = market.priceAsOf(t, currentDate);
        if (px.isEmpty()) {
            throw new TradeException("No price data for " + t + " on " + currentDate);
        }
        Order order = new Order(nextOrderId, t, side, type, quantity, price);
        if (type == OrderType.MARKET) {
            execute(order, px.getAsDouble());
            nextOrderId++;
            snapshot();
        } else {
            nextOrderId++;
            pending.add(order);
            events.add(String.format("%s: order #%d placed - %s %s %d %s @ %.2f",
                    currentDate, order.id(), type, side, quantity, t, price));
        }
        return order;
    }

    public boolean cancel(long orderId) {
        boolean removed = pending.removeIf(o -> o.id() == orderId);
        if (removed) {
            events.add(currentDate + ": order #" + orderId + " cancelled");
        }
        return removed;
    }

    /** Moves to the next trading day and fills any triggered pending orders. */
    public boolean advanceOneDay() {
        Optional<LocalDate> next = market.nextTradingDay(currentDate);
        if (next.isEmpty()) {
            return false;
        }
        currentDate = next.get();
        Iterator<Order> it = pending.iterator();
        while (it.hasNext()) {
            Order o = it.next();
            OptionalDouble close = market.closeOn(o.ticker(), currentDate);
            if (close.isEmpty() || !o.triggeredBy(close.getAsDouble())) {
                continue;
            }
            it.remove();
            try {
                execute(o, close.getAsDouble());
            } catch (TradeException e) {
                events.add(currentDate + ": order #" + o.id() + " rejected - " + e.getMessage());
            }
        }
        snapshot();
        return true;
    }

    /** Advances up to n trading days; returns how many days were actually advanced. */
    public int advanceDays(int n) {
        int moved = 0;
        while (moved < n && advanceOneDay()) {
            moved++;
        }
        return moved;
    }

    public boolean isAtEnd() {
        return market.nextTradingDay(currentDate).isEmpty();
    }

    private void execute(Order o, double fillPrice) {
        portfolio.apply(new Transaction(currentDate, o.ticker(), o.side(), o.quantity(), fillPrice));
        events.add(String.format("%s: %s order #%d filled - %s %d %s @ %.2f",
                currentDate, o.type(), o.id(), o.side(), o.quantity(), o.ticker(), fillPrice));
    }

    private void snapshot() {
        EquityPoint point = new EquityPoint(currentDate, portfolio.equity(market, currentDate));
        int last = equityCurve.size() - 1;
        if (last >= 0 && equityCurve.get(last).date().equals(currentDate)) {
            equityCurve.set(last, point);
        } else {
            equityCurve.add(point);
        }
    }

    /**
     * Rebuilds an engine from saved state by replaying the saved transactions day by day,
     * so the equity curve is regenerated exactly.
     */
    public static TradingEngine restore(MarketData market, double startingCash, LocalDate startDate,
                                        LocalDate currentDate, List<Transaction> txns,
                                        List<Order> pendingOrders, long nextOrderId) {
        if (!market.tradingDays().contains(currentDate)) {
            throw new IllegalArgumentException("Saved date is not a trading day in the data set: " + currentDate);
        }
        TradingEngine e = new TradingEngine(market, startingCash, startDate);
        Map<LocalDate, List<Transaction>> byDate = new TreeMap<>();
        for (Transaction t : txns) {
            byDate.computeIfAbsent(t.date(), d -> new ArrayList<>()).add(t);
        }
        int applied = 0;
        for (Transaction t : byDate.getOrDefault(startDate, List.of())) {
            e.portfolio.apply(t);
            applied++;
        }
        e.snapshot();
        for (LocalDate d : market.tradingDays().subSet(startDate, false, currentDate, true)) {
            e.currentDate = d;
            for (Transaction t : byDate.getOrDefault(d, List.of())) {
                e.portfolio.apply(t);
                applied++;
            }
            e.snapshot();
        }
        if (applied != txns.size()) {
            throw new IllegalArgumentException("Saved transactions fall outside the simulated date range");
        }
        e.pending.addAll(pendingOrders);
        e.nextOrderId = nextOrderId;
        return e;
    }

    public MarketData market() { return market; }
    public Portfolio portfolio() { return portfolio; }
    public LocalDate startDate() { return startDate; }
    public LocalDate currentDate() { return currentDate; }
    public long nextOrderId() { return nextOrderId; }
    public List<Order> pendingOrders() { return Collections.unmodifiableList(pending); }
    public List<EquityPoint> equityCurve() { return Collections.unmodifiableList(equityCurve); }
    public List<String> events() { return Collections.unmodifiableList(events); }
}
