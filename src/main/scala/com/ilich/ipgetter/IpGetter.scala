package com.ilich.ipgetter

import zio._
import zio.http._
import scala.util.matching.Regex
import java.util.concurrent.TimeUnit
import zio.Clock.currentTime

object IpGetter extends ZIOAppDefault {
  val NumberPattern: Regex = """\d{1,3}\.\d{1,3}\.\d{1,3}.\d{1,3}""".r
  val ExponentialTwice = Schedule.recurs(2) && Schedule.exponential(500.millis)

  def tryParseIp(body: String): IO[NumberFormatException, String] = {
    lazy val exception = new NumberFormatException(s"Failed to parse IP from response body: $body")
    ZIO.getOrFailWith(exception)(IpGetter.NumberPattern.findFirstIn(body))
  }

  def validateStatus(s: Status) = ZIO.whenCase(s){
    case Status.Ok => ZIO.succeed(())
    case _         => ZIO.fail(new IllegalStateException(s"HTTP response status is $s"))
  }

  def requestAndParse(client: Client, urlString: String): ZIO[Scope, Throwable, String] = {
    val url = URL.decode(urlString).toOption.get
    for {
      time   <- currentTime(TimeUnit.SECONDS)
      _      <- Console.printLine(s"attempting request:$time")
      res    <- client.url(url).get("/") //todo: if this fails, retry
      _      <- validateStatus(res.status)
      data   <- res.body.asString
      ip     <- tryParseIp(data) 
    } yield ip
  }

  def program(urlString: String = "https://api.ipify.org/?format=json") = for {
    client <- ZIO.service[Client]
    ip     <- requestAndParse(client, urlString) retry ExponentialTwice
    _      <- Console.printLine(ip) //todo: handle errors
  } yield ()

  override val run = program().provide(Client.default, Scope.default)

}