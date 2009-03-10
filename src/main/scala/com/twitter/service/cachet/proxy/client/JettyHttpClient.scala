package com.twitter.service.cachet.proxy.client

import com.twitter.service.cache.proxy.client.RequestSpecification
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.io.InputStream
import org.mortbay.io.Buffer
import org.mortbay.jetty.client.Address
import org.mortbay.jetty.HttpSchemes

class JettyHttpClient(timeout: Long) extends HttpClient {
  val client = new org.mortbay.jetty.client.HttpClient
  client.setTimeout(timeout)
  client.start()

  def apply(host: String, port: Int, requestSpecification: RequestSpecification, servletResponse: HttpServletResponse) {
    var exchange = new HttpExchange(servletResponse)
    exchange.setRequestContentSource(requestSpecification.inputStream)
    exchange.setMethod(requestSpecification.method)
    exchange.setAddress(new Address(host, port))
    exchange.setScheme(
      if (HttpSchemes.HTTPS.equals(requestSpecification.scheme))
        HttpSchemes.HTTPS_BUFFER
      else
        HttpSchemes.HTTP_BUFFER
      )
    exchange.setURI(requestSpecification.uri)
    for ((headerName, headerValue) <- requestSpecification.headers)
      exchange.addRequestHeader(headerName, headerValue)
    client.send(exchange)
    exchange.waitForDone()
  }

  private class HttpExchange(response: HttpServletResponse) extends org.mortbay.jetty.client.HttpExchange {
    override def onResponseHeader(name: Buffer, value: Buffer) {
      response.addHeader(name.toString, value.toString)
    }

    override def onResponseContent(content: Buffer) {
      content.writeTo(response.getOutputStream)
    }

    override def onResponseStatus(version: Buffer, status: Int, reason: Buffer) {
      response.setStatus(status)
    }
  }
}