package com.twitter.service.cache.client

import cachet.client.HttpClient
import java.util.List
import java.util.Collections._
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import scala.collection.jcl.Conversions._

class ForwardRequest(httpClient: HttpClient) {
  def apply(request: HttpServletRequest, response: HttpServletResponse) {
    httpClient.host = "localhost"
    httpClient.port = 3000
    httpClient.scheme = request.getScheme
    httpClient.method = request.getMethod
    httpClient.uri = request.getRequestURI
    httpClient.queryString = request.getQueryString
    for (headerName <- list(request.getHeaderNames).asInstanceOf[List[String]];
         headerValue <- list(request.getHeaders(headerName)).asInstanceOf[List[String]])
      httpClient.addHeader(headerName, headerValue)

    httpClient.performRequestAndWriteTo(response)
  }
}