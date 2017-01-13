package pdi.jwt

import upickle.Js
import upickle.default._

/**
  * Created by alonsodomin on 08/09/2016.
  */
class UpickleSpec extends JwtJsonCommonSpec[Js.Value] with UpickleFixture {
  override def jwtJsonCommon = JwtUpickle
}
