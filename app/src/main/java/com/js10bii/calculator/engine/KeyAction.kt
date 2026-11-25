package com.js10bii.calculator.engine

enum class KeyAction {
    // Digits
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
    DECIMAL,
    SIGN_CHANGE,

    // Arithmetic operators
    ADD, SUBTRACT, MULTIPLY, DIVIDE, EQUALS,

    // Clear / navigation
    CLEAR,
    CLEAR_ALL,
    BACKSPACE,

    // Shift keys
    ORANGE_SHIFT,
    BLUE_SHIFT,

    // TVM registers
    TVM_N,
    TVM_IYR,
    TVM_PV,
    TVM_PMT,
    TVM_FV,
    TVM_XPYR,
    TVM_NOM_PCT,
    TVM_EFF_PCT,
    TVM_PYR,
    TVM_AMORT,
    TVM_BEG_END,

    // Bond (deferred - keys defined to avoid NOOP chains)
    BOND_ACCINT, BOND_YTM, BOND_PRICE, BOND_CPN, BOND_CALL,

    // Date/calendar (deferred)
    DATE_DMY, DATE_360_ACT, DATE_SEMI_ANN, DATE_SET, DATE_MAT,

    // Business %
    INPUT,
    MU,
    CST,
    PRC,
    MAR,
    PCT,
    PCT_CHG,
    DATE_CALC,
    DATE_DELTA_DAYS,

    // Cash flow (deferred)
    CFJ, NJ, IRR_YR, NPV_CALC,

    // Statistics (deferred)
    STAT_PLUS,
    STAT_MINUS,
    STAT_MEAN_XY,
    STAT_SX_SY,
    STAT_OX_OY,
    STAT_XW_B,
    STAT_XR,
    STAT_YM,
    STAT_N,
    STAT_SUM_X,
    STAT_SUM_Y,
    STAT_SUM_X2,
    STAT_SUM_Y2,
    STAT_SUM_XY,
    REGR,
    CLEAR_STAT,

    // Break-even (deferred)
    BREAKEVEN_UNITS, BREAKEVEN_SP, BREAKEVEN_VC, BREAKEVEN_FC, BREAKEVEN_PROFIT,

    // Memory
    STORE_M,
    RECALL_M,
    M_PLUS,
    STO,
    RCL,
    K_CONST,
    SWAP,
    RND,

    // Math — one operand
    SQRT,
    X_SQUARED,
    RECIP,
    E_POW_X,
    LN,
    FACTORIAL,
    PI,

    // Math — two operand
    Y_POW_X,
    N_PERMUT_R,
    N_COMB_R,

    // Trig
    SIN, COS, TAN,
    INV_MODIFIER,
    HYP_MODIFIER,

    // Probability
    Z_TO_P, DF_T_TO_P, NPR, NCR, RAND,
    DF_P_TO_T, // INV df,t→P; only used as a pending operation, not on a key

    // Depreciation (deferred)
    DEPR_SL, DEPR_SOYD, DEPR_DB,

    // Display / mode
    DISP,
    RAD_DEG_TOGGLE,
    ALG_CHAIN_TOGGLE,

    // CMEM boxed multi-key selector
    CMEM_ARM,

    // Scientific notation
    SCI_NOTATION,

    // Parentheses
    PAREN_OPEN,
    PAREN_CLOSE,

    // Power
    ON_OFF,

    NOOP,
}
