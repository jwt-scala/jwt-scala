package pdi.scala.jwt

import org.scalatest._

class JwtUtilsSpec extends UnitSpec {
  val encoding = JwtUtils.encoding

  describe("JwtUtils") {
    describe("mapToJson") {
      it("should transform a map to a valid JSON") {
        val values: Seq[(String, Map[String, Any])] = Seq(
          """{"a":"b","c":1,"d":true}""" -> Map(
            "a" -> "b",
            "c" -> 1,
            "d" -> true
          ),
          "{}" -> Map()
        )

        values.zipWithIndex.foreach {
          case (value, index) => assertResult(value._1, "at index "+index) { JwtUtils.mapToJson(value._2) }
        }
      }
    }

    describe("mergeJson") {
      it("should correctly merge 2 JSONs") {
        val values: Seq[(String, String, Seq[String])] = Seq(
          ("{}", "{}", Seq("{}")),
          ("""{"a":1}""", """{"a":1}""", Seq("")),
          ("""{"a":1}""", """{"a":1}""", Seq("{}")),
          ("""{"a":1}""", """{}""", Seq("""{"a":1}""")),
          ("""{"a":1}""", "", Seq("""{"a":1}""")),
          ("""{"a":1,"b":2}""", """{"a":1}""", Seq("""{"b":2}""")),
          ("""{"a":1,"b":2,"c":"d"}""", """{"a":1}""", Seq("""{"b":2}""", """{"c":"d"}"""))
        )

        values.zipWithIndex.foreach {
          case (value, index) => assertResult(value._1, "at index "+index) { JwtUtils.mergeJson(value._2, value._3: _*) }
        }
      }
    }

    val signKey = Option("secret")
    val signMessage = """{"alg": "algo"}.{"user": 1, "admin": true, "value": "foo"}"""

    // Seq[(result, algo)]
    val signValues: Seq[(String, String)] = Seq(
      ("媉㶩஥ᐎ䗼ⲑΠ", "HmacMD5"),
      ("媉㶩஥ᐎ䗼ⲑΠ", "HMD5"),
      ("ﹰﱉ녙죀빊署▢륧婍", "HmacSHA1")
    )

    /*describe("sign byte array") {
      it("should correctly handle string") {
        signValues.foreach {
          value => assertResult(value._1.getBytes(encoding)) { JwtUtils.sign(signMessage.getBytes(encoding), signKey, Option(value._2)) }
        }
      }
    }

    describe("sign string") {
      it("should correctly handle string") {
        signValues.foreach {
          value => assertResult(value._1.getBytes(encoding)) { JwtUtils.sign(signMessage, signKey, Option(value._2)) }
        }
      }
    }*/
  }
}
