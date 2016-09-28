package com.semisafe.ticketoverlords

import org.joda.time.DateTime
import java.sql.Timestamp

import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

abstract class SlickMapping extends HasDatabaseConfigProvider[JdbcProfile] {

  implicit val jodaDateTimeMapping = {
    val dbConfig = dbConfigProvider.get[JdbcProfile]
    import dbConfig.driver.api._
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }
}
