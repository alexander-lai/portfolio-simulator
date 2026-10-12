package portfoliosim.model;

import java.time.LocalDate;

/** An executed trade. */
public record Transaction(LocalDate date, String ticker, Side side, int quantity, double price) {

    public double value() {
        return quantity * price;
    }

    /** Change to the cash balance caused by this transaction. */
    public double cashDelta() {
        return side == Side.BUY ? -value() : value();
    }
}
