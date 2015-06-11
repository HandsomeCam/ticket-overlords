package com.semisafe.ticketoverlords

import org.joda.time.DateTime

case class Event(
  id: Long,
  name: String,
  start: DateTime,
  end: DateTime,
  address: String,
  city: String,
  state: String,
  country: String)