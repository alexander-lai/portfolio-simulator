package portfoliosim.model;

import java.time.LocalDate;

/** Total account value (cash + holdings) at the close of a day. */
public record EquityPoint(LocalDate date, double equity) { }
