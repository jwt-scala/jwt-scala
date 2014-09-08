package pdi.scala

import play.api.Play
import play.api.mvc.{Result, RequestHeader}
import play.api.libs.json.JsObject

package object jwt extends JwtJsonImplicits with JwtPlayImplicits {}
