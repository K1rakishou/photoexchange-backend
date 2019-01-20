package com.kirakishou.photoexchange.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.springframework.mock.web.reactive.function.server.MockServerRequest

class ServerRequestExtensionsTest {

  @Test
  fun `test getIntVariable with Int MAX_VALUE`() {
    val request = MockServerRequest.builder().pathVariable("test", Int.MAX_VALUE.toString()).build()
    assertEquals(Int.MAX_VALUE, request.getIntVariable("test", Int.MIN_VALUE, Int.MAX_VALUE))
  }

  @Test
  fun `test getIntVariable with Int MIN_VALUE`() {
    val request = MockServerRequest.builder().pathVariable("test", Int.MIN_VALUE.toString()).build()
    assertEquals(Int.MIN_VALUE, request.getIntVariable("test", Int.MIN_VALUE, Int.MAX_VALUE))
  }

  @Test
  fun `test must coerce lower bound`() {
    val request = MockServerRequest.builder().pathVariable("test", Int.MIN_VALUE.toString()).build()
    assertEquals(-10, request.getIntVariable("test", -10, 10))
  }

  @Test
  fun `test must coerce upper bound`() {
    val request = MockServerRequest.builder().pathVariable("test", Int.MAX_VALUE.toString()).build()
    assertEquals(10, request.getIntVariable("test", -10, 10))
  }

  @Test
  fun `test getLongVariable with Long MAX_VALUE`() {
    val request = MockServerRequest.builder().pathVariable("test", Long.MAX_VALUE.toString()).build()
    assertEquals(Long.MAX_VALUE, request.getLongVariable("test", Long.MIN_VALUE, Long.MAX_VALUE))
  }

  @Test
  fun `test getLongVariable with Long MIN_VALUE`() {
    val request = MockServerRequest.builder().pathVariable("test", Long.MIN_VALUE.toString()).build()
    assertEquals(Long.MIN_VALUE, request.getLongVariable("test", Long.MIN_VALUE, Long.MAX_VALUE))
  }

  @Test
  fun `test getLongVariable must coerce lower bound`() {
    val request = MockServerRequest.builder().pathVariable("test", Long.MIN_VALUE.toString()).build()
    assertEquals(-10L, request.getLongVariable("test", -10, 10))
  }

  @Test
  fun `test getLongVariable must coerce upper bound`() {
    val request = MockServerRequest.builder().pathVariable("test", Long.MAX_VALUE.toString()).build()
    assertEquals(10L, request.getLongVariable("test", -10, 10))
  }

  @Test
  fun `test getDoubleVariable with Double MAX_VALUE`() {
    val request = MockServerRequest.builder().pathVariable("test", Double.POSITIVE_INFINITY.toString()).build()
    assertEquals(Double.POSITIVE_INFINITY, request.getDoubleValue("test", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
  }

  @Test
  fun `test getDoubleVariable with Double MIN_VALUE`() {
    val request = MockServerRequest.builder().pathVariable("test", Double.NEGATIVE_INFINITY.toString()).build()
    assertEquals(Double.NEGATIVE_INFINITY, request.getDoubleValue("test", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
  }

  @Test
  fun `test getDoubleVariable must coerce lower bound`() {
    val request = MockServerRequest.builder().pathVariable("test", Double.NEGATIVE_INFINITY.toString()).build()
    assertEquals(-10.0, request.getDoubleValue("test", -10.0, 10.0))
  }

  @Test
  fun `test getDoubleVariable must coerce upper bound`() {
    val request = MockServerRequest.builder().pathVariable("test", Double.POSITIVE_INFINITY.toString()).build()
    assertEquals(10.0, request.getDoubleValue("test", -10.0, 10.0))
  }

  @Test
  fun `test getStringVariable must return null if string is too long`() {
    val request = MockServerRequest.builder().pathVariable("test", "this_is_a_test_string|and_this_part_should_be_removed").build()
    assertNull(request.getStringVariable("test", 21))
  }
}