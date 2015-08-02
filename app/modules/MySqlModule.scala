package modules

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration


class MySqlModule extends AbstractModule with ScalaModule {

  @Provides
  @Singleton
  def dataSource(c: Configuration): HikariDataSource = {
    val mySqlConfiguration: MySqlModule.MySqlConfiguration = c.underlying.as[MySqlModule.MySqlConfiguration]("mysql")
    val hc = new HikariConfig()
    hc.setJdbcUrl(s"jdbc:mysql://${mySqlConfiguration.host }:${mySqlConfiguration.port }/${mySqlConfiguration.database }")
    hc.setUsername(mySqlConfiguration.user)
    hc.setPassword(mySqlConfiguration.password)
    mySqlConfiguration.hikari foreach { case (key, value) => hc.addDataSourceProperty(key, value) }
    new HikariDataSource(hc)
  }

  override def configure(): Unit = {}
}

object MySqlModule {

  case class MySqlConfiguration(host: String, port: Int, database: String, user: String, password: String, hikari: Map[String, String])

}
