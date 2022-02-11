package com.duckbox.errors.exception

import java.lang.RuntimeException

class ForbiddenException(message: String) : RuntimeException(message)