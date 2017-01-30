package com.fijimf.deepfij.models.services

import java.lang.management.ManagementFactory
import java.time.LocalDate
import javax.inject.Inject

import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.mailer.{Email, MailerClient}

import scala.collection.JavaConverters._
import scala.language.postfixOps

class MemoryMonitorServiceImpl @Inject()(mailerClient: MailerClient, override val messagesApi: MessagesApi) extends MemoryMonitorService with I18nSupport {
  val logger = Logger(this.getClass)
  val mxMemBean = ManagementFactory.getMemoryMXBean
  ManagementFactory.

  def check() {
    val heap = mxMemBean.getHeapMemoryUsage
    val nonHeap = mxMemBean.getNonHeapMemoryUsage
    logger.info(s"Heap     ${heap.getUsed}\t${heap.getMax}\t${100 * heap.getUsed.toDouble / heap.getMax.toDouble}")
    logger.info(s"Non-heap ${nonHeap.getUsed}\t${nonHeap.getMax}\t${100 * nonHeap.getUsed.toDouble / nonHeap.getMax.toDouble}")

    ManagementFactory.getGarbageCollectorMXBeans.asScala.foreach(gc => {
      logger.info(s"GC --    ${gc.getName}\t${gc.getCollectionCount}\t\t${gc.getCollectionTime}")
    })
  }

  private def mailSuccessReport(optDates: Option[List[LocalDate]]) = {
   //Eventually send a warning
  }

}