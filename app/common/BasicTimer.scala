package common

import play.api.Logger

/**
 * User: ub (20/4/14 - 14:28)
 */
class BasicTimer(timerType:String = "", timerFor:String = "", logToConsole:Boolean = true) {

  private var startTime: Long = _

  def start(): BasicTimer = {
    startTime = System.currentTimeMillis()
    this
  }

  def stop(): Unit = {
    val stopTime = System.currentTimeMillis()
    if(logToConsole) {
      Logger.info("TIMER: [" + timerType + "] [" + timerFor + "] took [" + (stopTime - startTime) + "]")
    }
  }

}
