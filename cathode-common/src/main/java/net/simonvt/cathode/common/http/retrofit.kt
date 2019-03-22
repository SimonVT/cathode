package net.simonvt.cathode.common.http

import retrofit2.Response

fun <T> Response<T>.requireBody(): T = body()!!
