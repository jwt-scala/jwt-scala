package pdi.scala.jwt

import org.scalatest._

class JwtBase64Spec extends UnitSpec {
  val eol = System.getProperty( "line.separator" )

  val values: Seq[(String, String)] = Seq(
    ("", ""),
    ("a", "YQ=="),
    ("1", "MQ=="),
    ("""{"alg": "algo"}.{"user": 1, "admin": true, "value": "foo"}""", "eyJhbGciOiAiYWxnbyJ9LnsidXNlciI6IDEsICJhZG1pbiI6IHRydWUsICJ2YWx1ZSI6ICJmb28ifQ=="),
    ("azeklZJEKL,93l,zae:km838{az:e}lekr[l874:e]aze", "YXpla2xaSkVLTCw5M2wsemFlOmttODM4e2F6OmV9bGVrcltsODc0OmVdYXpl")
  )

  describe("JwtBase64") {
    it("should encode string") {
      values.foreach {
        value => assertResult(value._2) { JwtBase64.encode(value._1) }
      }
    }

    it("should decode strings") {
      values.foreach {
        value => assertResult(value._1) { JwtBase64.decode(value._2) }
      }
    }

    it("should be symmetrical") {
      values.foreach {
        value => assertResult(value._1) { JwtBase64.decode(JwtBase64.encode(value._1)) }
      }

      values.foreach {
        value => assertResult(value._2) { JwtBase64.encode(JwtBase64.decode(value._2)) }
      }
    }
  }
}
