package com.duckbox.domain.group

import javax.persistence.*

@Entity
class GroupEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var leader: String, // did

    @Column(nullable = false)
    var status: GroupStatus,

    @Column
    var description: String,

    @Column(nullable = false)
    var menbers: Int,

    @Column
    var profile: String? = null, // image

    @Column
    var header: String? = null, // image

)

enum class GroupStatus {
    ALIVE, // [활성화]
    DELETED, // [삭제된]
    PENDING, // [인증전]
    REPORTED, // [신고된]
}
