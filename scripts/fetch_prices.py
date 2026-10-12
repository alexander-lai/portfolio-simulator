#!/usr/bin/env python3
"""Download real daily prices for the simulator (uses the yfinance package).

Usage:
    pip install yfinance
    python scripts/fetch_prices.py
    python scripts/fetch_prices.py --tickers SPY AAPL MSFT --start 2021-01-04 --end 2025-12-31

Prices are split- and dividend-adjusted closes. The download is all-or-nothing: if any ticker fails,
nothing is written, so you never end up with a mix of real and synthetic files.
"""
import argparse
import sys
from pathlib import Path

DEFAULT_TICKERS = ["SPY", "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "JPM", "V", "JNJ", "WMT", "XOM", "KO", "DIS"]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tickers", nargs="+", default=DEFAULT_TICKERS)
    parser.add_argument("--start", default="2022-01-03")
    parser.add_argument("--end", default="2025-12-31")
    parser.add_argument("--out", default="data/prices")
    args = parser.parse_args()

    try:
        import yfinance as yf
    except ImportError:
        sys.exit("yfinance is not installed. Run: pip install yfinance")

    downloaded = {}
    failed = []
    for ticker in args.tickers:
        ticker = ticker.upper()
        history = yf.Ticker(ticker).history(start=args.start, end=args.end, auto_adjust=True)
        if history is None or history.empty or "Close" not in history:
            failed.append(ticker)
            continue
        rows = ["Date,Close"]
        for date, close in history["Close"].dropna().items():
            rows.append(f"{date.date().isoformat()},{close:.4f}")
        downloaded[ticker] = "\n".join(rows) + "\n"
        print(f"  {ticker}: {len(rows) - 1} days")

    if failed:
        sys.exit(f"No data for: {', '.join(failed)}. Nothing was written. Check the symbols and your connection.")

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    marker = out / "SYNTHETIC_DATA.txt"
    if marker.exists():  # replacing the sample data: remove the old synthetic files
        for old in out.glob("*.csv"):
            if old.stem.upper() not in downloaded:
                old.unlink()
        marker.unlink()
    for ticker, text in downloaded.items():
        (out / f"{ticker}.csv").write_text(text)
    print(f"Wrote real prices for {len(downloaded)} tickers to {out}")


if __name__ == "__main__":
    sys.exit(main())
