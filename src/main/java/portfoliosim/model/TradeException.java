package portfoliosim.model;

/** Thrown when a trade cannot be carried out (not enough cash/shares, unknown ticker, ...). */
public class TradeException extends RuntimeException {
    public TradeException(String message) {
        super(message);
    }
}
