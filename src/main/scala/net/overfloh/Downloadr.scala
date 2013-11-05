package net.overfloh

import scala.xml.XML
import scala.xml.Node
import java.net.URL

case class Build(number: Int, url: String)

class Downloadr {
  private val hostName = "http://lnz-bobthebuilder/hudson/"
  private val xmlApiSuffix = "job/SilkTest_Setup/api/xml"

  private def toBuild(xmlNode: Node) = {
    val numberNode = xmlNode \ "number"
    val urlNode = xmlNode \ "url"
    Build(numberNode.text.toInt, urlNode.text)
  }

  private def fromUrl(url: String) =
    new URL(url).openConnection().getInputStream()

  /**
   * finds the triggering job for a certain build.
   * @param url the URL in the format http://hostname/job/jobName/&lt;buildNumber&gt;
   */
  private def findTriggeringBuild(url: String) = {
    val buildsXml = XML.load(fromUrl(url + "/api/xml"))
    val cause = buildsXml \\ "cause"
    for {
      url <- (cause \ "upstreamUrl").headOption
      build <- (cause \ "upstreamBuild").headOption
    } yield Build(build.text.toInt, hostName + url.text + build.text)
  }

  private def reverseSearchForBuild(buildNumber: Int): Option[(Build, Build)] = {
    val buildsXml = XML.load(fromUrl(hostName + xmlApiSuffix))
    val builds = (buildsXml \\ "build").map(toBuild).toList.reverse

    {
      for {
        build <- builds
        triggeringBuild <- findTriggeringBuild(build.url)
        if (triggeringBuild.number == buildNumber)
      } yield (triggeringBuild, build)
    }.headOption
  }

  def loadSetupForBuildNumber(buildNumber: Int) = {
    val searchResult = reverseSearchForBuild(buildNumber)
    val (nightlyBuild, setupBuild) = searchResult.getOrElse(throw new RuntimeException("no matching build found."))

    val exeName = "silktest150-" + nightlyBuild.number + ".exe"
    val exePath = "artifact/IASetup/SilkTestSetup/SilkTestSetup_Build_Output/Web_Installers/InstData/Windows/VM/"
    val fullUrl = setupBuild.url + exePath + exeName

    import sys.process._
    import scala.language.postfixOps // for postfix `!!`
    import java.io.File
    new URL(fullUrl) #> new File(exeName) !!
  }

}

object Downloadr extends App {
  val buildNumber = args.headOption.map(_.toInt).getOrElse(throw new RuntimeException("Please specify a build number."))
  
  new Downloadr().loadSetupForBuildNumber(buildNumber)
}