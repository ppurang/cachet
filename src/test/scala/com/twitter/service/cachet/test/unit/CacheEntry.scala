package com.twitter.service.cachet.test.unit

import com.twitter.service.cachet._
import javax.servlet.http._

import org.specs._
import org.specs.mock._
import org.specs.mock.JMocker._
import com.twitter.service.cachet.test.mock._

object CacheEntrySpec extends Specification with JMocker {
  "CacheEntry" should {
    var response: HttpServletResponse = null
    var cacheEntry: CacheEntry = null

    "have accessors" >> {
      doBefore {
        response = mock(classOf[HttpServletResponse])
        cacheEntry = new CacheEntry(response)
      }
    
      "addDateHeader(x, y) such that" >> {
        val millis = System.currentTimeMillis

        "it delegates to the response" +
        "dateValue returns y" >> {
          expect { one(response).addDateHeader("Date", millis) }
          cacheEntry.addDateHeader("Date", millis)
          cacheEntry.getDateHeader("Date") must be_==(millis)
        }
      }

      "addCookie(c) such that" >> {
        val cookie = new Cookie("key", "value")
      
        "it delegates to the response" +
        "cookies" >> {
          expect { one(response).addCookie(cookie) }
          cacheEntry.addCookie(cookie)
          cacheEntry.getCookies.contains(cookie) must be_==(true)
        }
      }
    
      "addHeader(n, v) such that" >> {
        val name = "name"
        val value = "value"
      
        "it delegates to the response" +
        "getHeader(n) returns v" >> {
          expect { one(response).addHeader(name, value) }
          cacheEntry.addHeader(name, value)
          cacheEntry.getHeader(name) must be(value)
        }
      }

      "addIntHeader(n, i) such that" >> {
        val name = "name"
        val value = 1
      
        "it delegates to the response" +
        "getHeader(n) returns v" >> {
          expect { one(response).addIntHeader(name, value) }
          cacheEntry.addIntHeader(name, value)
          cacheEntry.getIntHeader(name) must be(value)
        }
      }

      "sendError(...) such that" >> {
        "sendError(sc)" >> {
          val sc = 200

          "delegates to the response" +
          "getStatus() returns sc" >> {
            expect { one(response).sendError(sc) }
            cacheEntry.sendError(sc)
            cacheEntry.getStatus must be_==(sc)
          }
        }

        "sendError(sc)" >> {
          val sc = 200

          "delegates to the response" +
          "getStatus() returns sc" >> {
            expect { one(response).sendError(sc) }
            cacheEntry.sendError(sc)
            cacheEntry.getStatus must be_==(sc)
          }
        }
      }
    }
    
    "implement RFC 2616" >> {
      doBefore {
        response = new FakeHttpServletResponse
        cacheEntry = new CacheEntry(response)
        cacheEntry.noteResponseTime()
      }
      
      "age calculations" >> {
        "apparentAge" >> {
          "when dateValue <= responseTime" >> {
            "returns responseTime - dateValue" >> {
              val delta = 10
              cacheEntry.setDateHeader("Date", cacheEntry.responseTime - delta)
              cacheEntry.apparentAge must be_==(delta)
            }
          }
          
          "when dateValue > responseTime" >> {
            "returns 0" >> {
              cacheEntry.setDateHeader("Date", cacheEntry.responseTime + 10)
              cacheEntry.apparentAge must be_==(0)
            }
          }
        }

        "correctedReceivedAge" >> {
          "when apparentAge > ageValue" >> {
            "returns apparentAge" >> {
              cacheEntry.setDateHeader("Date", cacheEntry.responseTime + 1)
              cacheEntry.setIntHeader("Age", 0)
              cacheEntry.correctedReceivedAge must be_==(cacheEntry.apparentAge)
            }
          }
          
          "when apparentAge < AgeValue" >> {
            "returns ageValue" >> {
              cacheEntry.setDateHeader("Date", cacheEntry.responseTime)
              cacheEntry.setIntHeader("Age", 10)
              cacheEntry.correctedReceivedAge must be_==(cacheEntry.getIntHeader("Age"))
            }
          }
        }
        
        "responseDelay" >> {
          "returning responseTime - requestTime" >> {
            cacheEntry.responseDelay must be_==(cacheEntry.responseTime - cacheEntry.requestTime)
          }
        }
        
        "correctedInitialAge" >> {
          "returning correctedReceivedAge + responseDelay" >> {
            cacheEntry.setDateHeader("Date", cacheEntry.responseTime + 1)
            cacheEntry.setIntHeader("Age", 10)
            cacheEntry.correctedInitialAge must be_==(cacheEntry.correctedReceivedAge + cacheEntry.responseDelay)
          }
        }
        
        "residentTime" >> {
          "returning now - responseTime" >> {
            cacheEntry.residentTime must be_==(System.currentTimeMillis - cacheEntry.responseTime)
          }
        }
        
        "currentAge" >> {
          "returning correctedTnitialAge + residentTime" >> {
            cacheEntry.setDateHeader("Date", cacheEntry.responseTime + 1)
            cacheEntry.setIntHeader("Age", 10)
            cacheEntry.currentAge must be_==(cacheEntry.correctedInitialAge + cacheEntry.residentTime)
          }
        }
        
        "maxAgeValue" >> {
          "when there is a max-age control" >> {
            cacheEntry.setHeader("Cache-Control", "max-age=100")
            cacheEntry.maxAgeValue must be_==(100)
          }
          
          "when there is a s-maxage control" >> {
            cacheEntry.setHeader("Cache-Control", "s-maxage=100")
            cacheEntry.maxAgeValue must be_==(100)
          }
          
          "when both a max-age and s-maxage are present" >> {
            "returns s-maxage" >> {
              cacheEntry.setHeader("Cache-Control", "s-maxage=1,") 
              cacheEntry.maxAgeValue must be_==(1)
            }
          }
        }
      }

      "expiration calculations" >> {
        "when there is a max-age directive" >> {
          "freshnessLifetime" >> {
            "returns maxAgeValue" >> {}
          }
        }

        "when there is no max-age directive" >> {
          "when there is an Expires header" >> {
            "returns expiresValue - dateValue" >> {}
          }
        }

        "isFresh()" >> {
          "returns response_is_fresh = (freshness_lifetime > current_age)" >> {
          }
        }
      }
    }
  }
}