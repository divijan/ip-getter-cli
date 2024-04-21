package com.ilich.ipgetter

import zio._
import zio.http._
import scala.util.matching.Regex
import java.util.concurrent.TimeUnit
import zio.Clock.currentTime

object IpGetter extends ZIOAppDefault {
  val NumberPattern: Regex = """\d{1,3}\.\d{1,3}\.\d{1,3}.\d{1,3}""".r
  val ExponentialTwice = Schedule.recurs(2).tapOutput(o => ZIO.logDebug(s"retrying $o")) && Schedule.exponential(500.millis)

  def tryParseIp(body: String): IO[NumberFormatException, String] = {
    lazy val exception = new NumberFormatException(s"Failed to parse IP from response body: $body")
    ZIO.getOrFailWith(exception)(IpGetter.NumberPattern.findFirstIn(body))
  }

  def validateStatus(res: Response) = ZIO.whenCase(res.status){
    case Status.Ok => ZIO.succeed(())
    case s         => res.body.asString.flatMap(body => ZIO.fail(new IllegalStateException(
      s"Error msg received from API: $body (status: $s)")))
  }

  def requestAndParse(client: Client, urlString: String): ZIO[Scope, Throwable, String] = {
    val url = URL.decode(urlString).toOption.get
    for {
      res  <- client.url(url).get("/")
      _    <- validateStatus(res)
      data <- res.body.asString
      ip   <- tryParseIp(data) 
    } yield ip
  }

  def program(urlString: String = "https://api.ipify.org/?format=json") = for {
    client <- ZIO.service[Client]
    ip     <- requestAndParse(client, urlString) retry ExponentialTwice
    _      <- Console.printLine(ip)
  } yield ()

  override val run = program().provide(Client.default, Scope.default)

}