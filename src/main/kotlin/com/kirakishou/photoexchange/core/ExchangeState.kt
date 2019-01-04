package com.kirakishou.photoexchange.core

enum class ExchangeState(val state: Int) {
  ReadyToExchange(0),
  Exchanging(1),
  Exchanged(2);

  companion object {
    fun fromInt(state: Int): ExchangeState {
      return ExchangeState.values().first { it.state == state }
    }
  }
}