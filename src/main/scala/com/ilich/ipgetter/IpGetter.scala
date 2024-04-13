package com.ilich.ipgetter

import zio._
import zio.http._
import scala.util.matching.Regex

object IpGetter extends ZIOAppDefault {
  val numberPattern: Regex = """\d{1,3}\.\d{1,3}\.\d{1,3}.\d{1,3}""".r

  def tryParseIp(body: String): IO[NumberFormatException, String] = 
    ZIO.getOrFailWith(new NumberFormatException(s"Failed to parse IP from response body: $body"))(numberPattern.findFirstIn(body))

  val url = URL.decode("https://api.ipify.org/?format=json").toOption.get

  val exponentialTwice = Schedule.exponential(1.second) && Schedule.recurs(2)

  def requestAndParse(client: Client): ZIO[Scope, Throwable, String] = for {
    res    <- client.url(url).get("/")
    data   <- res.body.asString
    ip     <- tryParseIp(data) 
  } yield ip

  val program = for {
    client <- ZIO.service[Client]
    ip     <- requestAndParse(client) retry exponentialTwice
    _      <- Console.printLine(ip)
  } yield ()

  override val run = program.provide(Client.default, Scope.default)
  // test error body, test unavailable service, test numberpattern matching

}