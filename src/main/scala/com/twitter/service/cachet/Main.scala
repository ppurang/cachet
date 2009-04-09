package com.twitter.service.cachet

import limiter.LimitingProxyServletFilter
import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import net.lag.logging.Logger
import java.util.Properties

object Main {
  private val log = Logger.get
  private val runtime = new RuntimeEnvironment(getClass)

  def main(args: Array[String]) {
    runtime.load(args)

    val PROXY_PORT = Configgy.config.getInt("proxy_port", 1234)
    val server = new JettyServer(1234)
    log.info("Proxy Server listening on port: %s", PORT)
    //server.addFilter(new LimitingProxyServletFilter, "/")
    val initParams = new Properties()
    // FIXME: make these configurable.
    initParams.put("backend-host", "localhost")
    initParams.put("backend-port", "80")
    initParams.put("backend-timeout", "1000")
    // FIXME: nail down how to pass all traffic through either a proxy or servlet using OpenGSE.
    //server.addFilter(classOf[BasicFilter], "/*")
    server.addServlet(classOf[ProxyServlet], "/", initParams)
    server.start()
    server.join()
  }
}
