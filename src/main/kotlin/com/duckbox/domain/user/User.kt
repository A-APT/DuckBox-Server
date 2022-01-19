package com.duckbox.domain.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.stream.Collectors
import javax.persistence.*

@Entity
class User (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1,

    @Column(nullable = false)
    var did: String,

    @Column(nullable = false)
    var studentId: Int,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    private var password: String,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var nickname: String,

    @Column(nullable = false)
    var college: String,

    @Column(nullable = false)
    var department: String,

    @ElementCollection(fetch = FetchType.EAGER)
    var roles: Set<String>
): UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return roles.stream()
            .map { role: String? ->
                SimpleGrantedAuthority(
                    role
                )
            }
            .collect(Collectors.toList())
    }
    override fun getUsername(): String = email
    override fun getPassword(): String = password
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}
