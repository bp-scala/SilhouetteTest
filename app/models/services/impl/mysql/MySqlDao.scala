package models.services.impl.mysql

import javax.sql.DataSource

import slick.driver.MySQLDriver.api._

trait MySqlDao {
  protected def dataSource: DataSource

  final protected def database = Database.forDataSource(dataSource)
}
