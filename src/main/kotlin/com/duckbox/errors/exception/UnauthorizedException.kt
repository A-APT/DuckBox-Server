package com.duckbox.errors.exception

import java.lang.RuntimeException

class UnauthorizedException(message: String) : RuntimeException(message)