package pdi.jwt

import org.scalatest._

class JwtBase64Spec extends UnitSpec {
  val eol = System.getProperty( "line.separator" )

  val values: Seq[(String, String)] = Seq(
    ("", ""),
    ("a", "YQ"),
    ("1", "MQ"),
    ("""{"alg": "algo"}.{"user": 1, "admin": true, "value": "foo"}""", "eyJhbGciOiAiYWxnbyJ9LnsidXNlciI6IDEsICJhZG1pbiI6IHRydWUsICJ2YWx1ZSI6ICJmb28ifQ"),
    ("azeklZJEKL,93l,zae:km838{az:e}lekr[l874:e]aze", "YXpla2xaSkVLTCw5M2wsemFlOmttODM4e2F6OmV9bGVrcltsODc0OmVdYXpl"),
    ("""azeqsdwxcrtyfghvbnuyiopjhkml1234567890&é'(-è_çà)=$£ù%*µ,?;.:/!+-*/§äâêëûüîïÂÄÊËÎÏÜÛÔÖZRTYPQSDFGHJKLMWXCVBN<>#{}[]|`\^@¤""", "YXplcXNkd3hjcnR5ZmdodmJudXlpb3BqaGttbDEyMzQ1Njc4OTAmw6knKC3DqF_Dp8OgKT0kwqPDuSUqwrUsPzsuOi8hKy0qL8Knw6TDosOqw6vDu8O8w67Dr8OCw4TDisOLw47Dj8Ocw5vDlMOWWlJUWVBRU0RGR0hKS0xNV1hDVkJOPD4je31bXXxgXF5AwqQ")
  )

  describe("JwtBase64") {
    it("should encode string") {
      values.foreach {
        value => assertResult(value._2) { JwtBase64.encodeString(value._1) }
      }
    }

    it("should decode strings") {
      values.foreach {
        value => assertResult(value._1) { JwtBase64.decodeString(value._2) }
      }
    }

    it("should be symmetrical") {
      values.foreach {
        value => assertResult(value._1) { JwtBase64.decodeString(JwtBase64.encodeString(value._1)) }
      }

      values.foreach {
        value => assertResult(value._2) { JwtBase64.encodeString(JwtBase64.decodeString(value._2)) }
      }
    }

    it("should throw when invalid string") {
      val vals = Seq("a", "abcde", "*", "aze$")
      vals.foreach { v =>
        intercept[IllegalArgumentException] { JwtBase64.decode(v) }
      }
    }
  }
}
