package com.duckbox.domain.user

import java.util.*
import javax.persistence.*

@Entity
class EmailAuth (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1,

    @Column
    var email: String, // target email

    @Column
    var token: String, // validate token

    @Column
    var expirationTime: Long, // time in milliseconds

    @Column
    var expired: Boolean,

        )
