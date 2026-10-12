package portfoliosim.model;

/**
 * A trade instruction. For LIMIT and STOP orders {@code price} is the limit/stop price;
 * for MARKET orders it is ignored.
 */
public record Order(long id, String ticker, Side side, OrderType type, int quantity, double price) {

    public Order {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (type != OrderType.MARKET && !(price > 0)) {
            throw new IllegalArgumentException("Limit and stop orders need a positive price");
        }
        ticker = ticker.trim().toUpperCase();
    }

    /**
     * Whether a pending order should fill given a day's closing price.
     * <ul>
     *   <li>LIMIT buy fills at or below the limit; LIMIT sell at or above it.</li>
     *   <li>STOP sell (stop-loss) fills at or below the stop; STOP buy at or above it.</li>
     * </ul>
     */
    public boolean triggeredBy(double close) {
        return switch (type) {
            case MARKET -> true;
            case LIMIT -> side == Side.BUY ? close <= price : close >= price;
            case STOP -> side == Side.BUY ? close >= price : close <= price;
        };
    }
}
