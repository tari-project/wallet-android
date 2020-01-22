package com.tari.android.wallet.util

internal data class DummyWallet(
    var balanceTaris: Double
)

internal data class DummyTx(
    val timestamp: Long,
    val contactAlias: String,
    val message: String,
    val value: Double
)

internal val dummyWalletBalances = doubleArrayOf(
    55721.536,
    158904.24,
    58723.03,
    8904.24,
    851.38,
    102812.43,
    1102852.0
)

private var currentBalanceIndex = 0

internal fun nextBalance(): Double {
    return dummyWalletBalances[(currentBalanceIndex++) % dummyWalletBalances.size]
}

internal val dummyWallet = DummyWallet(balanceTaris = nextBalance())
//val dummyWallet = DummyWallet(balanceTaris = 0.536)

internal val dummyTxs = arrayOf(
    DummyTx(
        1576158338,
        "Sarah_D",
        "Yesterday's Lunch",
        34.54
    ),
    DummyTx(
        1576151135,
        "The_StarWars_Collector",
        "Thanks for the Skywalker Skin!",
        14.0
    ),
    DummyTx(
        1576115135,
        "Morpheus",
        "Thanks!",
        1.3
    ),
    DummyTx(
        1576107935,
        "Mr Anonymous",
        "I owe you this.",
        -165.74
    ),
    DummyTx(
        1576064735,
        "James",
        "Cheers!",
        -23.23
    ),
    DummyTx(
        1576021535,
        "The Terminator",
        "Some other message",
        10.23
    ),
    DummyTx(
        1575848735,
        "The_Fortnite_Kid",
        "My half for the rent",
        3452.0
    ),
    DummyTx(
        1575805535,
        "Lakers_Fan",
        "Some message",
        -105.43
    ),
    DummyTx(
        1569897935,
        "Somebody",
        "Thanks again for helping with my move.",
        -52.43
    ),
    DummyTx(
        1569930335,
        "John_Doe",
        "Here you go dude!",
        1453102.34
    )
)
