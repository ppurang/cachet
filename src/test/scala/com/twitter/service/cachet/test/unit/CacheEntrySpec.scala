package com.twitter.service.cachet.test.unit

import com.twitter.service.cachet._
import javax.servlet.http._

import org.specs._
import org.specs.mock._
import org.specs.mock.JMocker._
import com.twitter.service.cachet.test.mock._

object CacheEntrySpec extends Specification with JMocker {
  "CacheEntry" should {
    var response: ResponseWrapper = null
    var cacheEntry: CacheEntry = null

    "implement RFC 2616" >> {
      doBefore {
        response = new ResponseWrapper(new FakeHttpServletResponse)
        cacheEntry = new CacheEntry(response)
        cacheEntry.noteResponseTime()
      }
      
      "age calculations" >> {
        "dateValue" >> {
          "when there is a Date header" >> {
            "returns the value of the header" >> {
              val millis = System.currentTimeMillis
              response.setDateHeader("Date", millis)
              cacheEntry.dateValue must be_==(millis)
            }
          }

          "when there is no Date header" >> {
            "returns the response time" >> {
              cacheEntry.dateValue must be_==(cacheEntry.responseTime)
            }
          }
        }
        
        "apparentAge" >> {
          "when dateValue <= responseTime" >> {
            "returns responseTime - dateValue" >> {
              val delta = 10
              response.setDateHeader("Date", cacheEntry.responseTime - delta)
              cacheEntry.apparentAge must be_==(delta)
            }
          }
          
          "when dateValue > responseTime" >> {
            "returns 0" >> {
              response.setDateHeader("Date", cacheEntry.responseTime + 10)
              cacheEntry.apparentAge must be_==(0)
            }
          }
        }

        "correctedReceivedAge" >> {
          "when apparentAge > ageValue" >> {
            "returns apparentAge" >> {
              response.setDateHeader("Date", cacheEntry.responseTime + 1)
              response.setIntHeader("Age", 0)
              cacheEntry.correctedReceivedAge must be_==(cacheEntry.apparentAge)
            }
          }
          
          "when apparentAge < ageValue" >> {
            "returns ageValue" >> {
              response.setDateHeader("Date", cacheEntry.responseTime)
              response.setIntHeader("Age", 10)
              cacheEntry.correctedReceivedAge must be_==(response.getIntHeader("Age").get)
            }
          }
          
          "when no ageValue" >> {
            "returns apparentAge" >> {
              response.setDateHeader("Date", cacheEntry.responseTime)
              cacheEntry.correctedReceivedAge must be_==(cacheEntry.apparentAge)              
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
            response.setDateHeader("Date", cacheEntry.responseTime)
            response.setIntHeader("Age", 10)
            cacheEntry.correctedInitialAge must be_==(10 + cacheEntry.responseDelay)
          }
        }
        
        "residentTime" >> {
          "returning now - responseTime" >> {
            cacheEntry.residentTime must be_==(System.currentTimeMillis - cacheEntry.responseTime)
          }
        }
        
        "currentAge" >> {
          "returning correctedInitialAge + residentTime" >> {
            response.setDateHeader("Date", cacheEntry.responseTime)
            response.setIntHeader("Age", 10)
            cacheEntry.currentAge must be_==(10 + cacheEntry.residentTime)
          }
        }
        
        "maxAgeValue" >> {
          "when there is a max-age control" >> {
            response.setHeader("Cache-Control", "max-age=100")
            cacheEntry.maxAgeValue must be_==(Some(100))
          }
          
          "when there is a s-maxage control" >> {
            response.setHeader("Cache-Control", "s-maxage=100")
            cacheEntry.maxAgeValue must be_==(Some(100))
          }
          
          "when both a max-age and s-maxage are present" >> {
            "returns s-maxage" >> {
              response.setHeader("Cache-Control", "s-maxage=1, max-age=2") 
              cacheEntry.maxAgeValue must be_==(Some(1))
            }
          }
        }
      }

      "expiration calculations" >> {
        "when there is a max-age directive" >> {
          "freshnessLifetime" >> {
            "returns maxAgeValue" >> {
              response.setHeader("Cache-Control", "s-maxage=1") 
              cacheEntry.freshnessLifetime must be_==(Some(1))
            }
          }
        }

        "when there is no max-age directive" >> {
          "when there is an Expires header" >> {
            "returns expiresValue - dateValue" >> {
              response.setDateHeader("Date", cacheEntry.responseTime)
              response.setDateHeader("Expires", cacheEntry.responseTime + 10)
              cacheEntry.freshnessLifetime must be_==(Some(10))
            }
          }
        }
        
        "isFresh" >> {
          "when freshnessLifetime >= currentAge" >> {
            "returns true" >> {
              response.setHeader("Cache-Control", "s-maxage=100") 
              response.setDateHeader("Date", cacheEntry.responseTime)
              cacheEntry.isFresh must be_==(true)
            }
          }
            
          "when freshnessLifeftime < currentAge" >> {
            "returns false" >> {
              response.setHeader("Cache-Control", "s-maxage=1")
              response.setDateHeader("Date", cacheEntry.responseTime + 2)
              cacheEntry.isFresh must be_==(false)
            }
          }
          
          "when there is no freshnessLifetime" >> {
            "returns false" >> {
              cacheEntry.isFresh must be_==(false)
            }
          }
        }
      }
    }
  }
}