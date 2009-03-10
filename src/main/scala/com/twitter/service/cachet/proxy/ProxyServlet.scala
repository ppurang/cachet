package com.twitter.service.cachet

import com.twitter.service.cache.proxy.client.ForwardRequest
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import proxy.client.{JettyHttpClient, ApacheHttpClient}

class ProxyServlet(host: String, port: Int, timeout: Long) extends HttpServlet {
  var config = null: ServletConfig
  var forwardRequest = null: ForwardRequest

  override def init(c: ServletConfig) {
    config = c
    val client = new JettyHttpClient(timeout)
    forwardRequest = new ForwardRequest(client, host, port)
  }

  override def service(request: HttpServletRequest, response: HttpServletResponse) {
    forwardRequest(request, response)
  }
}