package portfoliosim.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Historical daily closing prices for a set of tickers. */
public class MarketData {

    private final TreeMap<String, TreeMap<LocalDate, Double>> closes = new TreeMap<>();
    private final TreeSet<LocalDate> tradingDays = new TreeSet<>();

    public void add(String ticker, LocalDate date, double close) {
        if (!(close > 0)) {
            throw new IllegalArgumentException("Close must be positive: " + close);
        }
        String t = ticker.trim().toUpperCase();
        closes.computeIfAbsent(t, k -> new TreeMap<>()).put(date, close);
        tradingDays.add(date);
    }

    public boolean isEmpty() {
        return closes.isEmpty();
    }

    public Set<String> tickers() {
        return Collections.unmodifiableSet(closes.keySet());
    }

    public boolean hasTicker(String ticker) {
        return closes.containsKey(ticker.trim().toUpperCase());
    }

    /** Close on exactly this date, if the ticker traded that day. */
    public OptionalDouble closeOn(String ticker, LocalDate date) {
        TreeMap<LocalDate, Double> series = closes.get(ticker.trim().toUpperCase());
        if (series == null || !series.containsKey(date)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(series.get(date));
    }

    /** Most recent close on or before the date. */
    public OptionalDouble priceAsOf(String ticker, LocalDate date) {
        TreeMap<LocalDate, Double> series = closes.get(ticker.trim().toUpperCase());
        if (series == null) {
            return OptionalDouble.empty();
        }
        var entry = series.floorEntry(date);
        return entry == null ? OptionalDouble.empty() : OptionalDouble.of(entry.getValue());
    }

    public NavigableSet<LocalDate> tradingDays() {
        return Collections.unmodifiableNavigableSet(tradingDays);
    }

    public Optional<LocalDate> nextTradingDay(LocalDate date) {
        return Optional.ofNullable(tradingDays.higher(date));
    }

    public Optional<LocalDate> firstTradingDay() {
        return tradingDays.isEmpty() ? Optional.empty() : Optional.of(tradingDays.first());
    }

    public Optional<LocalDate> lastTradingDay() {
        return tradingDays.isEmpty() ? Optional.empty() : Optional.of(tradingDays.last());
    }

    public NavigableMap<LocalDate, Double> series(String ticker) {
        TreeMap<LocalDate, Double> series = closes.get(ticker.trim().toUpperCase());
        return series == null ? Collections.emptyNavigableMap() : Collections.unmodifiableNavigableMap(series);
    }
}
