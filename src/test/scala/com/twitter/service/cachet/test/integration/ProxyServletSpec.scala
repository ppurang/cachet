package com.twitter.service.cachet.test.integration

import limiter.LimitingProxyServletFilter
import mock.{WaitingServlet, TestServer}
import org.mortbay.jetty.testing.HttpTester
import java.util.Properties
import javax.servlet.http.HttpServletResponse
import org.specs.Specification

object ProxyServletSpec extends Specification {
  "ProxyServlet" >> {
    def makeRequestThroughProxy(sleepTime: Long) = {
      val proxyServer = new TestServer(2345, 0, 10)
      proxyServer.addServlet(classOf[ProxyServlet], "/")
      proxyServer.start()

      val slowServer = new JettyServer(3000, 0, 10)
      val waitingProps = new Properties()
      waitingProps.put("timeout", sleepTime.toString)
      slowServer.addServlet(classOf[WaitingServlet], "/", waitingProps)
      slowServer.start()

      val request = new HttpTester
      val response = new HttpTester
      request.addHeader("X-Forwarded-For", "1.1.1.1")
      request.setMethod("GET")
      request.setURI("/")
      request.setVersion("HTTP/1.0")
      response.parse(proxyServer(request.generate))

      proxyServer.stop()
      slowServer.stop()
      response
    }

    "when the backend too slow" >> {
      "it times out the response, returning HTTP 503" >> {
        val response = makeRequestThroughProxy(2000)
        response.getStatus mustEqual HttpServletResponse.SC_GATEWAY_TIMEOUT
      }
    }

    "when the backend is fast" >> {
      "it propagates the response" >> {
        val response = makeRequestThroughProxy(0)
        response.getStatus mustEqual HttpServletResponse.SC_OK
      }
    }
  }
}
