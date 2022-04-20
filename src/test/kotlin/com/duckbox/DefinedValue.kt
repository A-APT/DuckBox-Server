package com.duckbox

import Point
import java.math.BigInteger

object DefinedValue {

    val R_: Point = Point(
        BigInteger("d80387d2861da050c1a8ae11c9a1ef5ed93572bd6537d50984c1dea2f2db912b", 16),
        BigInteger("edcef3840df9cd47256996c460f0ce045ccb4fac5e914f619c44ad642779011", 16)
    )

    val pubkey: Point = Point(
        BigInteger("d7bf79fbdfa2c473d86d2f5fb325c05a3f9815c6b6e3bd7c1b61780651be8be7", 16),
        BigInteger("79a09b8427069518535389161410ae45643588fd945919b9f53f6e1a5b98554f", 16)
    )

    val voteOwnerPrivate: String = "3a3eb88cc8cfc1c5dcaf67bfd7fef3609bdc7fab6fe8a43af6adc16586fbd6e5"
    // => BigInteger("3a3eb88cc8cfc1c5dcaf67bfd7fef3609bdc7fab6fe8a43af6adc16586fbd6e5", 16)

    val voteOwnerPublic: Point = Point(
        BigInteger("4719ded852f84728c0e25e2a7111e880f4ef516155f62e3db82be7b2981b0323", 16),
        BigInteger("e84813d29f2125b707bc94244aec3c3d52a8025b5f7c988c92736daa22a621ac", 16)
    )
}
