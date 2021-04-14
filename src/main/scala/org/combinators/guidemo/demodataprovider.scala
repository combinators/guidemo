package org.combinators.guidemo

import java.sql._
import javax.inject.Inject
import org.combinators.guidemo.domain.DatabaseType
import play.api.mvc.InjectedController
import org.h2.tools.{Server, Shell}
import play.api.inject.ApplicationLifecycle

import java.io.{BufferedReader, StringReader}
import scala.concurrent.Future
import scala.util.Try

class DataProviderController @Inject()(applicationLifecycle: ApplicationLifecycle) extends InjectedController {

  val data = Seq("Espresso", "Cappuccino", "Filter Coffee")

  val sqlURL = s"${DatabaseType.JDBC.defaultLocation};DB_CLOSE_DELAY=-1"

  private def populateDatabase(): Unit = {
    Class.forName("org.h2.Driver")
    val connection = DriverManager.getConnection(sqlURL, "sa", "")
    try {
      if (!connection.getMetaData.getTables(null, null, "COFFEE", null).next()) {
        println("initializing table")
        connection.createStatement().executeUpdate("CREATE TABLE COFFEE(name VARCHAR(255) NOT NULL, PRIMARY KEY(name))")
        data.foreach { datum =>
          val stmt = connection.prepareStatement("INSERT INTO COFFEE VALUES(?)")
          stmt.setString(1, datum)
          stmt.execute()
        }
      }
    } finally {
      connection.close()
    }
  }

  val sqlServer: Server = {
    val shell = new Shell()
    shell.setInReader(new BufferedReader(new StringReader("quit")))
    shell.runTool("-url", "jdbc:h2:mem:coffee", "-user", "sa", "-password", "")
    val result = Server.createTcpServer().start()
    applicationLifecycle.addStopHook { () =>
      Future.fromTry(Try {
        result.shutdown()
      })
    }
    populateDatabase()
    result
  }

  def productOptions() = Action {
    Ok(s"""[${data.mkString("\"", "\", \"", "\"")}]""").as("text/plain")
  }

  def sqlTest() = Action {
    Class.forName("org.h2.Driver")
    val connection = DriverManager.getConnection(sqlURL, "sa", "")
    val entries: Seq[String] = Try {
        try {
          val results = connection.prepareStatement("SELECT name FROM coffee").executeQuery()
          val resultList: scala.collection.mutable.MutableList[String] = scala.collection.mutable.MutableList.empty
          while (results.next()) {
            resultList += results.getString("name")
          }
          resultList
        } finally {
          connection.close()
        }
      }.get
    Ok(s"""${entries.mkString(",")}""").as("text/plain")
  }
}



