package com.ilich.ipgetter

import zio._
import zio.http._
import zio.http.netty.client.NettyClientDriver
import zio.test._
import zio.test.Assertion._

import java.io.IOException
import scala.util.Try

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.WireMockServer

import IpGetter._


object IpGetterSpec extends ZIOSpecDefault {
  val wireMockServer = new WireMockServer()
  //todo: test error body
  def spec = suite("IpGetterSpec")(
    suite("tryParseIpSpec")(
      test("tryParseIp correctly parses IP from response") {
        for {
          ip <- tryParseIp("""{"ip": "192.168.0.19"}""")
        } yield assertTrue(ip == "192.168.0.19")
      },
      test("tryParseIp fails with NumberFormatException if it cannot parse IP") {
        assertZIO(tryParseIp("Hi").exit)(fails(isSubtype[NumberFormatException](anything)))
      }
    ),
    suite("IpGetterRequestSpec")(
      test("request fails after being retried three times when return status is 500 and the body is parseable") {
        stubFor(get("/?format=json")
          .willReturn(aResponse().withStatus(500).withBody("""{"ip":"176.100.1.212"}""")))

        for {
          client <- ZIO.service[Client]
          result <- requestAndParse(client, "http://127.0.0.1:8080/?format=json").retry(ExponentialTwice).exit
        } yield assertTrue(result.isFailure && Try(verify(3, getRequestedFor(urlEqualTo("/?format=json")))).isSuccess)
      } @@ TestAspect.withLiveClock,
      test("request succeeds and ip is printed to the console") {
        stubFor(get("/?format=json")
          .willReturn(ok("""{"ip":"176.100.1.212"}""")))

        for {
          client <- ZIO.service[Client]
          result <- requestAndParse(client, "http://127.0.0.1:8080/?format=json")
            .retry(ExponentialTwice)
        } yield assertTrue(result == "176.100.1.212") 
      }
      /* ,
      test("request fails again") {
        for {
          client <- ZIO.service[Client]
          fiber  <- (client.url(URL.decode("http://127.0.0.1").toOption.get).get("/") retry IpGetter.ExponentialTwice).fork
          _      <- TestClock.adjust(1.second)
          _      <- TestClock.adjust(1.second)
          result <- fiber.join.exit
        } yield assertTrue(result.isFailure)
      } */
    ).provideShared(Client.default, Scope.default) 
    @@ TestAspect.sequential
    //@@ TestAspect.after(ZIO.succeed(WireMock.reset()))
    @@ TestAspect.beforeAll(ZIO.succeed(wireMockServer.start()))
    @@ TestAspect.afterAll(ZIO.succeed(wireMockServer.stop()))
  )
  
}