"""Regenerates app/src/test/resources/golden/*.csv with mpmath (50 digits).

Independent of the app's Kotlin code on purpose: do not port engine code here.
Usage: python3 app/src/test/golden/generate.py   (needs mpmath)
"""
import calendar
import os
import random
from datetime import date
from mpmath import mp, mpf, erfc, sqrt, quad, gamma, pi, inf

mp.dps = 50
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resources", "golden")


def write(name, header, rows):
    with open(os.path.join(OUT, name), "w") as f:
        f.write(header + "\n")
        for r in rows:
            f.write(",".join(x if isinstance(x, str) else repr(float(x)) for x in r) + "\n")
    print(name, len(rows))


def flows(n, pv, pmt, fv, begin):
    c = [mpf(0)] * (n + 1)
    c[0] += pv
    c[n] += fv
    for k in range(n):
        c[k if begin else k + 1] += pmt
    return c


def sign_changes(c):
    s = [1 if x > 0 else -1 for x in c if abs(x) > mpf(10) ** -30]
    return sum(a != b for a, b in zip(s, s[1:]))


def annuity(n, i):
    return mpf(n) if i == 0 else ((1 + i) ** n - 1) / i


def pmt_for(n, i, pv, fv, begin):
    return -(pv * (1 + i) ** n + fv) / (annuity(n, i) * (1 + i if begin else 1))


def tvm():
    rows = []
    amounts = [(14000, 0), (250000, 0), (-1000, 0), (0, 10000), (-5000, 20000),
               (20000, -5000), (100000, -100000), (-13500, 7500)]
    for pyr in (1, 2, 4, 12, 52):
        for n in (1, 2, 5, 12, 36, 60, 120, 180, 360, 480):
            for r in ("-2", "0", "0.5", "1", "3", "5", "7.5", "10", "18", "25", "40"):
                i = mpf(r) / 100 / pyr
                if (1 + i) ** n > 10 ** 6:
                    continue
                for pv, fv in amounts:
                    for begin in (False, True):
                        pmt = pmt_for(n, i, pv, fv, begin)
                        if abs(pmt) < mpf("0.01"):
                            continue
                        h = mpf(10) ** -20
                        dp = (pmt_for(n, i + h, pv, fv, begin) - pmt) / h
                        unique = sign_changes(flows(n, mpf(pv), pmt, mpf(fv), begin)) == 1
                        iyr_ok = unique and dp != 0 and abs(pmt * mpf("1e-13") / dp) * pyr * 100 < mpf("1e-7")
                        # N is undetermined when the balance never moves (e.g. interest only)
                        step = pv * i + pmt * (1 + i if begin else 1)
                        n_ok = abs(step) > abs(pmt) * mpf("1e-6")
                        rows.append((mpf(n), mpf(pyr), mpf(r), mpf(pv), pmt, mpf(fv),
                                     str(begin).lower(), str(iyr_ok).lower(), str(n_ok).lower()))
    write("tvm.csv", "n,pyr,iyr,pv,pmt,fv,begin,iyrSolvable,nSolvable", rows)


def cash_flows():
    """Last flow is chosen so that the IRR is exactly `r` per period; one sign change."""
    rows = []
    shapes = [[(-1000, 1), (100, 3)], [(-11000, 1), (0, 6), (1000, 3)], [(-100, 1)],
              [(-250000, 1), (1500, 120)], [(-5000, 1), (-2000, 2), (1500, 4)],
              [(-1, 1), (mpf("0.05"), 5)], [(-1000000, 1), (1000, 359)]]
    for shape in shapes:
        for r in ("-0.30", "-0.05", "0", "0.002", "0.01", "0.05", "0.1282", "0.35", "1", "2.5"):
            rate, t, acc = mpf(r), 0, mpf(0)
            for a, cnt in shape:
                for _ in range(cnt):
                    acc += mpf(a) / (1 + rate) ** t
                    t += 1
            last = -acc * (1 + rate) ** t
            if last <= 0 or last > 10 ** 9:
                continue
            fl = ";".join("%sx%d" % (repr(float(a)), c) for a, c in shape + [(last, 1)])
            probe = mpf("0.08")
            t, npv = 0, mpf(0)
            for a, cnt in shape + [(last, 1)]:
                for _ in range(cnt):
                    npv += mpf(a) / (1 + probe) ** t
                    t += 1
            rows.append((fl, rate, npv))
    write("cashflow.csv", "flows,irr,npvAt8pct", rows)


def bonds():
    rows = []
    for semi in (True, False):
        f = 2 if semi else 1
        for n in (1, 2, 5, 10, 20, 40, 60):
            for cpn in ("0", "2", "5.5", "8", "12"):
                for y in ("0.1", "1", "4.75", "8", "9", "15"):
                    for call in (100, 104):
                        c, i = mpf(cpn) / f, mpf(y) / 100 / f
                        price = sum(c / (1 + i) ** k for k in range(1, n + 1)) + call / (1 + i) ** n
                        rows.append((mpf(n), mpf(y), mpf(cpn), mpf(call), str(semi).lower(), price))
    write("bond.csv", "n,ytm,cpn,call,semi,price", rows)


def probability():
    rows = [("z", mpf(0), mpf(z) / 4, erfc(-mpf(z) / 4 / sqrt(2)) / 2) for z in range(-24, 25)]
    for df in (1, 2, 3, 8, 19, 26, 30, 100, 1000):
        c = gamma((mpf(df) + 1) / 2) / (sqrt(df * pi) * gamma(mpf(df) / 2))
        for t in ("-6", "-3", "-1.86", "-0.5", "0", "0.5", "1", "2.093", "4"):
            t = mpf(t)
            half = quad(lambda x: c * (1 + x * x / df) ** (-(mpf(df) + 1) / 2), [0, abs(t) / 2, abs(t)]) if t else mpf(0)
            rows.append(("t", mpf(df), t, mpf(1) / 2 + (half if t > 0 else -half)))
    write("lowertail.csv", "dist,df,x,p", rows)


def coupon_date(maturity, months_back):
    """HP manual appendix B: month-end maturities pay at month end, others on the same day if it exists."""
    m = maturity.year * 12 + maturity.month - 1 - months_back
    y, mo = m // 12, m % 12 + 1
    last = calendar.monthrange(y, mo)[1]
    at_end = maturity.day == calendar.monthrange(maturity.year, maturity.month)[1]
    return date(y, mo, last if at_end else min(maturity.day, last))


def days360(a, b):
    d1 = min(a.day, 30)
    d2 = 30 if d1 == 30 and b.day == 31 else b.day
    return (b.year - a.year) * 360 + (b.month - a.month) * 30 + d2 - d1


def dated_bond(settle, maturity, cpn, y, call, semi, actual):
    m = 2 if semi else 1
    n = 0
    while coupon_date(maturity, n * 12 // m) > settle:
        n += 1
    prev, nxt = coupon_date(maturity, n * 12 // m), coupon_date(maturity, (n - 1) * 12 // m)
    e = (nxt - prev).days if actual else 360 // m
    a = (settle - prev).days if actual else days360(prev, settle)
    c, i, w = mpf(cpn) / m, mpf(y) / 100 / m, mpf(e - a) / e
    accrued = c * a / e
    if n == 1:
        dirty = (call + c) / (1 + w * i)
    else:
        dirty = call / (1 + i) ** (n - 1 + w) + sum(c / (1 + i) ** (k - 1 + w) for k in range(1, n + 1))
    return dirty - accrued, accrued


def dated_bonds():
    # Printed results from the manual (chapter 10)
    p, a = dated_bond(date(2010, 4, 28), date(2020, 6, 4), "6.75", "4.75", 100, True, True)
    print("manual 10-2: price %.4f accrued %.4f total %.4f" % (p, a, p + a))
    for call, want in ((104, "5.72"), (100, "5.35")):
        lo, hi = mpf(0), mpf(20)
        for _ in range(120):
            mid = (lo + hi) / 2
            q = dated_bond(date(2012, 4, 15), date(2020, 10, 15), "5.5", mid, call, True, True)[0]
            lo, hi = (mid, hi) if q > 101 else (lo, mid)
        print("manual 10-3/4: call %d yield %.4f (manual %s)" % (call, lo, want))

    r = random.Random(5)
    rows = []
    fixed = [(date(2012, 2, 29), date(2020, 8, 31)), (date(2011, 2, 28), date(2015, 8, 30)),
             (date(2012, 2, 29), date(2015, 8, 29)), (date(2011, 12, 31), date(2021, 6, 30)),
             (date(2013, 9, 30), date(2013, 10, 1)), (date(2012, 4, 15), date(2020, 10, 15)),
             (date(2011, 3, 31), date(2030, 9, 30)), (date(2019, 8, 31), date(2020, 2, 29))]
    for k in range(700):
        if k < len(fixed):
            s, m = fixed[k]
        else:
            s = date.fromordinal(r.randint(date(1970, 1, 1).toordinal(), date(2060, 1, 1).toordinal()))
            m = date.fromordinal(s.toordinal() + r.choice([r.randint(1, 360), r.randint(361, 11000)]))
        for semi, actual in ((True, True), (True, False), (False, True), (False, False)):
            cpn, y, call = r.choice(["0", "2.125", "5.5", "9"]), r.choice(["-0.5", "0", "0.25", "4.75", "11"]), r.choice([100, 104])
            price, accrued = dated_bond(s, m, cpn, y, call, semi, actual)
            iso = lambda d: "%04d-%02d-%02d" % (d.year, d.month, d.day)
            rows.append((iso(s), iso(m), str(semi).lower(), str(actual).lower(), mpf(cpn), mpf(y), mpf(call), price, accrued))
    write("bond_dates.csv", "settle,maturity,semi,actual,cpn,ytm,call,price,accrued", rows)


os.makedirs(OUT, exist_ok=True)
tvm(); cash_flows(); bonds(); dated_bonds(); probability()
