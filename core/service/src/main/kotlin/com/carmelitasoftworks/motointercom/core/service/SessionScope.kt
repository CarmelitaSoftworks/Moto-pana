package com.carmelitasoftworks.motointercom.core.service

import javax.inject.Qualifier

/** App-wide scope the ride runs in; survives screens being closed. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionScope
