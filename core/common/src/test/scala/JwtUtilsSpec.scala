package pdi.jwt

import org.scalatest._

case class TestObject(value: String) {
  override def toString(): String = this.value
}

class JwtUtilsSpec extends UnitSpec {
  val ENCODING = JwtUtils.ENCODING

  describe("JwtUtils") {
    describe("hashToJson") {
      it("should transform a seq of tuples to a valid JSON") {
        val values: Seq[(String, Seq[(String, Any)])] = Seq(
          """{"a":"b","c":1,"d":true,"e":2,"f":3.4,"g":5.6}""" -> Seq(
            "a" -> "b",
            "c" -> 1,
            "d" -> true,
            "e" -> 2L,
            "f" -> 3.4f,
            "g" -> 5.6
          ),
          "{}" -> Seq(),
          """{"a\"b":"a\"b","c\"d":"c\"d","e\"f":["e\"f","e\"f"]}""" -> Seq(
            """a"b""" -> """a"b""",
            """c"d""" -> TestObject("""c"d"""),
            """e"f""" -> Seq("""e"f""", TestObject("""e"f"""))
          )
        )

        values.zipWithIndex.foreach {
          case (value, index) => assertResult(value._1, "at index "+index) { JwtUtils.hashToJson(value._2) }
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
          value => assertResult(value._1.getBytes(ENCODING)) { JwtUtils.sign(signMessage.getBytes(ENCODING), signKey, Option(value._2)) }
        }
      }
    }

    describe("sign string") {
      it("should correctly handle string") {
        signValues.foreach {
          value => assertResult(value._1.getBytes(ENCODING)) { JwtUtils.sign(signMessage, signKey, Option(value._2)) }
        }
      }
    }*/
  }
}
