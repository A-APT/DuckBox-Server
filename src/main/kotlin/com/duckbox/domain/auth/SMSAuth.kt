package com.duckbox.domain.auth

import javax.persistence.*

@Entity
class SMSAuth (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1,

    @Column
    var phoneNumber: String, // target phone-number

    @Column
    var token: String, // validate token

    @Column
    var expirationTime: Long, // time in milliseconds

    @Column
    var expired: Boolean,

        )
