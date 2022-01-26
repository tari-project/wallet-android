package com.tari.android.wallet.model

import junit.framework.TestCase
import org.junit.Assert
import java.math.BigInteger

class MicroTariTest : TestCase() {

    fun testGetFormattedTariValue() {
        Assert.assertEquals("-0.000002", MicroTari(BigInteger.valueOf(-2)).formattedTariValue)
        Assert.assertEquals("-0.000002", MicroTari(BigInteger.valueOf(-2)).formattedTariValue)
        Assert.assertEquals("-0.00002", MicroTari(BigInteger.valueOf(-20)).formattedTariValue)
        Assert.assertEquals("-0.0002", MicroTari(BigInteger.valueOf(-200)).formattedTariValue)
        Assert.assertEquals("-0.002", MicroTari(BigInteger.valueOf(-2000)).formattedTariValue)
        Assert.assertEquals("-0.02", MicroTari(BigInteger.valueOf(-20000)).formattedTariValue)
        Assert.assertEquals("-0.2", MicroTari(BigInteger.valueOf(-200000)).formattedTariValue)
        Assert.assertEquals("-2", MicroTari(BigInteger.valueOf(-2000000)).formattedTariValue)
        Assert.assertEquals("-20", MicroTari(BigInteger.valueOf(-20000000)).formattedTariValue)
        Assert.assertEquals("-200", MicroTari(BigInteger.valueOf(-200000000)).formattedTariValue)
        Assert.assertEquals("-20.20202", MicroTari(BigInteger.valueOf(-20202020)).formattedTariValue)
        Assert.assertEquals("-12.020202", MicroTari(BigInteger.valueOf(-12020202)).formattedTariValue)
        Assert.assertEquals("-22.222222", MicroTari(BigInteger.valueOf(-22222222)).formattedTariValue)
        Assert.assertEquals("0", MicroTari(BigInteger.valueOf(0)).formattedTariValue)
        Assert.assertEquals("0.000002", MicroTari(BigInteger.valueOf(2)).formattedTariValue)
        Assert.assertEquals("0.00002", MicroTari(BigInteger.valueOf(20)).formattedTariValue)
        Assert.assertEquals("0.0002", MicroTari(BigInteger.valueOf(200)).formattedTariValue)
        Assert.assertEquals("0.002", MicroTari(BigInteger.valueOf(2000)).formattedTariValue)
        Assert.assertEquals("0.02", MicroTari(BigInteger.valueOf(20000)).formattedTariValue)
        Assert.assertEquals("0.2", MicroTari(BigInteger.valueOf(200000)).formattedTariValue)
        Assert.assertEquals("2", MicroTari(BigInteger.valueOf(2000000)).formattedTariValue)
        Assert.assertEquals("20", MicroTari(BigInteger.valueOf(20000000)).formattedTariValue)
        Assert.assertEquals("200", MicroTari(BigInteger.valueOf(200000000)).formattedTariValue)
        Assert.assertEquals("20.20202", MicroTari(BigInteger.valueOf(20202020)).formattedTariValue)
        Assert.assertEquals("12.020202", MicroTari(BigInteger.valueOf(12020202)).formattedTariValue)
        Assert.assertEquals("22.222222", MicroTari(BigInteger.valueOf(22222222)).formattedTariValue)
    }
}