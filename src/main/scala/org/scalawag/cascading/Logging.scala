package org.scalawag.cascading

import org.slf4j.LoggerFactory

trait Logging {
  protected val log = LoggerFactory.getLogger(getClass)
}
