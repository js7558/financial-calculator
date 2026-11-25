package com.js10bii.calculator.engine

private fun key(
    row: Int, col: Int,
    p: String, o: String, b: String,
    pa: KeyAction, oa: KeyAction, ba: KeyAction,
) = KeyDefinition(row, col, p, o, b, pa, oa, ba)

val KEY_ROWS: List<List<KeyDefinition>> = listOf(
    // Row 0: TVM
    listOf(
        key(0, 0, "N",     "×P/YR",  "AccInt",   KeyAction.TVM_N,      KeyAction.TVM_XPYR,      KeyAction.BOND_ACCINT),
        key(0, 1, "I/YR",  "NOM%",   "YTM",      KeyAction.TVM_IYR,    KeyAction.TVM_NOM_PCT,   KeyAction.BOND_YTM),
        key(0, 2, "PV",    "EFF%",   "PRICE",    KeyAction.TVM_PV,     KeyAction.TVM_EFF_PCT,   KeyAction.BOND_PRICE),
        key(0, 3, "PMT",   "P/YR",   "CPN%",     KeyAction.TVM_PMT,    KeyAction.TVM_PYR,       KeyAction.BOND_CPN),
        key(0, 4, "FV",    "AMORT",  "CALL",     KeyAction.TVM_FV,     KeyAction.TVM_AMORT,     KeyAction.BOND_CALL),
    ),
    // Row 1: Input / business keys
    listOf(
        key(1, 0, "INPUT", "DATE",   "D.MY/M.DY",KeyAction.INPUT,      KeyAction.DATE_CALC,     KeyAction.DATE_DMY),
        key(1, 1, "MU",    "ΔDAYS",  "360/Act",  KeyAction.MU,         KeyAction.DATE_DELTA_DAYS,KeyAction.DATE_360_ACT),
        key(1, 2, "CST",   "IRR/YR", "Semi/Ann", KeyAction.CST,        KeyAction.IRR_YR,        KeyAction.DATE_SEMI_ANN),
        key(1, 3, "PRC",   "NPV",    "SetDate",  KeyAction.PRC,        KeyAction.NPV_CALC,      KeyAction.DATE_SET),
        key(1, 4, "MAR",   "Beg/End","MatDate",  KeyAction.MAR,        KeyAction.TVM_BEG_END,   KeyAction.DATE_MAT),
    ),
    // Row 2: K, %, CFj, Σ+, ←
    listOf(
        key(2, 0, "K",     "SWAP",   "UNITS",    KeyAction.K_CONST,    KeyAction.SWAP,          KeyAction.BREAKEVEN_UNITS),
        key(2, 1, "%",     "%CHG",   "SP",       KeyAction.PCT,        KeyAction.PCT_CHG,       KeyAction.BREAKEVEN_SP),
        key(2, 2, "CFj",   "Nj",     "VC",       KeyAction.CFJ,        KeyAction.NJ,            KeyAction.BREAKEVEN_VC),
        key(2, 3, "Σ+",    "Σ-",     "FC",       KeyAction.STAT_PLUS,  KeyAction.STAT_MINUS,    KeyAction.BREAKEVEN_FC),
        key(2, 4, "←",     "RND",    "PROFIT",   KeyAction.BACKSPACE,  KeyAction.RND,           KeyAction.BREAKEVEN_PROFIT),
    ),
    // Row 3: +/-, RCL, →M, RM, M+
    listOf(
        key(3, 0, "+/-",   "E",      "SL",       KeyAction.SIGN_CHANGE,KeyAction.SCI_NOTATION,  KeyAction.DEPR_SL),
        key(3, 1, "RCL",   "STO",    "SOYD",     KeyAction.RCL,        KeyAction.STO,           KeyAction.DEPR_SOYD),
        key(3, 2, "→M",    "C STAT", "DB",       KeyAction.STORE_M,    KeyAction.CLEAR_STAT,    KeyAction.DEPR_DB),
        key(3, 3, "RM",    "(",      "HYP",      KeyAction.RECALL_M,   KeyAction.PAREN_OPEN,    KeyAction.HYP_MODIFIER),
        key(3, 4, "M+",    ")",      "INV",      KeyAction.M_PLUS,     KeyAction.PAREN_CLOSE,   KeyAction.INV_MODIFIER),
    ),
    // Row 4: Shift keys (2 keys only, rendered separately)
    listOf(
        key(4, 0, "▲",     "",       "",         KeyAction.BLUE_SHIFT,  KeyAction.NOOP,         KeyAction.NOOP),
        key(4, 1, "▼",     "",       "",         KeyAction.ORANGE_SHIFT,KeyAction.NOOP,         KeyAction.NOOP),
    ),
    // Row 5: 7, 8, 9, ÷  (4 keys)
    listOf(
        key(5, 0, "7",     "x̄,ȳ",    "n",        KeyAction.DIGIT_7,    KeyAction.STAT_MEAN_XY,  KeyAction.STAT_N),
        key(5, 1, "8",     "Sx,Sy",   "Σx",      KeyAction.DIGIT_8,    KeyAction.STAT_SX_SY,    KeyAction.STAT_SUM_X),
        key(5, 2, "9",     "Ox,Oy",   "Σy",      KeyAction.DIGIT_9,    KeyAction.STAT_OX_OY,    KeyAction.STAT_SUM_Y),
        key(5, 3, "÷",     "1/x",     "SIN",     KeyAction.DIVIDE,     KeyAction.RECIP,         KeyAction.SIN),
    ),
    // Row 6: 4, 5, 6, ×  (4 keys)
    listOf(
        key(6, 0, "4",     "x̂,r",    "Σx²",      KeyAction.DIGIT_4,    KeyAction.STAT_XR,       KeyAction.STAT_SUM_X2),
        key(6, 1, "5",     "ŷ,m",    "Σy²",      KeyAction.DIGIT_5,    KeyAction.STAT_YM,       KeyAction.STAT_SUM_Y2),
        key(6, 2, "6",     "x̄w,b",   "Σxy",      KeyAction.DIGIT_6,    KeyAction.STAT_XW_B,     KeyAction.STAT_SUM_XY),
        key(6, 3, "×",     "y^x",    "COS",      KeyAction.MULTIPLY,   KeyAction.Y_POW_X,       KeyAction.COS),
    ),
    // Row 7: C, 1, 2, 3, -  (5 keys)
    listOf(
        key(7, 0, "C",     "C ALL",  "C MEM",    KeyAction.CLEAR,      KeyAction.CLEAR_ALL,     KeyAction.CMEM_ARM),
        key(7, 1, "1",     "eˣ",     "REGR",     KeyAction.DIGIT_1,    KeyAction.E_POW_X,       KeyAction.REGR),
        key(7, 2, "2",     "LN",     "df,t→P",   KeyAction.DIGIT_2,    KeyAction.LN,            KeyAction.DF_T_TO_P),
        key(7, 3, "3",     "n!",     "Z→P",      KeyAction.DIGIT_3,    KeyAction.FACTORIAL,     KeyAction.Z_TO_P),
        key(7, 4, "-",     "√x",     "TAN",      KeyAction.SUBTRACT,   KeyAction.SQRT,          KeyAction.TAN),
    ),
    // Row 8: ON, 0, ., =, +  (5 keys)
    listOf(
        key(8, 0, "ON",    "OFF",    "Alg/Chain",KeyAction.NOOP,       KeyAction.ON_OFF,        KeyAction.ALG_CHAIN_TOGGLE),
        key(8, 1, "0",     "π",      "nPr",      KeyAction.DIGIT_0,    KeyAction.PI,            KeyAction.N_PERMUT_R),
        key(8, 2, ".",     "+/-",    "nCr",      KeyAction.DECIMAL,    KeyAction.SIGN_CHANGE,   KeyAction.N_COMB_R),
        key(8, 3, "=",     "DISP",   "RAND",     KeyAction.EQUALS,     KeyAction.DISP,          KeyAction.RAND),
        key(8, 4, "+",     "x²",     "Rad/Deg",  KeyAction.ADD,        KeyAction.X_SQUARED,     KeyAction.RAD_DEG_TOGGLE),
    ),
)
